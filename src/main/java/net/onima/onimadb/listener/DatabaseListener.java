package net.onima.onimadb.listener;

import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.onima.onimaapi.OnimaAPI;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent;
import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.mongo.saver.NoSQLSaver;
import net.onima.onimaapi.players.APIPlayer;
import net.onima.onimaapi.players.OfflineAPIPlayer;
import net.onima.onimaapi.rank.RankType;
import net.onima.onimaapi.utils.ConfigurationService;
import net.onima.onimaapi.utils.Methods;
import net.onima.onimaapi.utils.OSound;
import net.onima.onimaapi.utils.callbacks.VoidCallback;
import net.onima.onimaboard.players.BoardPlayer;
import net.onima.onimaboard.players.OfflineBoardPlayer;
import net.onima.onimadb.deleter.FactionDelete;
import net.onima.onimadb.deleter.PlayerFactionDelete;
import net.onima.onimadb.insertion.PlayerInsertion;
import net.onima.onimadb.insertion.faction.FactionInsertion;
import net.onima.onimadb.insertion.faction.PlayerFactionInsertion;
import net.onima.onimafaction.faction.Faction;
import net.onima.onimafaction.faction.PlayerFaction;

public class DatabaseListener implements Listener {
	
	@EventHandler(priority = EventPriority.HIGHEST) 
	public void onDatabasePreUpdate(DatabasePreUpdateEvent event) {
		if (event.getAction() != DatabasePreUpdateEvent.Action.WRITE)
			return;
		
		NoSQLSaver saver = event.getSaver();
		VoidCallback<MongoResult> callback = result -> {
			if (result.isFailed()) {
				String msg = "";
				
				if (saver instanceof BoardPlayer)
					((BoardPlayer) saver).getApiPlayer().sendMessage(msg = "§f[" + Methods.toFormatDate(System.currentTimeMillis(), ConfigurationService.DATE_FORMAT_HOURS) + "] §cIl y a eu un problème lors de l'envoi de données de vers la BDD, veuillez screen ce message et contacter un développeur/administrateur.");
				else
					msg = "§f[" + Methods.toFormatDate(System.currentTimeMillis(), ConfigurationService.DATE_FORMAT_HOURS) + "] §cIl y a eu un problème lors de l'envoi de données de vers la BDD, veuillez screen ce message et contacter un développeur/administrateur.";
				
				for (APIPlayer player : APIPlayer.getOnlineAPIPlayers()) {
					if (player.getRank().getRankType().isAtLeast(RankType.TRIAL_MOD)) {
						player.sendMessage(msg);
						new OSound(Sound.NOTE_PIANO, 0.1F, 2.0F).play(player);
					}
				}	
			}
		};
		
		if (saver instanceof OfflineBoardPlayer) {
			OfflineBoardPlayer offlineBoardPlayer = (OfflineBoardPlayer) saver;
			OfflineAPIPlayer offlineApiPlayer = offlineBoardPlayer.getOfflineApiPlayer();
			MongoAccessor accessor = new PlayerInsertion(offlineBoardPlayer, callback);
			
			if (event.shouldRunAsync())
				OnimaMongo.executeAsync(accessor, false);
			else
				accessor.execute();
			
			if (!offlineApiPlayer.isOnline()) {
				offlineApiPlayer.remove();
				offlineBoardPlayer.remove();
				offlineBoardPlayer.getOfflineFPlayer().remove();
			}
		} else if (saver instanceof Faction) {
			Faction faction = (Faction) saver;
			MongoAccessor accessor = null;
			
			if (!(faction instanceof PlayerFaction))
				accessor = new FactionInsertion(faction, callback);
			else if (faction instanceof PlayerFaction)
				accessor = new PlayerFactionInsertion((PlayerFaction) faction, callback);
			
			if (event.shouldRunAsync())
				OnimaMongo.executeAsync(accessor, false);
			else
				accessor.execute();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDatabasePreDelete(DatabasePreUpdateEvent event) {
		if (event.getAction() != DatabasePreUpdateEvent.Action.DELETE)
			return;
		
		NoSQLSaver saver = event.getSaver();
		VoidCallback<MongoResult> callback = result -> {
			if (result.isFailed()) {
				String msg = "§f[" + Methods.toFormatDate(System.currentTimeMillis(), ConfigurationService.DATE_FORMAT_HOURS) + "] §cIl y a eu un problème lors de la suppression de données de vers la BDD, veuillez screen ce message et contacter un développeur/administrateur.";
				
				for (APIPlayer player : APIPlayer.getOnlineAPIPlayers()) {
					if (player.getRank().getRankType().isAtLeast(RankType.TRIAL_MOD)) {
						player.sendMessage(msg);
						new OSound(Sound.NOTE_PIANO, 0.1F, 2.0F).play(player);
					}
				}	
			} else {
				if (OnimaAPI.getSavers().contains(saver))
					OnimaAPI.getSavers().remove(saver);
				else if (OnimaAPI.getShutdownSavers().contains(saver))
					OnimaAPI.getShutdownSavers().remove(saver);
			}
				
		};
		
		if (saver instanceof Faction) {
			Faction faction = (Faction) saver;
			MongoAccessor accessor = null;
			
			if (!(faction instanceof PlayerFaction))
				accessor = new FactionDelete(faction, result -> result.isFailed());
			else if (faction instanceof PlayerFaction)
				accessor = new PlayerFactionDelete((PlayerFaction) faction, callback);
			
			if (event.shouldRunAsync())
				OnimaMongo.executeAsync(accessor, false);
			else
				accessor.execute();
		}
	}
	
}
