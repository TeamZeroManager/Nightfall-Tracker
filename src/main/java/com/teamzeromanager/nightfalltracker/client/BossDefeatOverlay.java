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
import net.minecraft.sounds.SoundEvents;
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
    private static final long CUT_START = 850L;
    private static final long SHATTER_START = 2900L;
    private static final long ANIMATION_END = 5000L;
    private static final long CUT_SEQUENCE_DURATION = SHATTER_START - CUT_START;
    private static final float NAME_SCALE = 2.0F;

    private static final int PANEL = 0xD909111B;
    private static final int PANEL_EDGE = 0xFF26394A;
    private static final int WHITE = 0xFFF4F7FA;
    private static final int CYAN = 0xFF55D6D0;
    private static final int RED = 0xFFFF5964;
    private static final int MUTED = 0xFF9CB0C2;

    private static String bossName;
    private static long startedAt;
    private static int lastSoundedCut = -1;
    private static int lastImpactCut = -1;

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
                    lastSoundedCut = -1;
                    lastImpactCut = -1;
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

        if (elapsed < CUT_START) {
            renderWholeName(graphics, minecraft, centerX, baseline, elapsed);
        } else if (elapsed < SHATTER_START) {
            renderCutSequence(graphics, minecraft, centerX, baseline, elapsed);
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

    private static void renderCutSequence(GuiGraphics graphics, Minecraft minecraft,
                                          int centerX, int baseline, long elapsed) {
        int textWidth = minecraft.font.width(bossName);
        int left = centerX - textWidth / 2;
        final int sliceCount = 4;
        final int cutCount = sliceCount - 1;
        final int sliceHeight = 3;

        float sequenceProgress = Mth.clamp((elapsed - CUT_START) /
            (float) CUT_SEQUENCE_DURATION, 0.0F, 1.0F);
        float cutPosition = sequenceProgress * cutCount;
        int completedCuts = Mth.clamp((int) cutPosition, 0, cutCount);
        int activeCut = Math.min(completedCuts, cutCount - 1);
        float activeProgress = cutPosition - completedCuts;

        // Each horizontal strip is clipped from the actual rendered word.
        // The glyphs themselves are therefore split into independent pieces,
        // rather than being replaced by detached whole characters.
        for (int slice = 0; slice < sliceCount; slice++) {
            int cutsBeforeSlice = Math.min(slice, completedCuts);
            float separation = (cutsBeforeSlice - completedCuts / 2.0F) * 3.2F;
            float driftX = (slice % 2 == 0 ? -1.0F : 1.0F)
                * cutsBeforeSlice * 1.1F;
            float driftY = separation;

            graphics.pose().pushPose();
            graphics.pose().translate(centerX, baseline + 5, 0);
            graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1.0F);
            graphics.pose().translate(-centerX, -(baseline + 5), 0);
            graphics.pose().translate(driftX / NAME_SCALE, driftY / NAME_SCALE, 0);

            graphics.enableScissor(left, baseline + slice * sliceHeight,
                left + textWidth, baseline + (slice + 1) * sliceHeight);
            graphics.drawCenteredString(minecraft.font, bossName, centerX, baseline,
                WHITE);
            graphics.disableScissor();
            graphics.pose().popPose();
        }

        // Play one katana-like sweep per cut and a sharper impact at contact.
        if (completedCuts < cutCount && activeCut != lastSoundedCut) {
            float pitch = 0.82F + activeCut * 0.09F;
            minecraft.player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.85F, pitch);
            lastSoundedCut = activeCut;
        }
        if (completedCuts < cutCount && activeProgress >= 0.82F
            && activeCut != lastImpactCut) {
            minecraft.player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.65F,
                0.92F + activeCut * 0.06F);
            lastImpactCut = activeCut;
        }

        // The active blade travels across the word on the next cut line.
        if (completedCuts < cutCount) {
            float sweep = Mth.clamp(activeProgress, 0.0F, 1.0F);
            float bladeX = -textWidth / 2.0F + textWidth * sweep;
            float bladeY = -5.0F + (activeCut + 1) * sliceHeight;
            graphics.pose().pushPose();
            graphics.pose().translate(centerX + bladeX, baseline + bladeY, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(
                (activeCut % 2 == 0) ? -16.0F : 16.0F));
            int glowAlpha = (int) (135 + 120 * Math.sin(sweep * Math.PI));
            graphics.fill(-3, -18, 3, 18, (glowAlpha << 24) | 0x00FF344F);
            graphics.fill(-1, -17, 1, 17, 0xFFFFFFFF);
            graphics.fill(2, -14, 4, 14, 0xFFFF5964);
            graphics.pose().popPose();

            int sparkAlpha = (int) (210 * (1.0F - Math.abs(0.5F - sweep) * 2.0F));
            int spark = (sparkAlpha << 24) | 0x00FFE7A3;
            graphics.fill((int) (centerX + bladeX - 7), (int) (baseline + bladeY - 3),
                (int) (centerX + bladeX - 3), (int) (baseline + bladeY + 1), spark);
            graphics.fill((int) (centerX + bladeX + 3), (int) (baseline + bladeY + 2),
                (int) (centerX + bladeX + 7), (int) (baseline + bladeY + 6), spark);
        }

        // Brief red seams remain where the letters have already been sliced.
        for (int cut = 0; cut < completedCuts; cut++) {
            int seamY = baseline - 5 + (cut + 1) * sliceHeight * 2;
            graphics.fill(left - 2, seamY, left + textWidth * 2 + 2, seamY + 1,
                0xCCFF5964);
        }

        graphics.drawCenteredString(minecraft.font, "BOSS DEFEATED",
            centerX, baseline - 27, MUTED);
    }

    private static void renderShatteredName(GuiGraphics graphics, Minecraft minecraft,
                                             int centerX, int baseline, long elapsed) {
        float progress = Mth.clamp((elapsed - SHATTER_START) /
            (float) (ANIMATION_END - SHATTER_START), 0.0F, 1.0F);
        float travel = 1.0F - (1.0F - progress) * (1.0F - progress);
        int groupSize = 1;
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
            float dx = direction * (34.0F + (i % 5) * 17.0F) * travel
                + side * 8.0F * travel;
            float lift = ((i % 5) - 2) * 24.0F * travel;
            float dy = lift + 155.0F * travel * travel;
            float rotation = direction * (55.0F + (i % 7) * 24.0F) * travel;
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
