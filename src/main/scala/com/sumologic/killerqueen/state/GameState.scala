package com.sumologic.killerqueen.state

import com.sumologic.killerqueen.model.{Event, XYConstants}

import scala.collection.mutable

class GameState {
  val id = System.currentTimeMillis()

  var map: Option[String] = None

  var inProgress = false
  private var _isBonusGame: Option[Boolean] = None
  private var _isDemoGame: Option[Boolean] = None

  def isBonusGame_=(newValue: Boolean): Unit = {
    if (_isBonusGame.isEmpty || _isBonusGame.get == newValue) {
      _isBonusGame = Some(newValue)
    } else {
      throw new RuntimeException(s"Conflicting heuristics for detecting bonus game state.  Was ${_isBonusGame.get} and was told to set to $newValue")
    }
  }

  def isBonusGame: Option[Boolean] = _isBonusGame

  def isDemoGame_=(newValue: Boolean): Unit = {
    if (_isDemoGame.isEmpty || _isDemoGame.get == newValue) {
      _isDemoGame = Some(newValue)
    } else {
      throw new RuntimeException(s"Conflicting heuristics for detecting demo game state.  Was ${_isDemoGame.get} and was told to set to $newValue")
    }
  }

  def isDemoGame: Option[Boolean] = _isDemoGame

  var victor: Option[String] = None
  var winType: Option[String] = None
  var duration: Option[Double] = None

  val playerMap: mutable.Map[Int, PlayerState] = mutable.Map.empty
  val playerList: mutable.Buffer[PlayerState] = mutable.Buffer[PlayerState]()

  var lastKnownSnailPosition: Int = XYConstants.ScreenWidth / 2


  def toCaseClass: FinalGameState = {
    val queenLives = if (isBonusGame.contains(true)) {
      2
    } else {
      3
    }

    val goldQueenDeaths = playerMap.get(1).map(_.totalDeaths).getOrElse(0)
    val blueQueenDeaths = playerMap.get(2).map(_.totalDeaths).getOrElse(0)

    def berriesScored(team: String, players: Seq[PlayerState], otherTeam: Seq[PlayerState]): Int = {
      if (winType.contains("economic") && victor.contains(team)) {
        12
      } else {
        players.map {
          player => player.foodKickedInForMyTeam + player.foodDeposited
        }.sum + otherTeam.map(_.foodKickedInForOtherTeam).sum
      }
    }

    val (goldPlayers, bluePlayers) = playerList.partition(_.team == "Gold")
    val goldBerriesScored = berriesScored("Gold", goldPlayers, bluePlayers)
    val blueBerriesScored = berriesScored("Blue", bluePlayers, goldPlayers)

    FinalGameState(
      id,
      map.getOrElse("UNKNOWN"),
      victor.getOrElse("NO VICTOR"),
      winType.getOrElse("NO WIN TYPE"),
      duration.getOrElse(Double.MinValue),
      isBonusGame.getOrElse(false),

      queenLives - goldQueenDeaths,
      queenLives - blueQueenDeaths,

      12 - goldBerriesScored,
      12 - blueBerriesScored,

      lastKnownSnailPosition
    )
  }

  override def toString(): String = {
    toCaseClass.toString
  }
}

case class FinalGameState(id: Long,
                          map: String,
                          victor: String,
                          winType: String,
                          duration: Double,
                          isBonusGame: Boolean,

                          goldQueenLivesRemaining: Int,
                          blueQueenLivesRemaining: Int,

                          goldBerriesRemaining: Int,
                          blueBerriesRemaining: Int,

                          lastKnownSnailPosition: Int) extends Event {
  val event = "finalGameState"
}