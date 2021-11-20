package com.rexcantor64.triton.guiapi;

import com.rexcantor64.triton.Triton;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ScrollableGui extends Gui {

    private List<GuiButton> items = new ArrayList<>();

    public ScrollableGui() {
        this("");
    }

    public ScrollableGui(String title) {
        super();
        rows = -1;
        super.title = title;
    }

    private static int getRows(int i) {
        int actual = 1;
        while (i > 9) {
            i = i - 9;
            actual++;
        }
        return actual * 9;
    }

    public int nextIndex() {
        return -1;
    }

    public void addButton(GuiButton button) {
        items.add(button);
    }

    public void setButton(int position, GuiButton button) {
        addButton(button);
    }

    public void removeButton(int position) {
        items.remove(position);
    }

    public void clearGuiWithoutButtons() {

    }

    public GuiButton getButton(int position) {
        return getButton(position, 1);
    }

    public GuiButton getButton(int position, int currentPage) {
        int index = (currentPage - 1) * 45 + position;
        if (items.size() <= index) return null;
        return items.get(position);
    }

    public int getSize() {
        return items.size();
    }

    private int getSize(int page) {
        if (getMaxPages() > 1) return 54;
        return getSize();
    }

    public void open(Player p) {
        open(p, 1);
    }

    public void open(Player p, int page) {
        Inventory inv = Bukkit
                .createInventory(this, getRows(getSize(page)), ChatColor.translateAlternateColorCodes('&', title));
        if (getMaxPages() == 1)
            for (GuiButton btn : items)
                inv.addItem(btn.getItemStack());
        else {
            for (int i = 0; i < 45; i++) {
                int index = (page - 1) * 45 + i;
                if (items.size() <= index) break;
                inv.addItem(items.get(index).getItemStack());
            }
            addNavigationButtons(inv, page);
        }
        p.openInventory(inv);
        Triton.get().getGuiManager().add(inv, new OpenGuiInfo(this, page));
        this.inv = inv;
    }

    private void addNavigationButtons(Inventory inv, int page) {
        if (page != 1) {
            ItemStack backwardsButton = new ItemStack(Material.ARROW);
            ItemMeta stackMeta = backwardsButton.getItemMeta();
            stackMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stackMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stackMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                    .getMessage("other.selector-gui-prev")));
            backwardsButton.setItemMeta(stackMeta);
            inv.setItem(45, backwardsButton);
        }
        if (page != getMaxPages()) {
            ItemStack forwardButton = new ItemStack(Material.ARROW);
            ItemMeta stackMeta = forwardButton.getItemMeta();
            stackMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stackMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stackMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                    .getMessage("other.selector-gui-forward")));
            forwardButton.setItemMeta(stackMeta);
            inv.setItem(53, forwardButton);
        }

        ItemStack infoButton = new ItemStack(Material.NAME_TAG);
        ItemMeta stackMeta = infoButton.getItemMeta();
        stackMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stackMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stackMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stackMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                .getMessage("other.selector-gui-currentpage", page, getMaxPages())));
        infoButton.setItemMeta(stackMeta);
        inv.setItem(49, infoButton);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getCurrentPage() {
        return -1;
    }

    public void setCurrentPage(int current) {
    }

    public int getMaxPages() {
        if (getSize() <= 54) return 1;
        return (int) Math.ceil(getSize() / 45);
    }

    public void setMaxPages(int max) {
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
