package com.tradewinds;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetID;


@Singleton
public class TradeWindsRestrictionService
{
    private final Client client;
    private final TradeWindsConfig config;
    private final BankTracker bankTracker;
    private final TradeWindsMenuService menuService;

    // ---------------------------------------------
    // TELEPORT WHITELISTS (MUST BE AT CLASS LEVEL)
    // ---------------------------------------------
    /** Items allowed to *trigger* a teleport (rings, tabs, etc.) */
    private final Set<Integer> TELEPORT_ITEM_WHITELIST = new HashSet<>(Arrays.asList(
            // fill with item IDs
    ));

    /** Items allowed to be carried during teleport */
    private final Set<Integer> TELEPORT_INVENTORY_WHITELIST = new HashSet<>(Arrays.asList(
            // fill with item IDs
    ));

    @Inject
    public TradeWindsRestrictionService(Client client, TradeWindsConfig config, BankTracker bankTracker,
                                        TradeWindsMenuService menuService)
    {
        this.client = client;
        this.config = config;
        this.bankTracker = bankTracker;
        this.menuService = menuService;
    }

    public void handleMenuOptionClicked(MenuOptionClicked event)
    {
        if (config.debugMode())
        {
            return;
        }

        if (config.enableLocalBanks()
                && bankTracker.hasKnownBankLocation()
                && isBankWithdraw(event))
        {
            int itemId = event.getItemId();
            int local = bankTracker.getLocalQuantity(itemId);

            if (local <= 0)
            {
                event.consume();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: No local stock of that item at this bank.", null);
                return;
            }
        }

        if (menuService.handleMenuOption(event))
        {
            return;
        }

        if (config.debugMode())
        {
            return;
        }

        // --- TELEPORT LOGIC (NO WHITELIST DECLARATIONS HERE) -----

        if (config.restrictTeleports())
        {
            String option = event.getMenuOption().toLowerCase();
            String target = event.getMenuTarget().toLowerCase();

            boolean isTeleport =
                    option.contains("teleport") ||
                            target.contains("teleport") ||
                            (option.contains("cast") && target.contains("tele")) ||
                            (option.contains("use") && target.contains("tele")) ||
                            option.contains("rub") ||
                            option.contains("invoke");

            if (isTeleport && inventoryHasIllegalTeleportCargo())
            {
                event.consume();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: Teleport blocked (carrying cargo).", null);
                return;
            }
        }



        if (config.disableGE())
        {
            String option = event.getMenuOption().toLowerCase();
            String target = event.getMenuTarget().toLowerCase();

            if (target.contains("grand exchange") && option.contains("exchange"))
            {
                event.consume();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                        "TradeWinds: Grand Exchange is disabled.", null);
            }
        }
    }

    // ------------------------------------------------------
    // Whitelist helper methods (these stay at class level)
    // ------------------------------------------------------

    private boolean isTeleportItemAllowed(MenuOptionClicked event)
    {
        int itemId = event.getItemId();
        if (itemId <= 0)
        {
            return false; // spellbook teleports not allowed
        }

        return TELEPORT_ITEM_WHITELIST.contains(itemId);
    }

    private boolean inventoryHasIllegalTeleportCargo()
    {
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv == null)
        {
            return false;
        }

        for (Item item : inv.getItems())
        {
            int id = item.getId();
            if (id <= 0)
            {
                continue;
            }

            if (!TELEPORT_INVENTORY_WHITELIST.contains(id)
                    && !TELEPORT_ITEM_WHITELIST.contains(id))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isBankWithdraw(MenuOptionClicked event)
    {
        String option = event.getMenuOption().toLowerCase();

        if (!(option.startsWith("withdraw") || option.equals("release")))
        {
            return false;
        }

        int widgetId = event.getWidgetId();
        int groupId = widgetId >> 16;

        return groupId == WidgetID.BANK_GROUP_ID && event.getMenuAction() == MenuAction.CC_OP;
    }
}
