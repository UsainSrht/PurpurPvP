package com.usainsrht.purpurpvp.dialog;

import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DialogResponseTest {

    static class FakeDialogResponseView implements DialogResponseView {
        final Map<String, String> texts = new HashMap<>();
        final Map<String, Boolean> booleans = new HashMap<>();
        final Map<String, Float> floats = new HashMap<>();

        @Override
        public BinaryTagHolder payload() {
            return null;
        }

        @Override
        public String getText(String key) {
            return texts.get(key);
        }

        @Override
        public Boolean getBoolean(String key) {
            return booleans.get(key);
        }

        @Override
        public Float getFloat(String key) {
            return floats.get(key);
        }
    }

    @Test
    void testTextExtraction() {
        FakeDialogResponseView view = new FakeDialogResponseView();
        view.texts.put("name", "Nodebuff");

        DialogResponse response = new DialogResponse(view);
        assertEquals("Nodebuff", response.getText("name"));
        assertEquals("Nodebuff", response.getText("name", "Default"));
        assertEquals("Fallback", response.getText("missing", "Fallback"));
    }

    @Test
    void testBooleanExtraction() {
        FakeDialogResponseView view = new FakeDialogResponseView();
        view.booleans.put("ranked", true);

        DialogResponse response = new DialogResponse(view);
        assertTrue(response.getBoolean("ranked"));
        assertTrue(response.getBoolean("ranked", false));
        assertFalse(response.getBoolean("missing", false));
    }

    @Test
    void testNumberExtraction() {
        FakeDialogResponseView view = new FakeDialogResponseView();
        view.floats.put("rounds", 3.0f);

        DialogResponse response = new DialogResponse(view);
        assertEquals(3.0f, response.getFloat("rounds", 1.0f));
        assertEquals(3, response.getInt("rounds", 1));
        assertEquals(5, response.getInt("missing", 5));
    }

    @Test
    void testNullViewHandling() {
        DialogResponse response = new DialogResponse(null);
        assertNull(response.getText("any"));
        assertEquals("fallback", response.getText("any", "fallback"));
        assertFalse(response.getBoolean("any"));
        assertTrue(response.getBoolean("any", true));
        assertEquals(42, response.getInt("any", 42));
    }
}
