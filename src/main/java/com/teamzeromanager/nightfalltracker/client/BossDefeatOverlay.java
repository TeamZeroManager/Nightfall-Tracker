package com.teamzeromanager.nightfalltracker.client;

import com.mojang.math.Axis;
import com.teamzeromanager.nightfalltracker.NightfallTracker;
import com.teamzeromanager.nightfalltracker.bestiary.BossRegistrySavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossDefeatOverlay {
    private static final long INTRO_END = 450L;
    private static final long CUT_START = 1450L;
    private static final long SHATTER_START = 1950L;
    private static final long ANIMATION_END = 5200L;
    private static final float NAME_SCALE = 2.0F;

    private static final int PANEL = 0xD909111B;
    private static final int PANEL_EDGE = 0xFF26394A;
    private static final int WHITE = 0xFFF4F7FA;
    private static final int CYAN = 0xFF55D6D0;
    private static final int RED = 0xFFFF5964;
    private static final int MUTED = 0xFF9CB0C2;

    private static String bossName;
    private static long startedAt;

    private BossDefeatOverlay() {}

    @SubscribeEvent
    public static void onGuiRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) return;

        if (bossName == null) {
            ResourceLocation pending = BossRegistrySavedData.get(server.overworld())
                .consumePendingDefeatAnimation();
            if (pending != null) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(pending).orElse(null);
                if (type != null) {
                    bossName = Component.translatable(type.getDescriptionId()).getString();
                    startedAt = System.currentTimeMillis();
                }
            }
        }

        if (bossName == null) return;

        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= ANIMATION_END) {
            bossName = null;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int baseline = screenHeight / 2 - 8;
        int scaledNameWidth = minecraft.font.width(bossName) * (int) NAME_SCALE;
        int panelWidth = Math.min(screenWidth - 16, Math.max(330, scaledNameWidth + 100));
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = baseline - 37;

        renderPanel(graphics, panelLeft, panelTop, panelWidth, elapsed);

        if (elapsed < SHATTER_START) {
            renderWholeName(graphics, minecraft, centerX, baseline, elapsed);
        }

        if (elapsed >= CUT_START && elapsed < SHATTER_START) {
            renderCutStreaks(graphics, centerX, baseline + 8,
                minecraft.font.width(bossName), elapsed);
        }

        if (elapsed >= SHATTER_START) {
            renderShatteredName(graphics, minecraft, centerX, baseline, elapsed);
        }
    }

    private static void renderPanel(GuiGraphics graphics, int left, int top,
                                    int width, long elapsed) {
        float intro = Mth.clamp(elapsed / (float) INTRO_END, 0.0F, 1.0F);
        int alpha = (int) (220 * intro);
        graphics.fill(left, top, left + width, top + 76,
            (alpha << 24) | 0x0009111B);
        graphics.fill(left, top, left + width, top + 1, PANEL_EDGE);
        graphics.fill(left, top + 75, left + width, top + 76, PANEL_EDGE);
        graphics.fill(left, top, left + 3, top + 76, CYAN);
        graphics.fill(left + width - 3, top, left + width, top + 76, RED);

        int lineAlpha = (int) (210 * intro);
        int lineColor = (lineAlpha << 24) | 0x0055D6D0;
        graphics.fill(left + 24, top + 20, left + width - 24, top + 21, lineColor);

        if (elapsed < SHATTER_START) {
            int pulse = (int) (20 * (0.5F + 0.5F *
                (float) Math.sin(elapsed / 130.0F)));
            graphics.fill(left + 3, top + 1, left + 3 + 2 + pulse / 5,
                top + 75, (Math.min(255, 90 + pulse) << 24) | 0x0055D6D0);
        }
    }

    private static void renderWholeName(GuiGraphics graphics, Minecraft minecraft,
                                        int centerX, int baseline, long elapsed) {
        float intro = Mth.clamp(elapsed / (float) INTRO_END, 0.0F, 1.0F);
        float scale = NAME_SCALE * (0.88F + 0.12F * intro);
        int alpha = (int) (255.0F * intro);
        int color = (alpha << 24) | 0x00F4F7FA;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseline + 5, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -(baseline + 5), 0);
        graphics.drawCenteredString(minecraft.font, bossName, centerX, baseline, color);
        graphics.pose().popPose();

        graphics.drawCenteredString(minecraft.font, "BOSS DEFEATED",
            centerX, baseline - 27, MUTED);
    }

    private static void renderCutStreaks(GuiGraphics graphics, int centerX,
                                         int centerY, int textWidth, long elapsed) {
        float progress = Mth.clamp((elapsed - CUT_START) /
            (float) (SHATTER_START - CUT_START), 0.0F, 1.0F);
        int length = Math.max(34, textWidth / 2);
        float[] offsets = {-0.40F, -0.12F, 0.19F, 0.42F, 0.02F};

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1.0F);
        for (int i = 0; i < offsets.length; i++) {
            float reveal = Mth.clamp(progress * 1.9F - i * 0.16F, 0.0F, 1.0F);
            if (reveal <= 0.0F) continue;

            int x = (int) (offsets[i] * textWidth);
            int y = (i - 2) * 2;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-42.0F + i * 8.0F));
            graphics.fill(-1, -length / 2, 2, length / 2,
                ((int) (245 * reveal) << 24) | 0x00FFFFFF);
            graphics.fill(2, -length / 2, 5, length / 2,
                ((int) (210 * reveal) << 24) | 0x00FF5964);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    private static void renderShatteredName(GuiGraphics graphics, Minecraft minecraft,
                                             int centerX, int baseline, long elapsed) {
        float progress = Mth.clamp((elapsed - SHATTER_START) /
            (float) (ANIMATION_END - SHATTER_START), 0.0F, 1.0F);
        float travel = 1.0F - (1.0F - progress) * (1.0F - progress);
        int groupSize = 2;
        List<String> fragments = new ArrayList<>();
        for (int start = 0; start < bossName.length(); start += groupSize) {
            fragments.add(bossName.substring(start,
                Math.min(start + groupSize, bossName.length())));
        }

        int fullWidth = minecraft.font.width(bossName);
        int left = centerX - fullWidth / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseline + 5, 0);
        graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1.0F);
        graphics.pose().translate(-centerX, -(baseline + 5), 0);

        int cursor = 0;
        for (int i = 0; i < fragments.size(); i++) {
            String fragment = fragments.get(i);
            int prefixWidth = minecraft.font.width(bossName.substring(0, cursor));
            int fragmentWidth = minecraft.font.width(fragment);
            float direction = (i % 2 == 0) ? -1.0F : 1.0F;
            float side = i - (fragments.size() - 1) / 2.0F;
            float dx = direction * (24.0F + (i % 4) * 13.0F) * travel
                + side * 5.0F * travel;
            float lift = ((i % 3) - 1) * 18.0F * travel;
            float dy = lift + 112.0F * travel * travel;
            float rotation = direction * (28.0F + (i % 5) * 17.0F) * travel;
            float fade = Mth.clamp((progress - 0.62F) / 0.38F, 0.0F, 1.0F);
            int alpha = 255 - (int) (255.0F * fade);
            int color = (alpha << 24) | 0x00F4F7FA;

            float fragmentCenterX = left + prefixWidth + fragmentWidth / 2.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(fragmentCenterX + dx, baseline + 5 + dy, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
            graphics.pose().translate(-fragmentCenterX, -(baseline + 5), 0);
            graphics.drawString(minecraft.font, fragment, left + prefixWidth, baseline,
                color, true);
            graphics.pose().popPose();
            cursor += fragment.length();
        }
        graphics.pose().popPose();

        int flashAlpha = (int) (115 * (1.0F - progress));
        if (flashAlpha > 0) {
            graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                (flashAlpha << 24) | 0x00D8F4F2);
        }
    }
}
