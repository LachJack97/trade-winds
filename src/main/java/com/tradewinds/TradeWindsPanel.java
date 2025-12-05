package com.tradewinds;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;

public class TradeWindsPanel extends PluginPanel
{
    private static final String CARD_REGISTRATION = "registration";
    private static final String CARD_MAIN = "main";

    private final TradeWindsPlugin plugin;
    private final TradeWindsConfig config;
    private final TradeWindsAuthService authService;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // Registration UI
    private JLabel regStatusLabel;
    private JButton registerButton;

    // Main UI
    private JLabel mainStatusLabel;

    public TradeWindsPanel(TradeWindsPlugin plugin, TradeWindsConfig config, TradeWindsAuthService authService)
    {
        this.plugin = plugin;
        this.config = config;
        this.authService = authService;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        buildUi();

        // Show the right card based on current auth state
        refreshState();

        // --- NEW: auto-refresh the panel every second ---
        Timer autoRefresh = new Timer(1000, e -> refreshState());
        autoRefresh.setRepeats(true);
        autoRefresh.start();
    }

    private void buildUi()
    {
        // Top title
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Trade Winds");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><body style='width:180px'>Localised banks, teleport restrictions, and GE limitations.</body></html>");
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(10));

        add(header, BorderLayout.NORTH);

        // Cards
        cardsPanel.add(buildRegistrationCard(), CARD_REGISTRATION);
        cardsPanel.add(buildMainCard(), CARD_MAIN);

        add(cardsPanel, BorderLayout.CENTER);
    }

    // ============================================================
    // REGISTRATION CARD
    // ============================================================

    private JPanel buildRegistrationCard()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        regStatusLabel = new JLabel();
        regStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel helper = new JLabel("<html><body style='width:200px'>Add this character while on Tutorial Island to initialise Trade Winds tracking.</body></html>");
        helper.setFont(helper.getFont().deriveFont(11f));
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);

        registerButton = new JButton("Add character");
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> beginRegistrationFlow());

        panel.add(regStatusLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(helper);
        panel.add(Box.createVerticalStrut(10));
        panel.add(registerButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ============================================================
    // MAIN CARD (POST-REGISTRATION UI)
    // ============================================================

    private JPanel buildMainCard()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        mainStatusLabel = new JLabel();
        mainStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel info = new JLabel("<html><body style='width:200px'>Trade Winds is active on this character. All restrictions and tracking are now enforced.</body></html>");
        info.setFont(info.getFont().deriveFont(11f));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(mainStatusLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(info);
        panel.add(Box.createVerticalGlue());

        // Later you can add more controls here: toggles, summaries, etc.

        return panel;
    }

    // ============================================================
    // REGISTRATION FLOW
    // ============================================================

    private void beginRegistrationFlow()
    {
        // Disable the button and show "Registering..." feedback
        registerButton.setEnabled(false);
        regStatusLabel.setText("Status: Registering character...");

        // Kick off the registration logic in the plugin/service
        plugin.triggerRegistration();

        // Poll for completion since registration happens on a background thread
        Timer pollTimer = new Timer(1000, e ->
        {
            if (authService.isAuthenticated())
            {
                ((Timer) e.getSource()).stop();
                // Swap to main UI
                refreshState();
            }
        });
        pollTimer.setRepeats(true);
        pollTimer.start();
    }

    // ============================================================
    // PUBLIC: refresh UI based on auth state
    // ============================================================

    /**
     * Called by plugin or by itself to redraw based on auth state.
     */
    public void refreshState()
    {
        boolean authed = authService.isAuthenticated();
        String characterId = config.characterId();

        if (authed)
        {
            // Show main card
            cardLayout.show(cardsPanel, CARD_MAIN);

            String displayId = characterId;
            if (displayId != null && displayId.length() > 10)
            {
                displayId = displayId.substring(0, 10) + "...";
            }

            TradeWindsAccountStatus status = authService.getAccountStatus();

            String statusText;
            switch (status)
            {
                case CLEAN:
                    statusText = "Clean";
                    break;
                case FLAGGED:
                    statusText = "Flagged";
                    break;
                case BRICKED:
                    statusText = "Bricked";
                    break;
                case UNKNOWN:
                default:
                    statusText = "Unknown";
                    break;
            }

            mainStatusLabel.setText(
                    String.format("Status: %s (ID: %s)", statusText, displayId != null ? displayId : "unknown")
            );
        }

        else
        {
            // Show registration card
            cardLayout.show(cardsPanel, CARD_REGISTRATION);

            regStatusLabel.setText("Status: Not registered");
            registerButton.setEnabled(true);
            registerButton.setText("Add character");
        }

        revalidate();
        repaint();
    }
}
