package com.rexcantor64.triton.spigot.guiapi;

import com.google.common.collect.Maps;
import com.rexcantor64.triton.spigot.SpigotTriton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map.Entry;

public class Gui implements InventoryHolder {

    int rows;
    Component title;
    boolean blocked = true;
    int currentPage;
    int maxPages;
    Inventory inv;
    private HashMap<Integer, GuiButton> items = Maps.newHashMap();

    public Gui(int rows, Component title) {
        this.rows = rows;
        this.title = title;
    }

    public Gui(int rows) {
        this(rows, Component.empty());
    }

    public Gui(Component title) {
        this(1, title);
    }

    public Gui() {
        this(1, Component.empty());
    }

    public int nextIndex() {
        for (int i = 0; i < getSize(); i++) {
            if (!items.containsKey(i))
                return i;
        }
        return -1;
    }

    public void addButton(GuiButton button) {
        int index = nextIndex();

        if (index == -1)
            throw new RuntimeException("Inventory cannot be full!");
        setButton(index, button);
    }

    public void setButton(int position, GuiButton button) {
        if (position > getSize())
            throw new IllegalArgumentException("Position cannot be bigger than the size!");
        items.put(position, button);
    }

    public void removeButton(int position) {
        items.remove(position);
    }

    public void clearGuiWithoutButtons() {
        for (int i = 0; i < getSize() - 9; i++) {
            items.remove(i);
        }
    }

    public GuiButton getButton(int position) {
        return items.get(position);
    }

    public int getSize() {
        return rows * 9;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(this, getSize(), LegacyComponentSerializer.legacySection().serialize(title));
        for (Entry<Integer, GuiButton> entry : items.entrySet())
            inv.setItem(entry.getKey(), entry.getValue().getItemStack());
        p.openInventory(inv);
        SpigotTriton.asSpigot().getGuiManager().add(inv, new OpenGuiInfo(this));
        this.inv = inv;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public void setCurrentPage(int current) {
        this.currentPage = current;
    }

    public int getMaxPages() {
        return this.maxPages;
    }

    public void setMaxPages(int max) {
        this.maxPages = max;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
