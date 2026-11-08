package com.teamzeromanager.nightfalltracker.bestiary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class BossRegistrySavedData extends SavedData {
    private static final String DATA_NAME = "nightfall_tracker_bestiary";
    private final Map<ResourceLocation, BossRecord> bosses = new LinkedHashMap<>();

    public static BossRegistrySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            BossRegistrySavedData::load,
            BossRegistrySavedData::new,
            DATA_NAME
        );
    }

    public static BossRegistrySavedData load(CompoundTag tag) {
        BossRegistrySavedData data = new BossRegistrySavedData();
        ListTag list = tag.getList("Bosses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag bossTag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(bossTag.getString("Id"));
            if (id != null) {
                data.bosses.put(id, new BossRecord(id, bossTag.getBoolean("Discovered"),
                    bossTag.getBoolean("Defeated")));
            }
        }
        return data;
    }

    public void discover(EntityType<?> type) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        BossRecord record = bosses.computeIfAbsent(id, key -> new BossRecord(key, false, false));
        if (!record.discovered) {
            record.discovered = true;
            setDirty();
        }
    }

    public void markDefeated(EntityType<?> type) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        BossRecord record = bosses.computeIfAbsent(id, key -> new BossRecord(key, true, false));
        boolean changed = !record.discovered || !record.defeated;
        record.discovered = true;
        record.defeated = true;
        if (changed) setDirty();
    }

    public BossRecord getRecord(ResourceLocation id) {
        return bosses.get(id);
    }

    public Map<ResourceLocation, BossRecord> getBosses() {
        return Collections.unmodifiableMap(bosses);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BossRecord record : bosses.values()) {
            CompoundTag bossTag = new CompoundTag();
            bossTag.putString("Id", record.id.toString());
            bossTag.putBoolean("Discovered", record.discovered);
            bossTag.putBoolean("Defeated", record.defeated);
            list.add(bossTag);
        }
        tag.put("Bosses", list);
        return tag;
    }

    public static final class BossRecord {
        private final ResourceLocation id;
        private boolean discovered;
        private boolean defeated;

        private BossRecord(ResourceLocation id, boolean discovered, boolean defeated) {
            this.id = id;
            this.discovered = discovered;
            this.defeated = defeated;
        }

        public ResourceLocation getId() { return id; }
        public boolean isDiscovered() { return discovered; }
        public boolean isDefeated() { return defeated; }
    }
}
