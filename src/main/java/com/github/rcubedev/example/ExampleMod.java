package com.github.rcubedev.example;

import net.minecraft.world.level.storage.ServerLevelData;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ConfigTest CONFIG_TEST = ConfigTest.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", "testconfig", ConfigTest.class);
    public static final OtherConfigTest OTHER_CONFIG_TEST = OtherConfigTest.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", "otherconfig", OtherConfigTest.class);


	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
        LOGGER.info("Disabled worlds: {}", OTHER_CONFIG_TEST.disabledWorlds.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue().getRepresentation().toString()).toList());
        // ElementOrList<String> test = new ElementOrList<>("", String.class);
        // LOGGER.info("ElementOrList<String>: {}", test.getClass().getSimpleName());
        // LOGGER.info("New Instance ElementOrList<String>: {}", test.newInstance("B").getClass().getSimpleName());

        ServerLivingEntityEvents.AFTER_DAMAGE.register(((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            LOGGER.info("Damage taken!");
            LOGGER.info("World Name: {}", ((ServerLevelData)entity.level().getLevelData()).getLevelName());
            LOGGER.info("Dimension Name: {}", entity.level().dimension().identifier());
            LOGGER.info("Entity type: {}", entity.getType());
            if (source.getEntity() != null) LOGGER.info("Hurt by entity type: {}", source.getEntity().getType());
            if (source.getDirectEntity() != null) LOGGER.info("Hurt by direct entity type: {}", source.getDirectEntity().getType());
        }));
	}
}