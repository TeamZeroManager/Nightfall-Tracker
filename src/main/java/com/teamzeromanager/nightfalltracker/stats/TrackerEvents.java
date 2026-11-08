package com.teamzeromanager.nightfalltracker.stats;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TrackerEvents {
    private TrackerEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getEntity().level().isClientSide) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        Entity killer = event.getSource().getEntity();
        boolean boss = killer instanceof EnderDragon
            || killer instanceof WitherBoss
            || killer instanceof Warden;

        TrackerSavedData.get(level).recordDeath(boss);
    }
}
