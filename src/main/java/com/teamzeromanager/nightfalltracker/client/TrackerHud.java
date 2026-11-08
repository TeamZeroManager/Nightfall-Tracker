package com.teamzeromanager.nightfalltracker.client;

import com.teamzeromanager.nightfalltracker.NightfallTracker;
import com.teamzeromanager.nightfalltracker.stats.TrackerSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrackerHud {
    private static final int PANEL_BG = 0xD9101724;
    private static final int PANEL_BORDER = 0xFF26394A;
    private static final int TEXT = 0xFFF1F5F9;
    private static final int MUTED = 0xFF91A4B7;
    private static final int CYAN = 0xFF55D6D0;
    private static final int RED = 0xFFFF7777;
    private static final int GOLD = 0xFFFFC857;
    private static final long STATS_REFRESH_INTERVAL = 250L;

    private static long cachedDeaths;
    private static long cachedBossDeaths;
    private static long nextStatsRefresh;

    private TrackerHud() {}

    @SubscribeEvent
    public static void onGuiRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

        long now = System.currentTimeMillis();
        if (now >= nextStatsRefresh) {
            nextStatsRefresh = now + STATS_REFRESH_INTERVAL;
            if (minecraft.getSingleplayerServer() != null) {
                ServerLevel overworld = minecraft.getSingleplayerServer().overworld();
                TrackerSavedData data = TrackerSavedData.get(overworld);
                cachedDeaths = data.getTotalDeaths();
                cachedBossDeaths = data.getBossDeaths();
            }
        }

        long days = Math.max(1L, (minecraft.level.getDayTime() / 24000L) + 1L);
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int right = screenWidth - 10;
        int top = 10;
        int panelWidth = 246;
        int panelHeight = 34;
        int left = right - panelWidth;

        graphics.fill(left, top, right, top + panelHeight, PANEL_BG);
        graphics.fill(left, top, right, top + 1, PANEL_BORDER);
        graphics.fill(left, top + panelHeight - 1, right, top + panelHeight, PANEL_BORDER);
        graphics.fill(left, top, left + 2, top + panelHeight, CYAN);

        drawStat(graphics, minecraft, left + 10, top + 5, "☀", "DAYS", days, GOLD);
        graphics.fill(left + 82, top + 7, left + 83, top + 27, PANEL_BORDER);

        drawStat(graphics, minecraft, left + 91, top + 5, "☠", "DEATHS", cachedDeaths, RED);
        graphics.fill(left + 164, top + 7, left + 165, top + 27, PANEL_BORDER);

        drawStat(graphics, minecraft, left + 173, top + 5, "⚔", "BOSS", cachedBossDeaths, CYAN);
    }

    private static void drawStat(GuiGraphics graphics, Minecraft minecraft, int x, int y,
                                 String icon, String label, long value, int accent) {
        graphics.drawString(minecraft.font, icon, x, y + 5, accent, false);
        graphics.drawString(minecraft.font, label, x + 15, y, MUTED, false);
        graphics.drawString(minecraft.font, Long.toString(value), x + 15, y + 12, TEXT, false);
    }
}
