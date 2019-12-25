package net.onima.onimadb.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.onima.onimaapi.event.mongo.AbstractPlayerLoadEvent;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent.Action;
import net.onima.onimaapi.event.mongo.PlayerLoadEvent;
import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.api.result.MongoQueryResult;
import net.onima.onimaapi.players.APIPlayer;
import net.onima.onimaapi.players.OfflineAPIPlayer;
import net.onima.onimaapi.players.utils.SpecialPlayerInventory;
import net.onima.onimaapi.utils.ConfigurationService;
import net.onima.onimaboard.players.BoardPlayer;
import net.onima.onimaboard.players.OfflineBoardPlayer;
import net.onima.onimadb.query.PlayerQuery;
import net.onima.onimafaction.OnimaFaction;
import net.onima.onimafaction.players.FPlayer;
import net.onima.onimafaction.players.OfflineFPlayer;

public class PlayerListener implements Listener {
	
	@EventHandler
	public void onPlayerLoad(PlayerLoadEvent event) {
		UUID uuid = event.getUuid();
		
		OnimaMongo.executeAsync(new PlayerQuery(uuid, mongoResult -> {
			MongoQueryResult result = (MongoQueryResult) mongoResult;
			
			if (!result.isFailed() && !result.isEmpty()) {
				OfflineAPIPlayer offline = new OfflineAPIPlayer(uuid);

				offline.queryDatabase(result);
				new OfflineFPlayer(offline).queryDatabase(result);
				new OfflineBoardPlayer(offline).queryDatabase(result);
			}
			
			if (event instanceof AbstractPlayerLoadEvent)
				((AbstractPlayerLoadEvent) event).done();
			
		}), true);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		
		if (OfflineAPIPlayer.isLoaded(uuid)) {
			OfflineAPIPlayer.getOfflineAPIPlayers().get(uuid).setIp(event.getAddress().getHostAddress());
			return;
		}
		
		new PlayerQuery(uuid, mongoResult -> {
			MongoQueryResult result = (MongoQueryResult) mongoResult;
			
			if (result.isFailed()) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ConfigurationService.SQL_ERROR_LOADING_DATA);
				return;
			} 
			
			OfflineAPIPlayer offline = new OfflineAPIPlayer(uuid);
			OfflineFPlayer offlineF = new OfflineFPlayer(offline);
			OfflineBoardPlayer offlineB = new OfflineBoardPlayer(offline);
			
			if (!result.isEmpty()) {
				offline.queryDatabase(result);
				offlineF.queryDatabase(result);
				offlineB.queryDatabase(result);
			} else {
				if (OnimaFaction.getInstance().getEOTW().isRunning()) {
					event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "L'EOTW est en cours. Revenez demain pour le SOTW."
							+ "\n\nSeulement les joueurs qui ont déjà"
							+ "\njoué sur cette map peuvent rejoindre.");
					return;
				}
				
				Bukkit.getPluginManager().callEvent(new DatabasePreUpdateEvent(offlineB, Action.WRITE, false));
			}

			offline.setIp(event.getAddress().getHostAddress());
		}).execute();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		
		if (Bukkit.hasWhitelist() && !Bukkit.getWhitelistedPlayers().contains(player) && !player.isOp())
			return;
		
		APIPlayer apiPlayer = new APIPlayer(player);
		FPlayer fPlayer = new FPlayer(apiPlayer);
		BoardPlayer boardPlayer = new BoardPlayer(apiPlayer);
		
		apiPlayer.loadLogin();
		fPlayer.loadLogin();
		boardPlayer.loadLogin();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		BoardPlayer boardPlayer = BoardPlayer.getPlayer(player);
		
		player.setNoDamageTicks(19);
		
		boardPlayer.getApiPlayer().loadJoin();
		boardPlayer.getFPlayer().loadJoin();
		boardPlayer.loadJoin();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		SpecialPlayerInventory.onDisconnect(player);
		APIPlayer.getPlayer(player).getDisguiseManager().undisguise();
	}
	
}
