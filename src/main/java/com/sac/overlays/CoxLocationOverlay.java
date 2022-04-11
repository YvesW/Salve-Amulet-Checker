package com.sac.overlays;

import com.google.inject.Inject;
import com.sac.SalveAmuletCheckerConfig;
import com.sac.SalveAmuletCheckerPlugin;
import lombok.val;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class CoxLocationOverlay extends OverlayPanel {

    private final Client client;
    private final SalveAmuletCheckerPlugin plugin;
    private final SalveAmuletCheckerConfig config;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;
    private final PanelComponent panelImages = new PanelComponent();

    @Inject
    private CoxLocationOverlay(Client client, SalveAmuletCheckerPlugin plugin, SalveAmuletCheckerConfig config, ItemManager itemManager, SpriteManager spriteManager) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.LOW);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Mystic Salve Amulet Checker Overlay"));

    }

    @Override
    public Dimension render(Graphics2D graphics) {

        if (config.isLocationVisibleInCox() && client.getSelectedSceneTile() != null && plugin.coxManager.isPlayerInCoxRaid()) {
            val currentRoom = plugin.coxManager.getCurrentRoom(client.getSelectedSceneTile());
            if (currentRoom != null) {
                panelComponent.getChildren().add(TitleComponent.builder()
                        .text(Text.titleCase(currentRoom))
                        .color(Color.white)
                        .build());
            }

        }
        return super.render(graphics);
    }
}
