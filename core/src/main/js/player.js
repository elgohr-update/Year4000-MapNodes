/*
 * Copyright 2016 Year4000. All Rights Reserved.
 */

class Player {
  constructor(json) {
    this._meta = json;
  }

  get uuid() {
    return this._meta.uuid;
  }

  get username() {
    return this._meta.username;
  }

  set team(team) {
    $.update_team(this, team);
  }

  /** Send a message to this player */
  send_message(message) {
    $.send_message(this.uuid, message);
  }
}
