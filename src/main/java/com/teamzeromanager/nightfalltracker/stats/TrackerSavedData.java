package com.teamzeromanager.nightfalltracker.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class TrackerSavedData extends SavedData {
    private static final String DATA_NAME = "nightfall_tracker_stats";
    private long totalDeaths;
    private long bossDeaths;

    public static TrackerSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            TrackerSavedData::load,
            TrackerSavedData::new,
            DATA_NAME
        );
    }

    public static TrackerSavedData load(CompoundTag tag) {
        TrackerSavedData data = new TrackerSavedData();
        data.totalDeaths = tag.getLong("TotalDeaths");
        data.bossDeaths = tag.getLong("BossDeaths");
        return data;
    }

    public long getTotalDeaths() {
        return totalDeaths;
    }

    public long getBossDeaths() {
        return bossDeaths;
    }

    public void recordDeath(boolean boss) {
        totalDeaths++;
        if (boss) bossDeaths++;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("TotalDeaths", totalDeaths);
        tag.putLong("BossDeaths", bossDeaths);
        return tag;
    }
}
