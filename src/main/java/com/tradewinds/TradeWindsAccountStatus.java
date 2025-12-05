

package com.tradewinds;

import java.util.Set;
import java.util.HashSet;

public enum TradeWindsAccountStatus
{
    CLEAN,
    FLAGGED,
    BRICKED,
    UNKNOWN;

    public static TradeWindsAccountStatus fromString(String value)
    {
        if (value == null || value.isEmpty())
        {
            return UNKNOWN;
        }

        switch (value.toUpperCase())
        {
            case "CLEAN":
                return CLEAN;
            case "FLAGGED":
                return FLAGGED;
            case "BRICKED":
                return BRICKED;
            default:
                return UNKNOWN;
        }
    }

    public String toConfigValue()
    {
        switch (this)
        {
            case CLEAN:
                return "CLEAN";
            case FLAGGED:
                return "FLAGGED";
            case BRICKED:
                return "BRICKED";
            case UNKNOWN:
            default:
                return "";
        }
    }

    public enum TWFlag {
        NON_TW_DROP_PICKUP,
        SUSPICIOUS_XP_JUMP,
        BANK_OUTSIDE_REGION,
        TRADE_WITH_NON_TW,
        ILLEGAL_TELEPORT_ATTEMPT,
    }

    private Set<TWFlag> flags = new HashSet<>();

    public void addFlag(TWFlag flag)
    {
        flags.add(flag);
        // optional: if too many flags → brick
    }
}
