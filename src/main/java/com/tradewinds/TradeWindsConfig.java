package com.tradewinds;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tradewinds")
public interface TradeWindsConfig extends Config
{
    // Public constant for the config group name, used by ConfigManager in TradeWindsAuthService
    String GROUP = "tradewinds";

    @ConfigItem(
            keyName = "enableLocalBanks",
            name = "Enable Localised Banks",
            description = "Track and display separate bank inventories per bank location",
            position = 1,
            hidden = true
    )
    default boolean enableLocalBanks()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showLocalGlobal",
            name = "Show Local/Global Counts",
            description = "Display an overlay showing local vs global bank quantities",
            position = 2,
            hidden = true
    )
    default boolean showLocalGlobal()
    {
        return true;
    }

    @ConfigItem(
            keyName = "restrictTeleports",
            name = "Restrict Teleports With Cargo",
            description = "Block teleports while carrying non-whitelisted cargo",
            position = 3
    )
    default boolean restrictTeleports()
    {
        return true;
    }

    @ConfigItem(
            keyName = "disableGE",
            name = "Disable Grand Exchange",
            description = "Prevent access to GE NPC and interface",
            position = 4,
            hidden = true
    )
    default boolean disableGE()
    {
        return false; // optional for Phase 1
    }

    // --- DEBUG / DEV ---

    @ConfigItem(
            keyName = "debugMode",
            name = "Debug / Seeding mode",
            description = "Developer mode: seed balances from current bank snapshot and do NOT enforce restrictions.",
            position = 50
    )
    default boolean debugMode()
    {
        return false;
    }

    // Internal storage for balances JSON (we don't actually read this; it just reserves the key)
    @ConfigItem(
            keyName = "balances",
            name = "Balances JSON",
            description = "Internal storage for TradeWinds bank balances",
            hidden = true
    )
    default String balancesJson()
    {
        return "";
    }

    // AUTH / FIRST-TIME SETUP STATE
    // ============================================================


    @ConfigItem(
            keyName = "characterId",
            name = "Character ID",
            description = "Internal Trade Winds character identifier returned by the backend",
            hidden = true
    )
    default String characterId()
    {
        return "";
    }

    // REMOVED: void characterId(String id); (The setter is removed, forcing per-account storage)

    @ConfigItem(
            keyName = "firstRunComplete",
            name = "First run complete",
            description = "Whether first-time setup has been completed for this client",
            hidden = true
    )
    default boolean firstRunComplete()
    {
        return false;
    }

    // REMOVED: void firstRunComplete(boolean value); (The setter is removed, authentication state is now per-account)

    @ConfigItem(
            keyName = "clientUuid",
            name = "Client UUID",
            description = "Random UUID generated on first run for this RuneLite installation",
            hidden = true
    )
    default String clientUuid()
    {
        return "";
    }

    @ConfigItem(
            keyName = "clientUuid",
            name = "",
            description = ""
    )
    void clientUuid(String uuid); // This remains as it is a client-wide ID

    @ConfigItem(
            keyName = "accountStatus",
            name = "Account Status",
            description = "Current TradeWinds account status",
            hidden = true
    )
    default String accountStatus()
    {
        return "";
    }

    // REMOVED: void accountStatus(String status); (The setter is removed)


    @ConfigItem(
            keyName = "brickReason",
            name = "Brick Reason",
            description = "Internal reason this account was bricked.",
            hidden = true
    )
    default String brickReason()
    {
        return "";
    }

    // REMOVED: void brickReason(String reason); (The setter is removed)

    @ConfigItem(
            keyName = "brickTimestamp",
            name = "Brick Time",
            description = "When the account was bricked.",
            hidden = true
    )
    default String brickTimestamp()
    {
        return "";
    }

    // REMOVED: void brickTimestamp(String ts); (The setter is removed)

}