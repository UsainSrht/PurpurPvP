package com.usainsrht.purpurpvp.util;

import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;

/**
 * Utility for serializing/deserializing ItemStack arrays to/from Base64 strings.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    public static String toBase64(ItemStack[] items) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeInt(items.length);
            for (ItemStack item : items) {
                if (item == null) {
                    oos.writeInt(-1);
                } else {
                    byte[] data = item.serializeAsBytes();
                    oos.writeInt(data.length);
                    oos.write(data);
                }
            }
            oos.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize items", e);
        }
    }

    public static ItemStack[] fromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack[0];
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            int length = ois.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                int dataLength = ois.readInt();
                if (dataLength == -1) {
                    items[i] = null;
                } else {
                    byte[] data = ois.readNBytes(dataLength);
                    items[i] = ItemStack.deserializeBytes(data);
                }
            }
            return items;
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }
    }

    public static String itemToBase64(ItemStack item) {
        if (item == null) return null;
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack itemFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
    }
}

