package com.tradewinds;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;

@Singleton
public class TradeWindsMenuService
{
    private static final Duration ITEM_INFO_LIFETIME = Duration.ofSeconds(10);

    private final Client client;
    private final BankLedgerService bankLedgerService;
    private final BankTracker bankTracker;
    private final TradeWindsConfig config;

    private ItemLocationInfo activeItemInfo;

    @Inject
    public TradeWindsMenuService(
            Client client,
            BankLedgerService bankLedgerService,
            BankTracker bankTracker,
            TradeWindsConfig config
    )
    {
        this.client = client;
        this.bankLedgerService = bankLedgerService;
        this.bankTracker = bankTracker;
        this.config = config;
    }

    // ------------------------------------------------------------
    // Add "TradeWinds locations" entry on Examine
    // ------------------------------------------------------------

    public void handleMenuEntryAdded(MenuEntryAdded event)
    {
        if (event.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId())
        {
            return;
        }

        if (!"Examine".equals(event.getOption()))
        {
            return;
        }

        int widgetId = event.getActionParam1();
        int groupId = widgetId >> 16;
        if (groupId != WidgetID.BANK_GROUP_ID)
        {
            return;
        }

        int slot = event.getActionParam0();

        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null || slot < 0 || slot >= bank.size())
        {
            return;
        }

        Item item = bank.getItem(slot);
        if (item == null || item.getId() <= 0)
        {
            return;
        }

        client.createMenuEntry(-1)
                .setOption("TradeWinds locations")
                .setTarget(event.getTarget())
                .setIdentifier(item.getId())  // this is the itemId for our RUNELITE action
                .setParam0(slot)
                .setParam1(widgetId)
                .setType(MenuAction.RUNELITE);
    }

    // ------------------------------------------------------------
    // Strip Withdraw-X from the right-click bank menu
    // ------------------------------------------------------------

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] entries = event.getMenuEntries();
        if (entries == null || entries.length == 0)
        {
            return;
        }

        List<MenuEntry> filtered = new ArrayList<>(entries.length);

        for (MenuEntry e : entries)
        {
            // Bank item widget id is stored in param1
            int widgetId = e.getParam1();
            int groupId = WidgetInfo.TO_GROUP(widgetId);

            if (groupId == WidgetID.BANK_GROUP_ID && "Withdraw-X".equals(e.getOption()))
            {
                // Skip this entry – removes X from the bank context menu
                continue;
            }

            filtered.add(e);
        }

        event.setMenuEntries(filtered.toArray(new MenuEntry[0]));
    }

    // ------------------------------------------------------------
    // Enforce local-bank withdraw limits
    // ------------------------------------------------------------

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        // First, handle our custom RUNELITE menu option
        if (handleMenuOption(event))
        {
            return;
        }

        // Only enforce when local banks feature is enabled
        if (!config.enableLocalBanks())
        {
            return;
        }

        String option = event.getMenuOption();
        if (option == null || !option.startsWith("Withdraw"))
        {
            return;
        }

        // Only bank item options
        int widgetId = event.getParam1();
        int groupId = WidgetInfo.TO_GROUP(widgetId);
        if (groupId != WidgetID.BANK_GROUP_ID)
        {
            return;
        }

        // For bank widget actions, param0 is the slot index.
        int slot = event.getParam0();
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null || slot < 0 || slot >= bank.size())
        {
            return;
        }

        Item item = bank.getItem(slot);
        if (item == null || item.getId() <= 0)
        {
            return;
        }

        int itemId = item.getId();

        int localQty  = bankLedgerService.getLocalQuantity(itemId, bankTracker.getCurrentBankLocation());
        int globalQty = bankLedgerService.getGlobalQuantity(itemId);

        // No local stock at all -> block any Withdraw
        if (localQty <= 0)
        {
            event.consume();
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: You don't have any of this item in this bank.", null);
            return;
        }

        // Strip the "Withdraw-" prefix
        String suffix = option.substring("Withdraw-".length());

        // 1) NUMERIC withdraws: "Withdraw-1", "Withdraw-5", "Withdraw-10",
        //    and also "Withdraw-100000" when X is set in the bank UI.
        int requested = -1;
        try
        {
            // Remove commas from "100,000" etc.
            String numeric = suffix.replace(",", "");
            requested = Integer.parseInt(numeric);
        }
        catch (NumberFormatException ignored)
        {
            // not a plain number, fall through to the keyword cases below
        }

        if (requested > 0)
        {
            if (requested > localQty)
            {
                event.consume();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: You only have " + localQty + " of this item in this bank.", null);
            }
            return;
        }

        // 2) Keyword withdraws: All / All-but-1 / X
        switch (suffix)
        {
            case "All":
            case "All-but-1":
            case "X":
            {
                // These can withdraw the full global stack. Only allow them
                // at the bank that owns the entire stack.
                if (globalQty > localQty)
                {
                    event.consume();
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                            "TradeWinds: '" + option + "' is only available at the bank that holds this stack.", null);
                }
                break;
            }

            default:
                // Tags, Examine, etc. – let them through
                break;
        }
    }


    // ------------------------------------------------------------
    // Handle RUNELITE "TradeWinds locations" option
    // ------------------------------------------------------------

    public boolean handleMenuOption(MenuOptionClicked event)
    {
        if (event.getMenuAction() == MenuAction.RUNELITE
                && event.getMenuOption().equals("TradeWinds locations"))
        {
            int itemId = event.getId(); // identifier we set above
            printItemLocationsToChat(itemId);
            event.consume();
            return true;
        }

        return false;
    }

    // ------------------------------------------------------------
    // Item info panel backing data
    // ------------------------------------------------------------

    public ItemLocationInfo getActiveItemInfo()
    {
        if (activeItemInfo == null)
        {
            return null;
        }

        if (Duration.between(activeItemInfo.getCreated(), Instant.now())
                .compareTo(ITEM_INFO_LIFETIME) > 0)
        {
            activeItemInfo = null;
            return null;
        }

        return activeItemInfo;
    }

    public void showItemLocations(int itemId)
    {
        Map<BankLocation, Integer> perBank = bankLedgerService.getPerBank(itemId);
        if (perBank == null || perBank.isEmpty())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: No known storage for that item yet.", null);
            activeItemInfo = null;
            return;
        }

        Map<BankLocation, Integer> copy = new EnumMap<>(BankLocation.class);
        for (Entry<BankLocation, Integer> e : perBank.entrySet())
        {
            if (e.getValue() > 0)
            {
                copy.put(e.getKey(), e.getValue());
            }
        }

        if (copy.isEmpty())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: No known storage for that item yet.", null);
            activeItemInfo = null;
            return;
        }

        String name = client.getItemDefinition(itemId).getName();
        activeItemInfo = new ItemLocationInfo(itemId, name, copy);

        StringBuilder sb = new StringBuilder("TradeWinds: ");
        boolean first = true;
        for (Entry<BankLocation, Integer> e : copy.entrySet())
        {
            if (!first)
            {
                sb.append(", ");
            }
            first = false;
            sb.append(e.getKey().name()).append("=").append(e.getValue());
        }
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", sb.toString(), null);
    }

    public void printItemLocationsToChat(int itemId)
    {
        Map<BankLocation, Integer> perBank = bankLedgerService.getPerBank(itemId);
        if (perBank == null || perBank.isEmpty())
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: No known storage for that item yet.", null);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Entry<BankLocation, Integer> e : perBank.entrySet())
        {
            int qty = e.getValue();
            if (qty <= 0)
            {
                continue;
            }

            if (sb.length() > 0)
            {
                sb.append(", ");
            }

            String bankName = e.getKey().name().toLowerCase().replace('_', ' ');
            bankName = bankName.substring(0, 1).toUpperCase() + bankName.substring(1);

            sb.append(bankName).append(": ").append(qty);
        }

        if (sb.length() == 0)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "TradeWinds: No known storage for that item yet.", null);
            return;
        }

        String itemName = client.getItemDefinition(itemId).getName();
        client.addChatMessage(
                ChatMessageType.GAMEMESSAGE,
                "",
                "TradeWinds – " + itemName + " > " + sb,
                null
        );
    }
}
