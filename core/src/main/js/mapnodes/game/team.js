/*
 * Copyright 2017 Year4000. All Rights Reserved.
 */
'use strict'

/** Represents a team from the json object */
class Team extends JsonObject {

  constructor(id, team) {
    super(id, team)
    this._members = []
  }

  /** Get the json for this team */
  get team() {
    return this._json
  }

  /** The name of the team, use legacy first then default to new standard */
  get name() {
    return this.team.name || super.name()
  }

  /** Get the name of the team with the color code */
  get color_name() {
    return this.color_code + this.name
  }

  /** Get the name of the color the team bellongs to */
  get color() {
    return _.lowerCase(this.team.color)
  }

  /** Get the color code for the team */
  get color_code() {
    return Colors[this.color] || '&f&k'
  }

  /** Have the player join this team */
  join(player) {
    Conditions.not_null(player, 'player')
    if (player._current_team) { // Swap the teams the player is on
      this.$event_emitter.trigger('swap_team', [player, player._current_team, this, this.$game])
      player.leave_team()
    }
    this._members.push(player)
    this.$event_emitter.trigger('join_team', [player, this, this.$game])
  }

  /** Tell the player its time to start */
  start_player(player) {
    this.$event_emitter.trigger('start_team_player', [player, this, this.$game])
    player.start()
  }

  /** Have the entire team start */
  start() {
    this.$event_emitter.trigger('start_team', [this, this.$game])
    _.forEach(this._members, member => this.start_player(member))
  }

  /** Have the player leave the team*/
  leave(player) {
    Conditions.not_null(player, 'player')
    _.remove(this._members, object => object.is_equal(player))
    this.$event_emitter.trigger('leave_team', [player, this, this.$game])
  }

  /** Get the size of the team */
  get size() {
    return _.size(this._members)
  }

  /** Lazy load all spawn regions */
  get spawns() {
    return this._spawns || (this._spawns = _.map(this.team.spawns, zone => Regions.map_region(zone)))
  }

  /** Get a random point from the list of spawns */
  get spawn_point() {
    // todo random spawn
    return this.spawns[0].points.first()
  }
}