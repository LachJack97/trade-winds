package com.tradewinds;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tradewinds")
public interface TradeWindsConfig extends Config
{
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

    @ConfigItem(
            keyName = "characterId",
            name = "",
            description = ""
    )
    void characterId(String id);

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

    @ConfigItem(
            keyName = "firstRunComplete",
            name = "",
            description = ""
    )
    void firstRunComplete(boolean value);

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
    void clientUuid(String uuid);

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

    @ConfigItem(
            keyName = "accountStatus",
            name = "",
            description = ""
    )
    void accountStatus(String status);


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

    @ConfigItem(
            keyName = "brickReason",
            name = "",
            description = ""
    )
    void brickReason(String reason);

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

    @ConfigItem(
            keyName = "brickTimestamp",
            name = "",
            description = ""
    )
    void brickTimestamp(String ts);

}

