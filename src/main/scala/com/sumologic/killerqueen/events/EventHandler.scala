package com.sumologic.killerqueen.events

import com.sumologic.killerqueen.Logging
import com.sumologic.killerqueen.model.InboundEvents._
import com.sumologic.killerqueen.model.OutboundEvents._
import com.sumologic.killerqueen.model.{GameplayEvent, InboundEvent, OutboundEvent, WireEvent}
import com.sumologic.killerqueen.state.StateMachine

/**
 * Handles all incoming events and delegates relevant [[GameplayEvent]] to [[StateMachine]].
 *
 * @param eventSender
 * @param stateMachine Allows injection of mocked or test [[StateMachine]] for UTs
 * @param victoryHook  Callback hook used for helping with UTs.  Called before [[StateMachine]] is reset
 */
class EventHandler(eventSender: EventSender,
                   stateMachine: StateMachine = new StateMachine,
                   victoryHook: () => Unit = () => {},
                   exceptionOnUnknownEvents: Boolean = false
                  ) extends Logging {
  def handle(event: WireEvent): Unit = {
    event match {
      case AliveEvent(_) =>
        eventSender.send(ImAliveEvent("null"))

      case ConnectedEvent(connectionId) =>
        info(s"Connection opened to cabinet completed ($connectionId)")
        stateMachine.reset(event)
        eventSender.send(AdminLoginEvent)

      case LoginEvent(success) =>
        info(s"Login succeeded? $success")
        eventSender.send(GetConfigEvent("goldonleft")) // TODO: This doesn't actually work, despite suggestions that it does.  Needs further investigation.
        eventSender.send(GetConfigEvent("tournamentstatus")) // Proof that this GetConfigEvent works

      case victoryEvent: VictoryEvent =>
        stateMachine.processEvent(victoryEvent)
        victoryHook()
        stateMachine.reset(event)

      case gameplayEvent: GameplayEvent =>
        stateMachine.processEvent(gameplayEvent)

      case _: OutboundEvent =>
        // Noop - no need to do anything for now

      case _: InboundEvent =>
        // Noop - no need to do anything for now

      case UnknownEvent(key, value) =>
        warn(s"Encountered unknown event. key: $key - value: $value")

        if (exceptionOnUnknownEvents) {
          throw new Exception(s"Unknown event: $event")
        }

      case _ =>
        error(s"Encountered completely unknown event type: ${event.toApi} (scala: $event)")

        if (exceptionOnUnknownEvents) {
          throw new Exception(s"Unknown event: $event")
        }
    }
  }
}
