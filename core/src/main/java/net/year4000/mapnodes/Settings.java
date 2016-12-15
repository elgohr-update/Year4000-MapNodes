/*
 * Copyright 2016 Year4000. All Rights Reserved.
 */
package net.year4000.mapnodes;

import net.year4000.utilities.Conditions;
import net.year4000.utilities.value.TypeValue;

/** Settings for MapNodes */
public final class Settings {

  /** The number of maps to load */
  public final int loadMaps = value("mapnodes.load_maps").toInt(30);

  /** The location to store the cached maps */
  public final String cache = (String) value("mapnodes.world_cache").getOrElse("/tmp/MapNodes");

  /** Get the value from the system properties */
  private static TypeValue value(String key) {
    Conditions.nonNullOrEmpty(key, "key");
    return new TypeValue(System.getProperty(key));
  }
}