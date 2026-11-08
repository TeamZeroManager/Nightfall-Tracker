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

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossDefeatOverlay {
    private static final long INTRO_END = 500L;
    private static final long CUT_START = 650L;
    private static final long SHATTER_START = 2_750L;
    private static final long ANIMATION_END = 4_800L;
    private static final long CUT_DURATION = (SHATTER_START - CUT_START) / 3L;
    private static final int CUT_COUNT = 3;
    private static final int FRAGMENT_COUNT = CUT_COUNT + 1;
    private static final int FRAGMENT_HEIGHT = 3;
    private static final long REGISTRY_POLL_INTERVAL = 200L;

    private static final int WHITE = 0xFFF4F7FA;
    private static final int CYAN = 0xFF55D6D0;
    private static final int RED = 0xFFFF5364;
    private static final int MUTED = 0xFF9CB0C2;
    private static final int PANEL = 0xD909111B;
    private static final int EDGE = 0xFF26394A;

    private static String bossName;
    private static long startedAt;
    private static long nextRegistryPoll;
    private static int lastSoundedCut = -1;
    private static int lastImpactCut = -1;
    private static int cachedTextWidth;

    private BossDefeatOverlay() {}

    @SubscribeEvent
    public static void onGuiRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;

        long now = System.currentTimeMillis();
        if (bossName == null && now >= nextRegistryPoll) {
            nextRegistryPoll = now + REGISTRY_POLL_INTERVAL;
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                ResourceLocation pending = BossRegistrySavedData.get(server.overworld())
                    .consumePendingDefeatAnimation();
                if (pending != null) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(pending).orElse(null);
                    if (type != null) {
                        bossName = Component.translatable(type.getDescriptionId()).getString();
                        cachedTextWidth = minecraft.font.width(bossName);
                        startedAt = now;
                        lastSoundedCut = -1;
                        lastImpactCut = -1;
                    }
                }
            }
        }

        if (bossName == null) return;
        long elapsed = now - startedAt;
        if (elapsed >= ANIMATION_END) {
            bossName = null;
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int nameTop = screenHeight / 2 - 5;
        int textLeft = centerX - cachedTextWidth / 2;
        int panelWidth = Math.min(screenWidth - 20, Math.max(300, cachedTextWidth + 90));
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = nameTop - 35;

        renderPanel(graphics, panelLeft, panelTop, panelWidth, elapsed);

        if (elapsed < CUT_START) {
            renderIntro(graphics, minecraft, centerX, nameTop, elapsed);
        } else if (elapsed < SHATTER_START) {
            renderCuts(graphics, minecraft, centerX, nameTop, textLeft, elapsed);
        } else {
            renderFragments(graphics, minecraft, centerX, nameTop, textLeft, elapsed);
        }
    }

    private static void renderPanel(GuiGraphics graphics, int left, int top,
                                    int width, long elapsed) {
        float fade = Mth.clamp(elapsed / (float) INTRO_END, 0.0F, 1.0F);
        int alpha = (int) (205 * fade);
        graphics.fill(left, top, left + width, top + 66, (alpha << 24) | 0x0009111B);
        graphics.fill(left, top, left + width, top + 1, EDGE);
        graphics.fill(left, top + 65, left + width, top + 66, EDGE);
        graphics.fill(left, top, left + 2, top + 66, CYAN);
        graphics.fill(left + width - 2, top, left + width, top + 66, RED);
        graphics.drawCenteredString(Minecraft.getInstance().font, "BOSS DEFEATED",
            left + width / 2, top + 12, MUTED);
    }

    private static void renderIntro(GuiGraphics graphics, Minecraft minecraft,
                                    int centerX, int nameTop, long elapsed) {
        float progress = Mth.clamp(elapsed / (float) INTRO_END, 0.0F, 1.0F);
        float scale = 0.85F + 0.15F * progress;
        int alpha = (int) (255 * progress);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, nameTop + 5, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -(nameTop + 5), 0);
        graphics.drawCenteredString(minecraft.font, bossName, centerX, nameTop,
            (alpha << 24) | 0x00F4F7FA);
        graphics.pose().popPose();
    }

    private static void renderCuts(GuiGraphics graphics, Minecraft minecraft,
                                   int centerX, int nameTop, int textLeft, long elapsed) {
        long cutElapsed = elapsed - CUT_START;
        int completed = Mth.clamp((int) (cutElapsed / CUT_DURATION), 0, CUT_COUNT);
        int active = Math.min(completed, CUT_COUNT - 1);
        float progress = completed == CUT_COUNT ? 1.0F
            : Mth.clamp((cutElapsed - active * CUT_DURATION) / (float) CUT_DURATION, 0.0F, 1.0F);

        // Each strip is clipped from the original rendered glyphs. The clip
        // rectangle follows the strip, so the letters themselves visibly split.
        for (int part = 0; part < FRAGMENT_COUNT; part++) {
            float verticalOffset = 0.0F;
            for (int cut = 0; cut < CUT_COUNT; cut++) {
                float amount = cut < completed ? 4.5F
                    : (cut == active ? 4.5F * progress : 0.0F);
                if (part <= cut) verticalOffset -= amount;
                else verticalOffset += amount;
            }
            float horizontalOffset = (part % 2 == 0 ? -1.0F : 1.0F)
                * Math.min(5.0F, Math.max(0, completed + progress) * 0.8F);
            int clipLeft = Math.round(textLeft + horizontalOffset - 2);
            int clipTop = Math.round(nameTop + part * FRAGMENT_HEIGHT + verticalOffset);
            int clipRight = Math.round(textLeft + cachedTextWidth + horizontalOffset + 2);
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipTop + FRAGMENT_HEIGHT);
            graphics.drawCenteredString(minecraft.font, bossName,
                Math.round(centerX + horizontalOffset),
                Math.round(nameTop + verticalOffset), WHITE);
            graphics.disableScissor();
        }

        if (completed < CUT_COUNT) {
            if (active != lastSoundedCut) {
                minecraft.player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP,
                    0.82F, 0.82F + active * 0.12F);
                lastSoundedCut = active;
            }
            if (progress >= 0.78F && active != lastImpactCut) {
                minecraft.player.playSound(SoundEvents.PLAYER_ATTACK_CRIT,
                    0.68F, 0.92F + active * 0.05F);
                lastImpactCut = active;
            }

            // A single bright blade stroke sweeps across the next cut line.
            float sweep = Mth.clamp(progress, 0.0F, 1.0F);
            float x = textLeft - 30 + (cachedTextWidth + 60) * sweep;
            float y = nameTop + (active + 1) * FRAGMENT_HEIGHT;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-24.0F));
            int glow = (int) (95 + 145 * Math.sin(sweep * Math.PI));
            graphics.fill(-27, -4, 27, 4, (glow << 24) | 0x00FF354F);
            graphics.fill(-25, -1, 25, 1, 0xFFF8F4E9);
            graphics.fill(-29, -2, -24, 2, 0xFFFF5364);
            graphics.pose().popPose();
        }

        for (int cut = 0; cut < completed; cut++) {
            int seamY = nameTop + (cut + 1) * FRAGMENT_HEIGHT;
            graphics.fill(textLeft, seamY, textLeft + cachedTextWidth, seamY + 1, 0xCCFF5364);
        }
    }

    private static void renderFragments(GuiGraphics graphics, Minecraft minecraft,
                                        int centerX, int nameTop, int textLeft, long elapsed) {
        float progress = Mth.clamp((elapsed - SHATTER_START)
            / (float) (ANIMATION_END - SHATTER_START), 0.0F, 1.0F);
        float eased = progress * progress * (3.0F - 2.0F * progress);

        // Keep the same four real glyph slices from the cutting sequence.
        // No per-character rendering or rotation matrices are needed here.
        for (int part = 0; part < FRAGMENT_COUNT; part++) {
            float direction = part < 2 ? -1.0F : 1.0F;
            float side = part - 1.5F;
            float dx = direction * (18.0F + Math.abs(side) * 10.0F) * eased;
            float dy = side * 13.0F * eased + 82.0F * eased * eased;
            int alpha = 255 - (int) (255 * Mth.clamp((progress - 0.48F) / 0.52F, 0, 1));

            int clipLeft = Math.round(textLeft + dx - 2);
            int clipTop = Math.round(nameTop + part * FRAGMENT_HEIGHT + dy);
            int clipRight = Math.round(textLeft + cachedTextWidth + dx + 2);
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipTop + FRAGMENT_HEIGHT);
            graphics.drawCenteredString(minecraft.font, bossName,
                Math.round(centerX + dx), Math.round(nameTop + dy),
                (alpha << 24) | 0x00F4F7FA);
            graphics.disableScissor();
        }

        int flashAlpha = (int) (38 * (1.0F - progress));
        if (flashAlpha > 0) {
            graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                (flashAlpha << 24) | 0x00D8F4F2);
        }
    }

}
