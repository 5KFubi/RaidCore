package me.fivekfubi.raidcore;

public class EVENT_TYPE {
    public static final String JUMP                        = "JUMP";
    public static final String SNEAK_JUMP                  = "SNEAK_JUMP";
    public static final String SPRINT_JUMP                 = "SPRINT_JUMP";

    public static final String SNEAK                       = "SNEAK";
    public static final String SNEAK_TOGGLE_ON             = "SNEAK_TOGGLE_ON";
    public static final String SNEAK_TOGGLE_OFF            = "SNEAK_TOGGLE_OFF";

    public static final String SPRINT                      = "SPRINT";
    public static final String SPRINT_TOGGLE_ON            = "SPRINT_TOGGLE_ON";
    public static final String SPRINT_TOGGLE_OFF           = "SPRINT_TOGGLE_OFF";

    public static final String KILL_ENTITY                 = "KILL_ENTITY";
    public static final String SNEAK_KILL_ENTITY           = "SNEAK_KILL_ENTITY";
    public static final String SPRINT_KILL_ENTITY          = "SPRINT_KILL_ENTITY";

    public static final String TAKE_DAMAGE                 = "TAKE_DAMAGE";
    public static final String SNEAK_TAKE_DAMAGE           = "SNEAK_TAKE_DAMAGE";
    public static final String SPRINT_TAKE_DAMAGE          = "SPRINT_TAKE_DAMAGE";

    public static final String PROJECTILE_LAUNCH           = "PROJECTILE_LAUNCH";
    public static final String SNEAK_PROJECTILE_LAUNCH     = "SNEAK_PROJECTILE_LAUNCH";
    public static final String SPRINT_PROJECTILE_LAUNCH    = "SPRINT_PROJECTILE_LAUNCH";
    public static final String PROJECTILE_HIT              = "PROJECTILE_HIT";
    public static final String PROJECTILE_HIT_ENTITY       = "PROJECTILE_HIT_ENTITY";
    public static final String PROJECTILE_HIT_BLOCK        = "PROJECTILE_HIT_BLOCK";

    public static final String BLOCK_BREAK                 = "BLOCK_BREAK";
    public static final String SNEAK_BLOCK_BREAK           = "SNEAK_BLOCK_BREAK";
    public static final String SPRINT_BLOCK_BREAK          = "SPRINT_BLOCK_BREAK";
    public static final String BLOCK_PLACE                 = "BLOCK_PLACE";
    public static final String SNEAK_BLOCK_PLACE           = "SNEAK_BLOCK_PLACE";
    public static final String SPRINT_BLOCK_PLACE          = "SPRINT_BLOCK_PLACE";

    public static final String LEFT_CLICK                  = "LEFT_CLICK";
    public static final String LEFT_CLICK_AIR              = "LEFT_CLICK_AIR";
    public static final String LEFT_CLICK_BLOCK            = "LEFT_CLICK_BLOCK";
    public static final String LEFT_CLICK_ENTITY           = "LEFT_CLICK_ENTITY";
    public static final String SNEAK_LEFT_CLICK            = "SNEAK_LEFT_CLICK";
    public static final String SNEAK_LEFT_CLICK_AIR        = "SNEAK_LEFT_CLICK_AIR";
    public static final String SNEAK_LEFT_CLICK_BLOCK      = "SNEAK_LEFT_CLICK_BLOCK";
    public static final String SNEAK_LEFT_CLICK_ENTITY     = "SNEAK_LEFT_CLICK_ENTITY";
    public static final String SPRINT_LEFT_CLICK           = "SPRINT_LEFT_CLICK";
    public static final String SPRINT_LEFT_CLICK_AIR       = "SPRINT_LEFT_CLICK_AIR";
    public static final String SPRINT_LEFT_CLICK_BLOCK     = "SPRINT_LEFT_CLICK_BLOCK";
    public static final String SPRINT_LEFT_CLICK_ENTITY    = "SPRINT_LEFT_CLICK_ENTITY";

    public static final String RIGHT_CLICK                 = "RIGHT_CLICK";
    public static final String RIGHT_CLICK_AIR             = "RIGHT_CLICK_AIR";
    public static final String RIGHT_CLICK_BLOCK           = "RIGHT_CLICK_BLOCK";
    public static final String RIGHT_CLICK_ENTITY          = "RIGHT_CLICK_ENTITY";
    public static final String SNEAK_RIGHT_CLICK           = "SNEAK_RIGHT_CLICK";
    public static final String SNEAK_RIGHT_CLICK_AIR       = "SNEAK_RIGHT_CLICK_AIR";
    public static final String SNEAK_RIGHT_CLICK_BLOCK     = "SNEAK_RIGHT_CLICK_BLOCK";
    public static final String SNEAK_RIGHT_CLICK_ENTITY    = "SNEAK_RIGHT_CLICK_ENTITY";
    public static final String SPRINT_RIGHT_CLICK          = "SPRINT_RIGHT_CLICK";
    public static final String SPRINT_RIGHT_CLICK_AIR      = "SPRINT_RIGHT_CLICK_AIR";
    public static final String SPRINT_RIGHT_CLICK_BLOCK    = "SPRINT_RIGHT_CLICK_BLOCK";
    public static final String SPRINT_RIGHT_CLICK_ENTITY   = "SPRINT_RIGHT_CLICK_ENTITY";

    public static final String MIDDLE_CLICK                = "MIDDLE_CLICK";
    public static final String MIDDLE_CLICK_AIR            = "MIDDLE_CLICK_AIR";
    public static final String MIDDLE_CLICK_BLOCK          = "MIDDLE_CLICK_BLOCK";
    public static final String MIDDLE_CLICK_ENTITY         = "MIDDLE_CLICK_ENTITY";
    public static final String SNEAK_MIDDLE_CLICK          = "SNEAK_MIDDLE_CLICK";
    public static final String SNEAK_MIDDLE_CLICK_AIR      = "SNEAK_MIDDLE_CLICK_AIR";
    public static final String SNEAK_MIDDLE_CLICK_BLOCK    = "SNEAK_MIDDLE_CLICK_BLOCK";
    public static final String SNEAK_MIDDLE_CLICK_ENTITY   = "SNEAK_MIDDLE_CLICK_ENTITY";
    public static final String SPRINT_MIDDLE_CLICK         = "SPRINT_MIDDLE_CLICK";
    public static final String SPRINT_MIDDLE_CLICK_AIR     = "SPRINT_MIDDLE_CLICK_AIR";
    public static final String SPRINT_MIDDLE_CLICK_BLOCK   = "SPRINT_MIDDLE_CLICK_BLOCK";
    public static final String SPRINT_MIDDLE_CLICK_ENTITY  = "SPRINT_MIDDLE_CLICK_ENTITY";

    public static final String DOUBLE_CLICK                = "DOUBLE_CLICK";
    public static final String DOUBLE_CLICK_AIR            = "DOUBLE_CLICK_AIR";
    public static final String DOUBLE_CLICK_BLOCK          = "DOUBLE_CLICK_BLOCK";
    public static final String DOUBLE_CLICK_ENTITY         = "DOUBLE_CLICK_ENTITY";
    public static final String SNEAK_DOUBLE_CLICK          = "SNEAK_DOUBLE_CLICK";
    public static final String SNEAK_DOUBLE_CLICK_AIR      = "SNEAK_DOUBLE_CLICK_AIR";
    public static final String SNEAK_DOUBLE_CLICK_BLOCK    = "SNEAK_DOUBLE_CLICK_BLOCK";
    public static final String SNEAK_DOUBLE_CLICK_ENTITY   = "SNEAK_DOUBLE_CLICK_ENTITY";
    public static final String SPRINT_DOUBLE_CLICK         = "SPRINT_DOUBLE_CLICK";
    public static final String SPRINT_DOUBLE_CLICK_AIR     = "SPRINT_DOUBLE_CLICK_AIR";
    public static final String SPRINT_DOUBLE_CLICK_BLOCK   = "SPRINT_DOUBLE_CLICK_BLOCK";
    public static final String SPRINT_DOUBLE_CLICK_ENTITY  = "SPRINT_DOUBLE_CLICK_ENTITY";

    // block break
    // block place

    public static final String CREATIVE_CLICK              = "CREATIVE_CLICK";

    public static final String SWAP_OFFHAND                = "SWAP_OFFHAND";
    public static final String SNEAK_SWAP_OFFHAND          = "SNEAK_SWAP_OFFHAND";
    public static final String SPRINT_SWAP_OFFHAND         = "SPRINT_SWAP_OFFHAND";
    public static final String SWAP_NUMBER_KEY             = "SWAP_NUMBER_KEY";

    public static final String DROP_ITEM                   = "DROP_ITEM";
    public static final String SNEAK_DROP_ITEM             = "SNEAK_DROP_ITEM";
    public static final String SPRINT_DROP_ITEM            = "SPRINT_DROP_ITEM";

    public static final String PICKUP_ITEM                 = "PICKUP_ITEM";
    public static final String SNEAK_PICKUP_ITEM           = "SNEAK_PICKUP_ITEM";
    public static final String SPRINT_PICKUP_ITEM          = "SPRINT_PICKUP_ITEM";

    public static final String UNKNOWN                     = "UNKNOWN";
}
