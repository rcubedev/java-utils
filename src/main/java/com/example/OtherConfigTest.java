package com.example;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.entity.EntityType;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.DisplayNameConvention;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.FloatRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.SerializedNameConvention;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.NamingSchemes;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import org.jetbrains.annotations.NotNull;

@DisplayNameConvention(NamingSchemes.SPACE_SEPARATED_LOWER_CASE_INITIAL_UPPER_CASE)
@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class OtherConfigTest extends WrappedConfig {

    @Comment({"Should debug messages be sent to the console?", "This option should be enabled if you are reporting an error or bug."})
    public boolean debugMode = false;

    // small section, don't make a subclass
    // todo :: maybe remove, seems useless
    @Comment("These are toggles for the different broadcast messages")
    public Map<String, Boolean> broadcast = ValueMap.builder(false)
            .put("on-load", true)
            .put("on-enable", true)
            .put("on-disable", true) // todo :: needed? is there a disable event?
            .build();

    @Comment({"This is a list of ignored dimensions for each world that CombatLogX will follow when tagging players.",
            "World names are case-sensitive. \"world\" is not the same as \"WoRlD\"",
            "Make sure you are not using the world aliases from Multiverse",
            "",
            "None disabled example (default)",
            "disabled_worlds = {}",
            "",
            "Example with some dimensions disabled",
            "[disabled_worlds]",
            "\"disabled_world_1\" = \"*\" <-- CombatLogX is disabled for all dimensions in disabled_world_1",
            "\"DiSaBlEd_WoRlD_2\" = [\"minecraft:overworld\", \"example:custom_dimension\"] <-- CombatLogX is disabled for the overworld and a custom dimension in DiSaBlEd_WoRlD_2"
    })
    public Map<String, StringOrListExt> disabledWorlds = ValueMap.builder(new StringOrListExt("", String.class)).put("test", new StringOrListExt("*", String.class)).build();

    @Comment({"This option changes the 'disabled_worlds' to a list of enabled dimensions.",
            "You can use this when you have fewer combat dimensions than disabled dimensions."
    })
    public boolean disabledWorldsInverted = false;

    @Comment({"CombatLogX can link pets, such as wolves, cats, and other animals that can be tamed, to their owner.",
            "This will only link the attacker, not the entity that was attacked."
    })
    public boolean linkPets = true;

    @Comment({"CombatLogX can link projectiles, such as arrows from skeletons, to their shooter.",
            "This will only link the attacker, not the entity that was attacked."
    })
    public boolean linkProjectiles = true;

    @Comment({"Which projectiles will be ignored when the 'link-projectiles' option is enabled?",
            "If 'link-projectiles' is false, all projectiles will be ignored.",
            "Use the Entity ID of projectile",
            "",
            "None disabled example",
            "ignored_projectiles = []",
            "",
            "Example with some projectiles disabled (default)",
            "ignoredProjectiles = [\"minecraft:egg\", \"minecraft:ender_pearl\", \"minecraft:snowball\"]"
    })
    public List<String> ignoredProjectiles = ValueList.create("", "minecraft:egg", "minecraft:ender_pearl", "minecraft:snowball");

    @Comment({"CombatLogX can link a fishing rod to the entity that cast it.",
            "This will only link the attacker, not the entity that was attacked."
    })
    public boolean linkFishingRod = true;

    @Comment({"CombatLogX can sometimes link TNT to the entity that caused it to explode.",
            "This will only link the attacker, not the entity that was attacked."
    })
    public boolean linkTNT = true;

    public TimerSection timer = new TimerSection();
    public static class TimerSection implements Section {
        // TODO: maybe remove
        @Comment({"CombatLogX has two different types of timers that you can select.",
                "GLOBAL:",
                "Every player will be tagged for the length of the 'default_timer' value.",
                "",
                "PERMISSION:",
                "Each player will be tagged for a different amount of time based on a permission.",
                "Permission Format: \"combatlogx.timer.<seconds>\"",
                "Permission Example: \"combatlogx.timer.30\"",
                "Any player that does not have a permission will be tagged with the 'default_timer' value."
        })
        public TimerType type = TimerType.GLOBAL;

        @Comment({"This is the default amount of time that players will be tagged (in seconds).",
                "This value will be used if the timer type is GLOBAL or if a permission cannot be detected."
        })
        @IntegerRange(min = 0, max = 2147483647)
        public int defaultTimer = 10;
    }

    // TODO: will this require a restart to re-register the permissions?
    @Comment({"Which permission will prevent players from being tagged into combat?",
            "You must add this permission manually.",
            "OPs do not have this permission by default.",
            "",
            "Setting the value of this option to \"\" will disable the bypass feature."
    })
    public String bypassPermission = "combatlogx.bypass";

    @Comment("Should CombatLogX tag players when they shoot themselves with a projectile?")
    public boolean selfCombat = false;

    @Comment("Should CombatLogX remove players from combat when they are killed?")
    public boolean untagOnDeath = true;

    @Comment({"Should CombatLogX remove players from combat when their enemy is killed?",
            "This also removed combat if the enemy is a creeper and decides to explode."
    })
    public boolean untagOnEnemyDeath = true;

    // TODO: Remove?
    @Comment({"How long must players wait between requests?",
            "This cooldown is for the '/clx forgive request' command and is in seconds."
    })
    public int forgiveRequestCooldown = 30;

    // TODO: remove?
    @Comment("What is the minimum server TPS to allow tagging?")
    @FloatRange(min = 0, max = 20)
    public float minimumServerTPS = 15;

    @Comment({"Which tag reasons are allowed?",
            "You can see a full list here:",
            "https://github.com/SirBlobman/CombatLogX/blob/main/api/src/main/java/com/github/sirblobman/combatlogx/api/object/TagReason.java",
            "",
            "No tag reasons enabled example",
            "enabled_tag_reasons = []",
            "",
            "Example with some tag reasons enabled",
            "enabled_tag_reasons = [\"UNKNOWN\", \"ATTACKED\"]",
            "",
            "All enabled example",
            "enabled_tag_reasons = \"*\""
    })
    // TODO: string or list?
    public StringOrListExt enabledTagReasons = new StringOrListExt("*", String.class);
    public StringOrListExt randomStringOrListTest = new StringOrListExt(List.of("val1", "val2"), String.class);

    public static class ConfigReader {
        private final OtherConfigTest configuration;

        public ConfigReader(OtherConfigTest configuration) {
            this.configuration = configuration;
        }

        public boolean isProjectileIgnored(EntityType<?> type) {
            return configuration.ignoredProjectiles.contains(EntityType.getKey(type).toString());
        }

        // todo: cache?
        // public @NotNull Set<TagReason> getEnabledTagReasons() {
        //     Set<TagReason> enabledTagReasons = ConfigurationHelper.parseEnums(configuration.enabledTagReasons, TagReason.class);
        //     return Collections.unmodifiableSet(enabledTagReasons);
        // }
    }
}
