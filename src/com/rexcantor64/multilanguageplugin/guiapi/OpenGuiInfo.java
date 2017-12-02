package com.rexcantor64.multilanguageplugin.guiapi;

public class OpenGuiInfo {

    private Gui gui;
    private int currentPage;

    public OpenGuiInfo(Gui gui) {
        this.gui = gui;
        this.currentPage = -1;
    }

    public OpenGuiInfo(ScrollableGui gui, int currentPage) {
        this.gui = gui;
        this.currentPage = currentPage;
    }

    public Gui getGui() {
        return gui;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
