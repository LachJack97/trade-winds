package com.tradewinds;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@Singleton
public class TradeWindsBankOverlay extends WidgetItemOverlay
{
    private final BankLedgerService bankLedgerService;
    private final BankTracker bankTracker;
    private final TradeWindsConfig config;
    private final Client client; // <-- add this

    @Inject
    public TradeWindsBankOverlay(
            BankLedgerService bankLedgerService,
            BankTracker bankTracker,
            TradeWindsConfig config,
            Client client
    )
    {
        this.bankLedgerService = bankLedgerService;
        this.bankTracker = bankTracker;
        this.config = config;
        this.client = client;

        showOnBank();
    }

    // -------- placeholders ----------
    private boolean isPlaceholderItem(int itemId)
    {
        if (itemId <= 0)
        {
            return false;
        }

        ItemComposition comp = client.getItemDefinition(itemId);
        if (comp == null)
        {
            return false;
        }

        return comp.getPlaceholderTemplateId() != -1;
    }

    // -------- quantity formatting K/M/B ----------
    private static String formatQuantity(long qty)
    {
        if (qty >= 1_000_000_000L)
        {
            return trimZero(String.format("%.1fB", qty / 1_000_000_000.0));
        }
        else if (qty >= 1_000_000L)
        {
            return trimZero(String.format("%.1fM", qty / 1_000_000.0));
        }
        else if (qty >= 1_000L)
        {
            return trimZero(String.format("%.1fK", qty / 1_000.0));
        }

        return Long.toString(qty);
    }

    private static String trimZero(String s)
    {
        // "1.0K" -> "1K"
        return s.replace(".0", "");
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {
        if (!config.enableLocalBanks() || !config.showLocalGlobal())
        {
            return;
        }

        // Skip placeholders completely
        if (isPlaceholderItem(itemId))
        {
            return;
        }

        int local = bankLedgerService.getLocalQuantity(itemId, bankTracker.getCurrentBankLocation());
        int global = bankLedgerService.getGlobalQuantity(itemId);

        if (global == 0)
        {
            return; // nothing to show
        }

        Rectangle bounds = itemWidget.getCanvasBounds();
        if (bounds == null)
        {
            return;
        }

        // local/global with K/M/B formatting
        String text = formatQuantity(local) + "/" + formatQuantity(global);

        Font originalFont = graphics.getFont();
        Font smallFont = originalFont.deriveFont(originalFont.getSize2D() - 1.0f);
        graphics.setFont(smallFont);

        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int padding = 1;
        int x = (int) (bounds.getX() + bounds.getWidth() - textWidth - padding);
        int y = (int) (bounds.getY() + textHeight + padding);

        // Shadow
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);

        // Foreground
        graphics.setColor(new Color(0, 255, 255));
        graphics.drawString(text, x, y);

        graphics.setFont(originalFont);

        // Dim items with zero LOCAL storage
        if (local == 0)
        {
            Color old = graphics.getColor();
            graphics.setColor(new Color(0, 0, 0, 120));
            graphics.fill(bounds);
            graphics.setColor(old);
        }
    }
}
