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

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrackerHud {
    private TrackerHud() {}

    @SubscribeEvent
    public static void onGuiRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

        long days = Math.max(1L, (minecraft.level.getDayTime() / 24000L) + 1L);
        long deaths = 0L;
        long bossDeaths = 0L;

        if (minecraft.getSingleplayerServer() != null) {
            ServerLevel overworld = minecraft.getSingleplayerServer().overworld();
            TrackerSavedData data = TrackerSavedData.get(overworld);
            deaths = data.getTotalDeaths();
            bossDeaths = data.getBossDeaths();
        }

        String text = "DAY " + days + "   |   DEATHS " + deaths + "   |   BOSS DEATHS " + bossDeaths;
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = screenWidth - minecraft.font.width(text) - 12;
        int y = 10;

        graphics.fill(x - 6, y - 5, screenWidth - 6, y + 14, 0xA0101420);
        graphics.fill(x - 6, y - 5, x - 4, y + 14, 0xFF39D5C8);
        graphics.drawString(minecraft.font, text, x + 1, y + 1, 0xC0000000, false);
        graphics.drawString(minecraft.font, text, x, y, 0xFFF2F2F2, false);
    }
}
