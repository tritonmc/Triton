package com.rexcantor64.triton.spigot.guiapi;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class GuiButton {

    private ItemStack is;
    private GuiButtonClickEvent event;

    public GuiButton(ItemStack is) {
        this.is = is;
    }

    public GuiButton setListener(final Consumer<InventoryClickEvent> event) {
        this.event = new GuiButtonClickEvent() {

            @Override
            public void onClick(InventoryClickEvent e) {
                event.accept(e);
            }
        };
        return this;
    }

    public ItemStack getItemStack() {
        return is;
    }

    public GuiButtonClickEvent getEvent() {
        return event;
    }

}
