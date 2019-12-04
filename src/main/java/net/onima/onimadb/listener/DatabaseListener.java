package net.onima.onimadb.listener;

import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent;
import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.players.APIPlayer;
import net.onima.onimaapi.players.OfflineAPIPlayer;
import net.onima.onimaapi.rank.RankType;
import net.onima.onimaapi.saver.mongo.NoSQLSaver;
import net.onima.onimaapi.utils.ConfigurationService;
import net.onima.onimaapi.utils.Methods;
import net.onima.onimaapi.utils.OSound;
import net.onima.onimaapi.utils.callbacks.VoidCallback;
import net.onima.onimaboard.players.BoardPlayer;
import net.onima.onimaboard.players.OfflineBoardPlayer;
import net.onima.onimadb.insertion.PlayerInsertion;
import net.onima.onimadb.insertion.faction.FactionInsertion;
import net.onima.onimadb.insertion.faction.PlayerFactionInsertion;
import net.onima.onimafaction.faction.Faction;
import net.onima.onimafaction.faction.PlayerFaction;

public class DatabaseListener implements Listener {
	
	@EventHandler
	public void onDatabasePreUpdate(DatabasePreUpdateEvent event) {
		NoSQLSaver saver = event.getSaver();
		VoidCallback<MongoResult> callback = result -> {
			String msg = "";
			
			if (result.isFailed()) {
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
			
			OnimaMongo.executeAsync(new PlayerInsertion(offlineBoardPlayer, callback), false);
			
			if (!offlineApiPlayer.isOnline()) {
				offlineApiPlayer.remove();
				offlineBoardPlayer.remove();
				offlineBoardPlayer.getOfflineFPlayer().remove();
			}
		} else if (saver instanceof Faction) {
			Faction faction = (Faction) saver;
			
			if (!(faction instanceof PlayerFaction))
				OnimaMongo.executeAsync(new FactionInsertion(faction, callback), false);
			else if (faction instanceof PlayerFaction)
				OnimaMongo.executeAsync(new PlayerFactionInsertion((PlayerFaction) faction, callback), false);
		}
	}
	
}
