package com.tradewinds;

import java.time.Instant;
import java.util.Map;

public class ItemLocationInfo
{
    private final int itemId;
    private final String name;
    private final Map<BankLocation, Integer> perBank;
    private final Instant created;

    public ItemLocationInfo(int itemId, String name, Map<BankLocation, Integer> perBank)
    {
        this.itemId = itemId;
        this.name = name;
        this.perBank = perBank;
        this.created = Instant.now();
    }

    public int getItemId()
    {
        return itemId;
    }

    public String getName()
    {
        return name;
    }

    public Map<BankLocation, Integer> getPerBank()
    {
        return perBank;
    }

    public Instant getCreated()
    {
        return created;
    }
}
