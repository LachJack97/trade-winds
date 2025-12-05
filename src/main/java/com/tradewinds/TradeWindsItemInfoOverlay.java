package com.tradewinds;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.ItemQuantityMode;


import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Singleton
public class TradeWindsItemInfoOverlay extends OverlayPanel
{
    private final TradeWindsMenuService menuService;
    private final Client client;

    @Inject
    public TradeWindsItemInfoOverlay(TradeWindsMenuService menuService, Client client)
    {
        this.menuService = menuService;
        this.client = client;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Make sure vanilla yellow stack text is disabled while the bank is open
        hideVanillaBankQuantities();

        ItemLocationInfo info = menuService.getActiveItemInfo();
        if (info == null)
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(220, 0));

        // Title: item name
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(info.getName())
                        .color(Color.WHITE)
                        .build()
        );

        // Per-bank lines
        for (Map.Entry<BankLocation, Integer> entry : info.getPerBank().entrySet())
        {
            int qty = entry.getValue();
            if (qty <= 0)
            {
                continue;
            }

            String bankName = entry.getKey().name().toLowerCase().replace('_', ' ');
            bankName = bankName.substring(0, 1).toUpperCase() + bankName.substring(1);

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(bankName)
                            .right(Integer.toString(qty))
                            .build()
            );
        }

        return super.render(graphics);
    }

    // ============================================================
    // Hide vanilla bank stack quantities
    // ============================================================

    private void hideVanillaBankQuantities()
    {
        Widget container = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        if (container == null)
        {
            return; // bank not open
        }

        Widget[] children = container.getDynamicChildren();
        if (children == null)
        {
            return;
        }

        for (Widget child : children)
        {
            // Only touch real items
            if (child.getItemId() > 0 &&
                    child.getItemQuantityMode() != ItemQuantityMode.NEVER)
            {
                child.setItemQuantityMode(ItemQuantityMode.NEVER);
            }
        }
    }
}
