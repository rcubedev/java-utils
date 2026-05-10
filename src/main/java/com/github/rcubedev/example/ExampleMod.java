package com.github.rcubedev.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ServerLevelData;

import com.github.rcubedev.example.mixin.WrappedConfigAccessor;
import folk.sisby.kaleido.api.KaleidoConfig;
import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.Config;
import folk.sisby.kaleido.lib.quiltconfig.api.Serializer;
import folk.sisby.kaleido.lib.quiltconfig.api.serializers.TomlSerializer;
import folk.sisby.kaleido.lib.quiltconfig.impl.ConfigImpl;
import folk.sisby.kaleido.lib.quiltconfig.impl.builders.ConfigBuilderImpl;
import folk.sisby.kaleido.lib.quiltconfig.implementor_api.ConfigEnvironment;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
    public static ConfigTest CONFIG_TEST = createCfg("testconfig", ConfigTest.class);
    // public static final ConfigTest CONFIG_TEST = ConfigTest.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", "testconfig", ConfigTest.class);
    public static OtherConfigTest OTHER_CONFIG_TEST = createCfg("otherconfig", OtherConfigTest.class);
    // public static final OtherConfigTest OTHER_CONFIG_TEST = OtherConfigTest.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", "otherconfig", OtherConfigTest.class);
    public static OtherConfigTest OTHER_CONFIG_TEST2 = createCfg("otherconfig2", OtherConfigTest.class);
    // public static final OtherConfigTest OTHER_CONFIG_TEST2 = OtherConfigTest.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", "otherconfig2", OtherConfigTest.class);

    public static <T extends WrappedConfig> T createCfg(String id, Class<T> clazz) {
        try {
            return WrappedConfig.createToml(FabricLoader.getInstance().getConfigDir(), "examplemoddir", id, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T extends WrappedConfig> int reload(T config, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Config wrapped = ((WrappedConfigAccessor) config).test$getWrapped();
            if (!(wrapped instanceof ConfigImpl wrappedImpl)) throw new IllegalStateException("Unexpected config type: " + wrapped.getClass().getName());
            ConfigBuilderImpl.doInitialSerialization(wrappedImpl);
            context.getSource().sendSuccess(() -> Component.literal("Reloaded config."), false);
            return 1;
        } catch (Throwable t) {
            if (t instanceof CommandSyntaxException commandSyntaxException) throw commandSyntaxException;
            LOGGER.error("An error occurred while reloading a config.", t);
            context.getSource().sendFailure(Component.literal("An error occurred while reloading."));
            return 0;
        }
    }
	@Override
	public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(Commands.literal("reloadcfg")
                    .then(Commands.literal("testconfig").executes(ctx -> {
                        try {
                            LOGGER.info("Reloading CONFIG_TEST / testconfig. Current random: {}", CONFIG_TEST.random);
                            CONFIG_TEST.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("Attempted reload of CONFIG_TEST / testconfig.toml"), false);
                            LOGGER.info("Reloaded CONFIG_TEST / testconfig. New random: {}", CONFIG_TEST.random);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        // CONFIG_TEST.save();
                        // LOGGER.info("Reloading CONFIG_TEST / testconfig. Current random: {}", CONFIG_TEST.random);
                        // CONFIG_TEST = createCfg("testconfig", ConfigTest.class);
                        // ctx.getSource().sendSuccess(() -> Component.literal("Attempted reload of CONFIG_TEST / testconfig.toml"), false);
                        // LOGGER.info("Reloaded CONFIG_TEST / testconfig. New random: {}", CONFIG_TEST.random);
                        // // // or can manually recreate ConfigImpl like below:
                        // // Config newWrapped = ConfigImpl.create(
                        // //         KaleidoConfig.tomlEnvironment(FabricLoader.getInstance().getConfigDir()),
                        // //         "examplemoddir", "testconfig",
                        // //         WrappedConfigCreator.of(ConfigTest.class)
                        // // );
                        // // CONFIG_TEST.setWrappedConfig(newWrapped);
                        return 1;
                    }))
                    .then(Commands.literal("otherconfig").executes(ctx -> {
                        // todo so this works but is quite janky. maybe fork quilt config or add onto it similar to kaleido
                        //  to add a way to call deserialize w/o doing this jank; assuming toml, assuming dir, etc.
                        LOGGER.info("Reloading OTHER_CONFIG_TEST / otherconfig. Current linkPets: {}", OTHER_CONFIG_TEST.linkPets);
                        ConfigEnvironment environment = KaleidoConfig.tomlEnvironment(FabricLoader.getInstance().getConfigDir());
                        Serializer serializer = TomlSerializer.INSTANCE;
                        Path path = environment.getSaveDir().resolve("examplemoddir").resolve(Paths.get("otherconfig" + "." + serializer.getFileExtension()));
                        try {
                            serializer.deserialize(OTHER_CONFIG_TEST, Files.newInputStream(path));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        // CONFIG_TEST = createCfg("otherconfig", ConfigTest.class);
                        ctx.getSource().sendSuccess(() -> Component.literal("Attempted reload of OTHER_CONFIG_TEST / otherconfig.toml"), false);
                        LOGGER.info("Reloaded OTHER_CONFIG_TEST / otherconfig. New linkPets: {}", OTHER_CONFIG_TEST.linkPets);
                        return 1;
                    }))
                    .then(Commands.literal("otherconfig2").executes(ctx -> reload(OTHER_CONFIG_TEST2, ctx)))
            );
        });
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