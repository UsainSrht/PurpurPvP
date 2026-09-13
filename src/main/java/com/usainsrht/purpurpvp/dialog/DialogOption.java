package com.usainsrht.purpurpvp.dialog;

import net.kyori.adventure.text.Component;

/**
 * Represents a selectable option within a single-option dialog input.
 */
public record DialogOption(String id, Component label, boolean initial) {

    public static DialogOption of(String id, Component label) {
        return new DialogOption(id, label, false);
    }

    public static DialogOption of(String id, Component label, boolean initial) {
        return new DialogOption(id, label, initial);
    }

    public static DialogOption of(String id, String label) {
        return new DialogOption(id, Component.text(label), false);
    }
}
