package com.tradewinds;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;

@Slf4j
@Singleton
public class BankTracker
{
    private final Client client;
    private final TradeWindsConfig config;
    private final BankLedgerService bankLedgerService;

    @Getter
    private BankLocation currentBankLocation;

    @Inject
    public BankTracker(Client client, TradeWindsConfig config, BankLedgerService bankLedgerService)
    {
        this.client = client;
        this.config = config;
        this.bankLedgerService = bankLedgerService;
    }

    public void handleWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == WidgetID.BANK_GROUP_ID && client.getLocalPlayer() != null)
        {
            WorldPoint wp = client.getLocalPlayer().getWorldLocation();
            int region = wp.getRegionID();

            currentBankLocation = BankLocation.fromWorldPoint(wp);
            log.info("Bank opened at location: {} (region {}) world ({}, {})",
                    currentBankLocation, region, wp.getX(), wp.getY());
        }
    }

    public void handleItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.enableLocalBanks())
        {
            return;
        }

        if (event.getContainerId() != InventoryID.BANK.getId())
        {
            return;
        }

        ItemContainer container = event.getItemContainer();
        if (container == null || currentBankLocation == null)
        {
            return;
        }

        Map<Integer, Integer> newGlobalTotals = new HashMap<>();
        for (Item item : container.getItems())
        {
            if (item.getId() > 0)
            {
                newGlobalTotals.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }

        bankLedgerService.reconcileWithGlobalTotals(currentBankLocation, newGlobalTotals);
        bankLedgerService.saveBalances();
    }

    public boolean hasKnownBankLocation()
    {
        return currentBankLocation != null && currentBankLocation != BankLocation.UNKNOWN;
    }

    public int getLocalQuantity(int itemId)
    {
        return bankLedgerService.getLocalQuantity(itemId, currentBankLocation);
    }

    public void reset()
    {
        currentBankLocation = null;
    }
}
