package com.teamzeromanager.nightfalltracker.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.teamzeromanager.nightfalltracker.NightfallTracker;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = NightfallTracker.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TrackerKeyMappings {
    public static final KeyMapping OPEN_BESTIARY = new KeyMapping(
        "key.nightfalltracker.open_bestiary",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.nightfalltracker"
    );

    private TrackerKeyMappings() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BESTIARY);
    }
}
