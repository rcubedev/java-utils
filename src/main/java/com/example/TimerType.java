package com.example;

/**
 * The type of timer that will be used by CombatLogX
 */
public enum TimerType implements ISerializableEnum<TimerType> {
    /**
     * Every player will be tagged for the same amount of time.
     */
    GLOBAL,

    /**
     * Some players will have special combat times based on their permissions, others will use the global time.
     */
    PERMISSION;
}
