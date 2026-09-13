package com.usainsrht.purpurpvp.dialog;

import io.papermc.paper.dialog.DialogResponseView;

/**
 * Clean wrapper around Paper's DialogResponseView for convenient data extraction.
 */
public class DialogResponse {

    private final DialogResponseView view;

    public DialogResponse(DialogResponseView view) {
        this.view = view;
    }

    public String getText(String key) {
        return view != null ? view.getText(key) : null;
    }

    public String getText(String key, String defaultValue) {
        String val = getText(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (view == null) return defaultValue;
        Boolean val = view.getBoolean(key);
        return val != null ? val : defaultValue;
    }

    public float getFloat(String key, float defaultValue) {
        if (view == null) return defaultValue;
        Float val = view.getFloat(key);
        return val != null ? val : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return Math.round(getFloat(key, (float) defaultValue));
    }

    public DialogResponseView rawView() {
        return view;
    }
}
