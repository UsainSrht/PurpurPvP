package com.usainsrht.purpurpvp.match;

/**
 * Fixed daytime settings for matches.
 */
public enum DaytimeSetting {
    SUNRISE(23000),
    NOON(6000),
    SUNSET(12000),
    NIGHT(13000),
    MIDNIGHT(18000);

    private final long ticks;

    DaytimeSetting(long ticks) {
        this.ticks = ticks;
    }

    public long getTicks() { return ticks; }

    public DaytimeSetting next() {
        DaytimeSetting[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }
}

