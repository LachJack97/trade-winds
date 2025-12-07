package com.tradewinds;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

import net.runelite.api.Client;
import net.runelite.api.GameState;

import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;

import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
        name = "TradeWinds",
        description = "Localised banks, teleport restrictions, GE limitations, and player authenticity"
)
public class TradeWindsPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private EventBus eventBus;
    @Inject

    private TradeWindsBankOverlay bankOverlay;
    @Inject
    private TradeWindsItemInfoOverlay itemInfoOverlay;
    @Inject
    private BankLedgerService bankLedgerService;
    @Inject
    private BankTracker bankTracker;
    @Inject
    private TradeWindsMenuService menuService;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private TradeWindsConfig config;
    @Inject
    private TradeWindsStatusLookupService statusLookupService;
    @Inject
    private TradeWindsAuthService authService;
    @Inject
    private TradeWindsStatusOverlay statusOverlay;
    @Inject
    private TradeWindsRestrictionService restrictionService;

    private TradeWindsStorage storage;
    private TradeWindsPanel panel;
    private NavigationButton navButton;

    private boolean balancesLoaded = false;

    @Override
    protected void startUp() {
        log.info("TradeWinds plugin started.");

        // --- STORAGE INIT (OLD WORKING SYSTEM) ---
        storage = new TradeWindsStorage(RuneLite.RUNELITE_DIR, client);
        bankLedgerService.setStorage(storage);


        // --- ADD OVERLAYS ---
        overlayManager.add(bankOverlay);
        overlayManager.add(itemInfoOverlay);
        overlayManager.add(statusOverlay);

        // --- REGISTER EVENT-BASED SERVICES ---
        eventBus.register(itemInfoOverlay);
        eventBus.register(menuService);
        eventBus.register(bankTracker);

        // --- PANEL + SIDEBAR NAV ---
        panel = new TradeWindsPanel(this, config, authService);
//
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "tradewinds.png");
        navButton = NavigationButton.builder()
                .tooltip("Trade Winds")
                .icon(icon)
                .panel(panel)
                .priority(5)
                .build();


        clientToolbar.addNavigation(navButton);

        balancesLoaded = false;
    }

    @Override
    protected void shutDown() {
        log.info("TradeWinds plugin stopped.");

        overlayManager.remove(bankOverlay);
        overlayManager.remove(itemInfoOverlay);
        overlayManager.remove(statusOverlay);


        eventBus.unregister(itemInfoOverlay);
        eventBus.unregister(menuService);
        eventBus.unregister(bankTracker);

        bankLedgerService.saveBalances();
        bankLedgerService.clear();
        bankTracker.reset();

        balancesLoaded = false;

        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        panel = null;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // --- REALTIME PRESENCE BROADCAST ---
        if (client.getLocalPlayer() != null && authService.isAuthenticated())
        {
            String username = TradeWindsAuthService.normalizeUsername(
                    client.getLocalPlayer().getName()
            );

        }

        // --- TEMP DEBUG ---
        log.info("TW onGameTick: balancesLoaded={} isAuthenticated={}",
                balancesLoaded, authService.isAuthenticated());

        // --- LOAD BANK BALANCES ON FIRST TICK LOGGED IN ---
        if (!balancesLoaded
                && client.getGameState() == GameState.LOGGED_IN
                && client.getLocalPlayer() != null)
        {
            balancesLoaded = true;

            log.info("Player detected: loading TradeWinds balances...");

            clientThread.invokeLater(() ->
            {
                try {
                    bankLedgerService.loadBalances();
                    authService.debugPrintAuthState();
                }
                catch (Exception e) {
                    log.warn("TradeWinds: loadBalances() failed", e);
                }
            });
        }



        // --- AUTH STATUS POLLING (LOW FREQUENCY) ---
        authService.pollStatusIfDue();

        // --- PANEL REFRESH ---
        if (panel != null)
        {
            panel.refreshState();
        }

        statusLookupService.pumpQueueIfDue();

    }



    // -----------------------
    // BANK + MENU EVENTS
    // -----------------------
    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        bankTracker.handleWidgetLoaded(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        bankTracker.handleItemContainerChanged(event);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        menuService.handleMenuEntryAdded(event);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        restrictionService.handleMenuOptionClicked(event);
    }

    // -----------------------
    // CONFIG + REGISTRATION
    // -----------------------
    @Provides
    TradeWindsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TradeWindsConfig.class);
    }

    public void triggerRegistration() {
        // Ensure registration (and its chat messages) run on the client thread
        clientThread.invoke(() -> authService.startFirstTimeRegistration());
    }
}
