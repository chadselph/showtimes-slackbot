package me.chadrs.slack.reducers

import me.chadrs.slack.SlackAction

trait BotInteraction[State, Action] {

  def init: State

  def update(state: State, action: SlackAction): State

}
