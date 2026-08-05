package com.trove.warday;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

/** Loaded only when Curios is present. */
final class CuriosInventoryBridge {
    private static final String CAPTURED_TAG = "Captured";
    private static final String DATA_TAG = "Data";

    private CuriosInventoryBridge() {
    }

    static CompoundTag capture(ServerPlayer player) {
        CompoundTag snapshot = new CompoundTag();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            Tag data = handler.writeTag();
            snapshot.putBoolean(CAPTURED_TAG, true);
            snapshot.put(DATA_TAG, data.copy());
        });
        return snapshot;
    }

    static boolean wasCaptured(CompoundTag snapshot) {
        return snapshot.getBoolean(CAPTURED_TAG) && snapshot.contains(DATA_TAG);
    }

    static boolean restore(ServerPlayer player, CompoundTag snapshot) {
        if (!snapshot.getBoolean(CAPTURED_TAG) || !snapshot.contains(DATA_TAG)) {
            return false;
        }

        Tag data = snapshot.get(DATA_TAG);
        if (data == null) {
            return false;
        }
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            handler.readTag(data.copy());
            handler.processSlots();
            return true;
        }).orElse(false);
    }
}
