package com.teamzeromanager.nightfalltracker.client;

import com.teamzeromanager.nightfalltracker.bestiary.BossRegistrySavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BestiaryScreen extends Screen {
    private static final int PANEL = 0xE8111926;
    private static final int BORDER = 0xFF34485D;
    private static final int ACCENT = 0xFF55D6D0;
    private static final int MUTED = 0xFF9BAFC2;
    private static final int WHITE = 0xFFF2F6FA;
    private static final int GREEN = 0xFF79D99A;
    private static final int GOLD = 0xFFFFC857;
    private static final int RED = 0xFFFF7777;

    private List<EntityType<?>> bossTypes = List.of();
    private Map<ResourceLocation, BossRegistrySavedData.BossRecord> records = Map.of();
    private int scrollOffset;

    public BestiaryScreen() {
        super(Component.translatable("screen.nightfalltracker.bestiary"));
    }

    @Override
    protected void init() {
        refreshData();
    }

    private void refreshData() {
        List<EntityType<?>> found = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.is(Tags.EntityTypes.BOSSES) || type == EntityType.WARDEN) {
                found.add(type);
            }
        }
        found.sort(Comparator.comparing(type ->
            BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        bossTypes = found;

        MinecraftServer server = minecraft == null ? null : minecraft.getSingleplayerServer();
        if (server != null) {
            ServerLevel overworld = server.overworld();
            records = BossRegistrySavedData.get(overworld).getBosses();
        } else {
            records = Map.of();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(430, width - 32);
        int panelHeight = Math.min(330, height - 32);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 2, ACCENT);
        graphics.fill(left, top + 2, left + 1, bottom - 1, BORDER);
        graphics.fill(right - 1, top + 2, right, bottom - 1, BORDER);
        graphics.fill(left, bottom - 1, right, bottom, BORDER);

        graphics.drawCenteredString(font, title, width / 2, top + 13, WHITE);
        graphics.drawCenteredString(font,
            Component.translatable("screen.nightfalltracker.bestiary.subtitle"),
            width / 2, top + 29, MUTED);

        int discoveredCount = 0;
        int defeatedCount = 0;
        for (EntityType<?> type : bossTypes) {
            BossRegistrySavedData.BossRecord record =
                records.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            if (record != null && record.isDiscovered()) discoveredCount++;
            if (record != null && record.isDefeated()) defeatedCount++;
        }
        String progress = discoveredCount + "/" + bossTypes.size()
            + "  •  " + defeatedCount + " "
            + Component.translatable("screen.nightfalltracker.bestiary.defeated").getString();
        graphics.drawCenteredString(font, progress, width / 2, top + 44, GOLD);

        int listTop = top + 64;
        int listBottom = bottom - 25;
        graphics.enableScissor(left + 12, listTop, right - 12, listBottom);
        int rowY = listTop - scrollOffset;
        for (EntityType<?> type : bossTypes) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            BossRegistrySavedData.BossRecord record = records.get(id);
            boolean discovered = record != null && record.isDiscovered();
            boolean defeated = record != null && record.isDefeated();

            if (rowY + 22 >= listTop && rowY <= listBottom) {
                graphics.fill(left + 12, rowY, right - 12, rowY + 21, 0x88212E3D);
                String name = discovered
                    ? Component.translatable(type.getDescriptionId()).getString()
                    : Component.translatable("screen.nightfalltracker.bestiary.unknown").getString();
                graphics.drawString(font, name, left + 20, rowY + 6,
                    discovered ? WHITE : MUTED, false);
                String state = !discovered
                    ? Component.translatable("screen.nightfalltracker.bestiary.undiscovered").getString()
                    : defeated
                        ? Component.translatable("screen.nightfalltracker.bestiary.defeated").getString()
                        : Component.translatable("screen.nightfalltracker.bestiary.pending").getString();
                int stateColor = !discovered ? MUTED : defeated ? GREEN : GOLD;
                graphics.drawString(font, state, right - 20 - font.width(state), rowY + 6,
                    stateColor, false);
            }
            rowY += 24;
        }
        graphics.disableScissor();

        graphics.drawCenteredString(font,
            Component.translatable("screen.nightfalltracker.bestiary.close_hint"),
            width / 2, bottom - 16, MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, bossTypes.size() * 24 - (Math.min(330, height - 32) - 89));
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - delta * 24));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
