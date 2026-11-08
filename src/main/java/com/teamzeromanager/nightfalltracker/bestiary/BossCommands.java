package com.teamzeromanager.nightfalltracker.bestiary;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.teamzeromanager.nightfalltracker.NightfallTracker;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossCommands {
    private BossCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nightfalltracker")
            .then(Commands.literal("boss")
                .then(Commands.literal("add")
                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                        .executes(context -> {
                            ResourceLocation id = ResourceLocationArgument.getId(context, "entity");
                            Optional<?> optional = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
                            if (optional.isEmpty()) {
                                context.getSource().sendFailure(Component.literal(
                                    "No se encontró la entidad: " + id));
                                return 0;
                            }
                            Object value = optional.get();
                            if (!(value instanceof net.minecraft.world.entity.EntityType<?> type)) return 0;
                            ServerLevel level = context.getSource().getLevel();
                            BossRegistrySavedData.get(level).registerManual(type);
                            context.getSource().sendSuccess(() -> Component.literal(
                                "Jefe añadido al Bestiario: " + id), true);
                            return 1;
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                        .executes(context -> {
                            ResourceLocation id = ResourceLocationArgument.getId(context, "entity");
                            ServerLevel level = context.getSource().getLevel();
                            BossRegistrySavedData data = BossRegistrySavedData.get(level);
                            BossRegistrySavedData.BossRecord record = data.getBosses().get(id);
                            if (record == null || !record.isManual()) {
                                context.getSource().sendFailure(Component.literal(
                                    "Ese jefe no está registrado manualmente: " + id));
                                return 0;
                            }
                            data.removeManual(id);
                            context.getSource().sendSuccess(() -> Component.literal(
                                "Registro manual eliminado: " + id), true);
                            return 1;
                        })))));
    }
}
