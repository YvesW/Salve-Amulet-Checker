package com.sac;

import com.google.inject.Provides;
import javax.inject.Inject;

import com.sac.constants.EntityNames;
import com.sac.enums.TobState;
import com.sac.infoboxs.SalveAmuletCheckerInfoBox;
import com.sac.managers.CoxManager;
import com.sac.managers.TobManager;
import com.sac.models.SaRaider;
import com.sac.overlays.CoxLocationOverlay;
import com.sac.overlays.MysticRoomOverlay;
import com.sac.panel.SalveAmuletCheckerPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.kit.KitType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.grounditems.GroundItemsOverlay;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
import net.runelite.client.plugins.raids.Raid;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
	name = "SalveAmuletChecker"
)
public class SalveAmuletCheckerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SalveAmuletCheckerConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	public TobManager tobManager;

	@Inject
	public CoxManager coxManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MysticRoomOverlay mysticRoomOverlay;
	@Inject
	private CoxLocationOverlay coxLocationOverlay;


	@Inject
	private RuneLiteConfig runeLiteConfig;

	private TobState currentTobState;
	private SalveAmuletCheckerPanel panel;
	private NavigationButton navButton;
	private ArrayList<SaRaider> saRaiders;
	private SalveAmuletCheckerInfoBox coxSalveAmuletCheckerInfoBox;
	private SalveAmuletCheckerInfoBox tobSalveAmuletCheckerInfoBox;
	private SalveAmuletCheckerInfoBox locationSalveAmuletInfoBox;

	//private final BufferedImage coxBufferedImage = itemManager.getImage(ItemID.SALVE_AMULETEI);

	@Override
	protected void startUp() throws Exception
	{
		// Can't @Inject because it is nulled out in shutdown()
		panel = injector.getInstance(SalveAmuletCheckerPanel.class);

		InputStream in = SalveAmuletCheckerPlugin.class.getResourceAsStream("salveAmuletEi.png");

		BufferedImage ICON = ImageUtil.loadImageResource(SalveAmuletCheckerPlugin.class, "salveAmuletEi.png");
		navButton = NavigationButton.builder()
				.tooltip("Salve Amulet Checker")
				.icon(ICON)
				.priority(10)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navButton);
		log.info("Salve Amulet Checker started!");

		overlayManager.add(mysticRoomOverlay);
		overlayManager.add(coxLocationOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		log.info("Salve Amulet Checker stopped!");
		removeInfoBox(coxSalveAmuletCheckerInfoBox);
		overlayManager.remove(mysticRoomOverlay);
		overlayManager.remove(coxLocationOverlay);
	}


	@Provides
	SalveAmuletCheckerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SalveAmuletCheckerConfig.class);
	}

	@Subscribe(priority = -1)
	public void onGameTick(GameTick event) {
		computeActiveCheck();
	}

	private void computeActiveCheck(){
		if(config.isEnabledInTob()){
			currentTobState = tobManager.getTobState();
			if (currentTobState != TobState.InTob){
				return;
			}
			panel.setActiveMonster(EntityNames.BLOAT, true);
			tobManager.LoadRaiders();
			if(tobManager.GetRoom() == EntityNames.BLOAT){
				//do check for players without salve
				for (Player player: client.getPlayers()) {
					if(tobManager.getRaiderNames().contains(player.getName())){
						if(!isSalveAmuletEquipped(player)){
							whenSalveAmuletNotEquipped(player);
						}
					}
				}
			}
		}
		else if(config.isEnabledInCox()){
			panel.setActiveMonster(EntityNames.MYSTIC, true);
			if(coxManager.isPlayerInCoxParty()){
				coxManager.setUpRaidParty(client.getSelectedSceneTile());
			}
			val currentRoom = coxManager.getCurrentRoom(client.getSelectedSceneTile());
			if(currentRoom != null){
				addCoxSalveAmuletCheckerInfoBox(currentRoom.name(),currentRoom.name(),Color.white);
			}
			val playersMap = coxManager.getPlayersInMysticRoom();
			playersMap.forEach((player,isInMysticRoom) -> {
				addPlayerSalveAmuletCheckerInfoBox(player.getName(),isInMysticRoom.toString(), Color.black);
				if(isInMysticRoom){
					 if(!isSalveAmuletEquipped(player)) {
						 log.debug(player.getName());
						 if(config.isToxic()){
							 whenSalveAmuletNotEquipped(player);
						 }
					 }
				}
			});
		}

	}

	public boolean isSalveAmuletEquipped(Player player){
		 int itemId = player.getPlayerComposition().getEquipmentId(KitType.AMULET);
		 return isSalveAmulet(itemId);
	}

	private void whenSalveAmuletNotEquipped(Player offendingPlayer){
		client.addChatMessage(ChatMessageType.GAMEMESSAGE,offendingPlayer.getName(),String.format("%s Is not wearing their Salve Amulet!",offendingPlayer.getName()),null);
	}

	public boolean isSalveAmulet(int itemId){
		boolean isSalveAmulet = false;
		if(itemId == ItemID.SALVE_AMULET || itemId == ItemID.SALVE_AMULET_E || itemId == ItemID.SALVE_AMULETEI){
			isSalveAmulet = true;
		}
		return isSalveAmulet;
	}

	private SalveAmuletCheckerInfoBox addInfoBox(BufferedImage image, String text, String toolTip){
		val infoBox = new SalveAmuletCheckerInfoBox(image,this);
		infoBox.setInfoBoxText(text);
		infoBox.setTooltip(toolTip);
		return infoBox;
	}

	private void removeInfoBox(InfoBox infoBox){
		infoBoxManager.removeInfoBox(infoBox);
		infoBox = null;
	}

	private SalveAmuletCheckerInfoBox createInfoBox(BufferedImage image, String text, String toolTip, Color textBoxColor){
		val salveAmuletInfoBox = new SalveAmuletCheckerInfoBox(image,this);
		salveAmuletInfoBox.setInfoBoxText(text);
		salveAmuletInfoBox.setTooltip(toolTip);
		salveAmuletInfoBox.setInfoBoxTextColor(textBoxColor);
		return salveAmuletInfoBox;
	}

	private void addCoxSalveAmuletCheckerInfoBox(String text, String toolTip, Color color) {
		removeInfoBox(locationSalveAmuletInfoBox);
		locationSalveAmuletInfoBox = addInfoBox(itemManager.getImage(ItemID.SALVE_AMULETEI), text, toolTip);
		infoBoxManager.addInfoBox(locationSalveAmuletInfoBox);
	}

	private void addPlayerSalveAmuletCheckerInfoBox(String text, String toolTip, Color color){
		removeInfoBox(coxSalveAmuletCheckerInfoBox);
		coxSalveAmuletCheckerInfoBox = addInfoBox(itemManager.getImage(ItemID.DRAGON_2H_SWORD), text, toolTip);
		infoBoxManager.addInfoBox(coxSalveAmuletCheckerInfoBox);
	}

}
