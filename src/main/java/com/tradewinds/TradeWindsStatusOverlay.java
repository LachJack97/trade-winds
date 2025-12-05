package com.tradewinds;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.awt.*;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;

import net.runelite.client.ui.overlay.*;

@Singleton
public class TradeWindsStatusOverlay extends Overlay
{
    private final Client client;
    private final TradeWindsAuthService authService;

    @Inject
    public TradeWindsStatusOverlay(Client client, TradeWindsAuthService authService)
    {
        this.client = client;
        this.authService = authService;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGHEST);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {

        if (!authService.isAuthenticated())
        {
            return null;
        }

        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        for (Player p : client.getPlayers())
        {
            if (p == null || p.getName() == null) continue;

            String norm = TradeWindsAuthService.normalizeUsername(p.getName());

            TradeWindsAccountStatus status = authService.getStatusOf(norm);
            if (status == null) continue; // user not using plugin

            // Color based on status
            Color color;
            switch (status)
            {
                case CLEAN:
                    color = Color.GREEN;
                    break;
                case FLAGGED:
                    color = Color.ORANGE;
                    break;
                case BRICKED:
                    color = Color.RED;
                    break;
                default:
                    continue;
            }

            LocalPoint lp = p.getLocalLocation();
            if (lp == null) continue;

            int z = p.getLogicalHeight() + 40;
            net.runelite.api.Point pt = Perspective.localToCanvas(
                    client, lp, client.getPlane(), z
            );

            if (pt == null) continue;

            int r = 6;
            g.setColor(color);
            g.fillOval(pt.getX() - r, pt.getY() - r, r * 2, r * 2);

            g.setColor(Color.BLACK);
            g.drawOval(pt.getX() - r, pt.getY() - r, r * 2, r * 2);
        }

        return null;
    }
}
