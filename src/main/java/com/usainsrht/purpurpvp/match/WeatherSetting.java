package com.usainsrht.purpurpvp.match;

/**
 * Fixed weather settings for matches.
 */
public enum WeatherSetting {
    CLEAR,
    RAIN,
    THUNDER,
    SNOW;

    public WeatherSetting next() {
        WeatherSetting[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }
}

