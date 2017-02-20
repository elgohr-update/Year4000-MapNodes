/*
 * Copyright 2017 Year4000. All Rights Reserved.
 */

/**
  Bindings is the bridge between the multiple languages. The bindings allows
  for object programing, but still allow the speed that is needed. Since only
  the low level forms the data types are transmitted between.
*/

/** This constant is created by the runtime no need for it */
var PLATFORM = PLATFORM || 'none';

/** This constant is created by the runtime no need for it */
var JAVA = JAVA || {};

/** The constants that are known when the JS runtime is created */
var PLATFORMS = {
  "PC": "java", // Sponge
  "PE": "java" // Nukkit
};

/** Used to interact the Javascript with MapNodes base game */
class $ {
  /** Get the instance of this javascript object */
  static get js() {
    return $._js || ($._js = new $());
  }

  /** Map a specific object to this bindings var */
  static get bindings() {
    if (PLATFORM == PLATFORMS.PE) {
      return JAVA;
    } else if (PLATFORM == PLATFORMS.PC) {
      return JAVA;
    } else {
      throw "PLATFORM is not defined!";
    }
  }

  /** Send a message to the player */
  static send_message(player, message) {
      $.bindings.send_message(player.uuid, message);
  }

  /** This function just makes sure the bindings were properly established */
  platform_name() { // @Bind
    return PLATFORM;
  }

  /** Is the game currently running */
  is_game_running() { // @Bind
    return false;
  }

  /** Load the environment */
  load() { // @Bind
    println("Loading environment from the Javascript side");
    println('Lodash Version: ' + _.VERSION);
    println('Immutable Version: ' + typeof Immutable);
  }
}

/** Include the resource into the V8 runtime */
function include(path) {
  $.bindings._include(path);
}

/** Print the message to the console */
function print(message) {
  $.bindings.print(message);
}

/** Print a line to the console */
function println(message) {
  print(message + "\n")
}

/** Dump the var to the screen */
function var_dump(variable) {
  println(JSON.stringify(variable));
}