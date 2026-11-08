package com.teamzeromanager.nightfalltracker;

import com.teamzeromanager.nightfalltracker.stats.TrackerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(NightfallTracker.MOD_ID)
public class NightfallTracker {
    public static final String MOD_ID = "nightfalltracker";

    public NightfallTracker() {
        MinecraftForge.EVENT_BUS.register(TrackerEvents.class);
    }
}
