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
import net.minecraft.util.Util;
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
    private static final long INTRO_END = 360L;
    private static final long CUT_END = 680L;
    private static final long ANIMATION_END = 2050L;

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
                    startedAt = Util.getMillis();
                }
            }
        }

        if (bossName == null) return;

        long elapsed = Util.getMillis() - startedAt;
        if (elapsed >= ANIMATION_END) {
            bossName = null;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int baseline = minecraft.getWindow().getGuiScaledHeight() / 2 - 12;
        int textWidth = minecraft.font.width(bossName);
        int left = centerX - textWidth / 2;

        if (elapsed < CUT_END) {
            renderWholeName(graphics, minecraft, centerX, baseline, elapsed);
        }

        if (elapsed >= INTRO_END && elapsed < CUT_END) {
            renderCutStreaks(graphics, centerX, baseline + 7, textWidth, elapsed);
        }

        if (elapsed >= CUT_END) {
            renderShatteredName(graphics, minecraft, centerX, baseline, left, elapsed);
        }
    }

    private static void renderWholeName(GuiGraphics graphics, Minecraft minecraft,
                                        int centerX, int baseline, long elapsed) {
        float progress = Mth.clamp(elapsed / (float) INTRO_END, 0.0F, 1.0F);
        int alpha = (int) (255.0F * progress);
        int color = (alpha << 24) | 0x00F4F7FA;
        float scale = 1.12F - 0.12F * progress;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseline + 5, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -(baseline + 5), 0);
        graphics.drawCenteredString(minecraft.font, bossName, centerX, baseline, color);
        graphics.pose().popPose();
    }

    private static void renderCutStreaks(GuiGraphics graphics, int centerX,
                                         int centerY, int textWidth, long elapsed) {
        float progress = Mth.clamp((elapsed - INTRO_END) / (float) (CUT_END - INTRO_END),
            0.0F, 1.0F);
        int length = Math.max(28, textWidth / 2);
        int[] offsets = {-0.34F, 0.08F, 0.38F, -0.02F};
        for (int i = 0; i < offsets.length; i++) {
            float reveal = Mth.clamp(progress * 1.8F - i * 0.22F, 0.0F, 1.0F);
            if (reveal <= 0.0F) continue;

            int x = centerX + (int) (offsets[i] * textWidth);
            int y = centerY + (i - 1) * 3;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-42.0F + i * 9.0F));
            graphics.fill(-1, -length / 2, 2, length / 2,
                ((int) (235 * reveal) << 24) | 0x00FFFFFF);
            graphics.fill(2, -length / 2, 4, length / 2,
                ((int) (170 * reveal) << 24) | 0x00E75B63);
            graphics.pose().popPose();
        }
    }

    private static void renderShatteredName(GuiGraphics graphics, Minecraft minecraft,
                                             int centerX, int baseline, int left, long elapsed) {
        float progress = Mth.clamp((elapsed - CUT_END) /
            (float) (ANIMATION_END - CUT_END), 0.0F, 1.0F);
        float travel = 1.0F - (1.0F - progress) * (1.0F - progress);
        int groupSize = 3;
        List<String> fragments = new ArrayList<>();
        for (int start = 0; start < bossName.length(); start += groupSize) {
            fragments.add(bossName.substring(start, Math.min(start + groupSize, bossName.length())));
        }

        int cursor = 0;
        for (int i = 0; i < fragments.size(); i++) {
            String fragment = fragments.get(i);
            int prefixWidth = minecraft.font.width(bossName.substring(0, cursor));
            int fragmentWidth = minecraft.font.width(fragment);
            float direction = (i % 2 == 0) ? -1.0F : 1.0F;
            float side = (i - (fragments.size() - 1) / 2.0F);
            float dx = direction * (13.0F + (i % 4) * 8.0F) * travel
                + side * 3.0F * travel;
            float dy = (i % 3 - 1) * 9.0F * travel + 76.0F * travel * travel;
            float rotation = direction * (12.0F + (i % 5) * 8.0F) * travel;
            int alpha = 255 - (int) (255.0F * Mth.clamp((progress - 0.58F) / 0.42F,
                0.0F, 1.0F));
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
    }
}
