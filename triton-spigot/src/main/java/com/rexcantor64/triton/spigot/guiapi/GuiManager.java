package com.rexcantor64.triton.spigot.guiapi;

import com.google.common.collect.Maps;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class GuiManager implements Listener {

    private HashMap<Inventory, OpenGuiInfo> open = Maps.newHashMap();

    public void add(Inventory inv, OpenGuiInfo gui) {
        open.put(inv, gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        OpenGuiInfo guiInfo = open.get(e.getInventory());
        if (guiInfo == null) return;
        Gui gui = guiInfo.getGui();
        if (gui.isBlocked())
            e.setCancelled(true);
        if (e.getClickedInventory() == null)
            return;
        GuiButton btn = gui.getButton(e.getRawSlot());
        if (gui instanceof ScrollableGui) {
            ScrollableGui sGui = (ScrollableGui) gui;
            if (sGui.getMaxPages() != 1)
                if (e.getRawSlot() >= 45) {
                    if (e.getRawSlot() == 45)
                        if (guiInfo.getCurrentPage() > 1)
                            sGui.open((Player) e.getWhoClicked(), guiInfo.getCurrentPage() - 1);
                        else if (e.getRawSlot() == 54)
                            if (guiInfo.getCurrentPage() < sGui.getMaxPages())
                                sGui.open((Player) e.getWhoClicked(), guiInfo.getCurrentPage() + 1);
                    return;
                }
            btn = sGui.getButton(e.getRawSlot(), guiInfo.getCurrentPage());
        }
        if (btn == null)
            return;
        btn.getEvent().onClick(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        open.remove(e.getInventory());
    }

}
