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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BestiaryScreen extends Screen {
    private static final int PANEL = 0xEF111925;
    private static final int PANEL_INNER = 0xCC1A2635;
    private static final int BORDER = 0xFF34485D;
    private static final int ACCENT = 0xFF55D6D0;
    private static final int MUTED = 0xFF9BAFC2;
    private static final int WHITE = 0xFFF2F6FA;
    private static final int GREEN = 0xFF79D99A;
    private static final int GOLD = 0xFFFFC857;
    private static final int RED = 0xFFFF7777;

    private List<EntityType<?>> bossTypes = List.of();
    private Map<ResourceLocation, BossRegistrySavedData.BossRecord> records = Map.of();
    private ResourceLocation selectedId;
    private int scrollOffset;
    public BestiaryScreen() {
        super(Component.translatable("screen.nightfalltracker.bestiary"));
    }

    @Override
    protected void init() {
        refreshData();
        if (selectedId == null && !bossTypes.isEmpty()) {
            selectedId = BuiltInRegistries.ENTITY_TYPE.getKey(bossTypes.get(0));
        }
    }

    private void refreshData() {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.is(Tags.EntityTypes.BOSSES)
                || type == EntityType.ENDER_DRAGON
                || type == EntityType.WITHER
                || type == EntityType.WARDEN) {
                ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            }
        }

        MinecraftServer server = minecraft == null ? null : minecraft.getSingleplayerServer();
        if (server != null) {
            ServerLevel overworld = server.overworld();
            records = BossRegistrySavedData.get(overworld).getBosses();
            ids.addAll(records.keySet());
        } else {
            records = Map.of();
        }

        List<EntityType<?>> found = new ArrayList<>();
        for (ResourceLocation id : ids) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(found::add);
        }
        found.sort(Comparator.comparing(type ->
            BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        bossTypes = found;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(570, width - 24);
        int panelHeight = Math.min(370, height - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 2, ACCENT);
        graphics.fill(left, top + 2, left + 1, bottom - 1, BORDER);
        graphics.fill(right - 1, top + 2, right, bottom - 1, BORDER);
        graphics.fill(left, bottom - 1, right, bottom, BORDER);

        graphics.drawCenteredString(font, title, width / 2, top + 12, WHITE);
        graphics.drawCenteredString(font,
            Component.translatable("screen.nightfalltracker.bestiary.subtitle"),
            width / 2, top + 27, MUTED);

        int discovered = 0;
        int defeated = 0;
        for (EntityType<?> type : bossTypes) {
            BossRegistrySavedData.BossRecord record =
                records.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            if (record != null && record.isDiscovered()) discovered++;
            if (record != null && record.isDefeated()) defeated++;
        }
        String progress = discovered + "/" + bossTypes.size()
            + "  •  " + defeated + " " +
            Component.translatable("screen.nightfalltracker.bestiary.defeated").getString();
        graphics.drawCenteredString(font, progress, width / 2, top + 43, GOLD);

        int listLeft = left + 12;
        int listRight = left + 224;
        int listTop = top + 63;
        int listBottom = bottom - 24;
        graphics.fill(listLeft, listTop, listRight, listBottom, PANEL_INNER);
        graphics.drawString(font,
            Component.translatable("screen.nightfalltracker.bestiary.catalog"),
            listLeft + 8, listTop + 7, ACCENT, false);

        int rowsTop = listTop + 22;
        graphics.enableScissor(listLeft + 3, rowsTop, listRight - 3, listBottom - 3);
        int rowY = rowsTop - scrollOffset;
        for (EntityType<?> type : bossTypes) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            BossRegistrySavedData.BossRecord record = records.get(id);
            boolean isSelected = id.equals(selectedId);
            boolean known = record != null && record.isDiscovered();
            boolean isDefeated = record != null && record.isDefeated();

            if (rowY + 22 >= rowsTop && rowY <= listBottom) {
                int rowColor = isSelected ? 0xFF294354 : 0x88212E3D;
                graphics.fill(listLeft + 4, rowY, listRight - 4, rowY + 21, rowColor);
                if (isSelected) graphics.fill(listLeft + 4, rowY, listLeft + 6, rowY + 21, ACCENT);

                String name = known
                    ? Component.translatable(type.getDescriptionId()).getString()
                    : Component.translatable("screen.nightfalltracker.bestiary.unknown").getString();
                graphics.drawString(font, trim(name, 23), listLeft + 11, rowY + 6,
                    known ? WHITE : MUTED, false);
                String marker = !known ? "?" : isDefeated ? "✓" : "•";
                int markerColor = !known ? MUTED : isDefeated ? GREEN : GOLD;
                graphics.drawString(font, marker, listRight - 16, rowY + 6, markerColor, false);
            }
            rowY += 24;
        }
        graphics.disableScissor();

        int detailLeft = listRight + 12;
        int detailRight = right - 12;
        int detailTop = listTop;
        int detailBottom = listBottom;
        graphics.fill(detailLeft, detailTop, detailRight, detailBottom, PANEL_INNER);

        EntityType<?> selectedType = selectedId == null ? null
            : BuiltInRegistries.ENTITY_TYPE.getOptional(selectedId).orElse(null);
        if (selectedType != null) {
            BossRegistrySavedData.BossRecord record = records.get(selectedId);
            boolean known = record != null && record.isDiscovered();
            boolean isDefeated = record != null && record.isDefeated();

            String displayName = known
                ? Component.translatable(selectedType.getDescriptionId()).getString()
                : Component.translatable("screen.nightfalltracker.bestiary.unknown").getString();
            int nameX = detailLeft + 13;
            int nameY = detailTop + 16;
            graphics.drawString(font, trim(displayName, 34), nameX, nameY, WHITE, false);

            int y = detailTop + 43;
            graphics.drawString(font,
                Component.translatable("screen.nightfalltracker.bestiary.status"),
                detailLeft + 13, y, MUTED, false);
            String status = !known
                ? Component.translatable("screen.nightfalltracker.bestiary.undiscovered").getString()
                : isDefeated
                    ? Component.translatable("screen.nightfalltracker.bestiary.defeated").getString()
                    : Component.translatable("screen.nightfalltracker.bestiary.pending").getString();
            graphics.drawString(font, status, detailLeft + 13, y + 13,
                !known ? MUTED : isDefeated ? GREEN : GOLD, false);

            y += 43;
            graphics.fill(detailLeft + 13, y, detailRight - 13, y + 1, BORDER);
            y += 12;
            graphics.drawString(font,
                Component.translatable("screen.nightfalltracker.bestiary.mod_source"),
                detailLeft + 13, y, MUTED, false);
            graphics.drawString(font, selectedId.getNamespace(),
                detailLeft + 13, y + 13, ACCENT, false);

            y += 39;
            graphics.drawString(font,
                Component.translatable("screen.nightfalltracker.bestiary.entity_id"),
                detailLeft + 13, y, MUTED, false);
            graphics.drawString(font, trim(selectedId.toString(), 38),
                detailLeft + 13, y + 13, WHITE, false);

            y += 39;
            String registration = record != null && record.isManual()
                ? Component.translatable("screen.nightfalltracker.bestiary.manual").getString()
                : Component.translatable("screen.nightfalltracker.bestiary.automatic").getString();
            graphics.drawString(font, registration, detailLeft + 13, y, MUTED, false);

            if (record != null && record.isManual()) {
                graphics.drawString(font,
                    Component.translatable("screen.nightfalltracker.bestiary.manual_hint"),
                    detailLeft + 13, y + 14, GOLD, false);
            }
        } else {
            graphics.drawCenteredString(font,
                Component.translatable("screen.nightfalltracker.bestiary.empty"),
                (detailLeft + detailRight) / 2, detailTop + 55, MUTED);
        }

        graphics.drawCenteredString(font,
            Component.translatable("screen.nightfalltracker.bestiary.close_hint"),
            width / 2, bottom - 15, MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String trim(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(1, maxChars - 3)) + "...";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelWidth = Math.min(570, width - 24);
            int panelHeight = Math.min(370, height - 24);
            int left = (width - panelWidth) / 2;
            int top = (height - panelHeight) / 2;
            int listLeft = left + 12;
            int listRight = left + 224;
            int listTop = top + 63;
            int listBottom = top + panelHeight - 24;
            int rowsTop = listTop + 22;
            if (mouseX >= listLeft && mouseX <= listRight &&
                mouseY >= rowsTop && mouseY <= listBottom) {
                int index = (int) ((mouseY - rowsTop + scrollOffset) / 24);
                if (index >= 0 && index < bossTypes.size()) {
                    selectedId = BuiltInRegistries.ENTITY_TYPE.getKey(bossTypes.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int panelHeight = Math.min(370, height - 24);
        int visibleHeight = panelHeight - 109;
        int max = Math.max(0, bossTypes.size() * 24 - visibleHeight);
        scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - delta * 24));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
