package com.sumologic.killerqueen.events

import com.sumologic.killerqueen.Logging
import com.sumologic.killerqueen.model.InboundEvents._
import com.sumologic.killerqueen.model.OutboundEvents._
import com.sumologic.killerqueen.model.{GameplayEvent, InboundEvent}
import com.sumologic.killerqueen.state.StateMachine

class EventHandler(eventSender: EventSender,
                   stateMachine: StateMachine = new StateMachine,
                   victoryHook: () => Unit = () => {}
                  ) extends Logging {
  def handle(event: InboundEvent): Unit = {
    event match {
      case AliveEvent(_) =>
        eventSender.send(ImAliveEvent)

      case UnknownEvent(key, value) =>
        warn(s"Encountered unknown event. key: $key - value: $value")

      case ConnectedEvent(connectionId) =>
        info(s"Connection opened to cabinet completed ($connectionId)")
        stateMachine.reset(event)

      case victoryEvent: VictoryEvent =>
        stateMachine.processEvent(victoryEvent)
        victoryHook()
        stateMachine.reset(event)

      case gameplayEvent: GameplayEvent =>
        stateMachine.processEvent(gameplayEvent)

      case _ =>
        error(s"Encountered completely unknown event type: ${event.toApi} (scala: $event)")
    }
  }
}