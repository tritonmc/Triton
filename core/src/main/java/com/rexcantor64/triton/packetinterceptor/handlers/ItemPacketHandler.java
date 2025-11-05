package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.utils.ItemStackTranslationUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ItemPacketHandler {

    private final ItemStackTranslationUtils itemHandler;

    public ItemPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.itemHandler = new ItemStackTranslationUtils(parser, config.getItemsSyntax());
    }

    public void onSetCursorItemPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSetCursorItem(event);

        val result = this.itemHandler.translateItem(packet.getStack(), languagePlayer);
        if (result.isModified()) {
            packet.setStack(result.getModified());
            event.markForReEncode(true);
        }
    }

    public void onSetPlayerInventoryPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSetPlayerInventory(event);

        val result = this.itemHandler.translateItem(packet.getStack(), languagePlayer);
        if (result.isModified()) {
            packet.setStack(result.getModified());
            event.markForReEncode(true);
        }
    }

    public void onSetSlotPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSetSlot(event);

        val result = this.itemHandler.translateItem(packet.getItem(), languagePlayer);
        if (result.isModified()) {
            packet.setItem(result.getModified());
            event.markForReEncode(true);
        }
    }

    public void onWindowItemsPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerWindowItems(event);

        val carriedItem = packet.getCarriedItem();
        if (carriedItem.isPresent()) {
            val result = this.itemHandler.translateItem(carriedItem.get(), languagePlayer);
            if (result.isModified()) {
                packet.setCarriedItem(result.getModified());
                event.markForReEncode(true);
            }
        }

        val windowItems = packet.getItems();
        for (int i = 0; i < windowItems.size(); i++) {
            val item = windowItems.get(i);
            val result = this.itemHandler.translateItem(item, languagePlayer);
            if (result.isModified()) {
                windowItems.set(i, result.getModified());
                event.markForReEncode(true);
            }
        }
    }
}
