package com.teamzeromanager.nightfalltracker.stats;

import com.teamzeromanager.nightfalltracker.bestiary.BossRegistrySavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TrackerEvents {
    private TrackerEvents() {}

    private static boolean isBoss(Entity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES)
            || entity.getType() == net.minecraft.world.entity.EntityType.ENDER_DRAGON
            || entity.getType() == net.minecraft.world.entity.EntityType.WITHER
            || entity.getType() == net.minecraft.world.entity.EntityType.WARDEN;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof LivingEntity living && isBoss(living)) {
            BossRegistrySavedData.get(level).discover(living.getType());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim.level() instanceof ServerLevel level)) return;

        if (victim instanceof Player) {
            Entity killer = event.getSource().getEntity();
            boolean boss = killer != null && isBoss(killer);
            TrackerSavedData.get(level).recordDeath(boss);
        }

        if (victim instanceof LivingEntity living && isBoss(living)) {
            BossRegistrySavedData.get(level).markDefeated(living.getType());
        }
    }
}
