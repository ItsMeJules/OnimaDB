package net.onima.onimadb;

import java.util.concurrent.CopyOnWriteArraySet;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mongodb.MongoException;

import net.onima.onimaapi.OnimaAPI;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent.Action;
import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.OnimaMongo.OnimaCollection;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoQueryResult;
import net.onima.onimaapi.mongo.saver.NoSQLSaver;
import net.onima.onimaapi.players.APIPlayer;
import net.onima.onimaapi.report.Report;
import net.onima.onimaapi.saver.FileSaver;
import net.onima.onimaapi.saver.Saver;
import net.onima.onimaapi.utils.ConfigurationService;
import net.onima.onimaapi.utils.Methods;
import net.onima.onimaboard.players.BoardPlayer;
import net.onima.onimadb.listener.DatabaseListener;
import net.onima.onimadb.listener.DisableListener;
import net.onima.onimadb.listener.PlayerListener;
import net.onima.onimadb.query.PlayerQuery;
import net.onima.onimadb.query.faction.FactionQuery;
import net.onima.onimafaction.faction.Faction;
import net.onima.onimafaction.faction.PlayerFaction;
import net.onima.onimafaction.players.FPlayer;

public class OnimaDB extends JavaPlugin {
	
	private static OnimaDB instance;
	
	@Override
	public void onEnable() {
		if (!Bukkit.getOnlineMode()) {
			getPluginLoader().disablePlugin(this);
			return;
		}
		
		long started = System.currentTimeMillis();
		OnimaAPI.sendConsoleMessage("====================§6[§3ACTIVATION§6]§r====================", ConfigurationService.ONIMABOARD_PREFIX);
		instance = this;
		
		for (Document document : OnimaMongo.get(OnimaCollection.FACTIONS).find()) {
			MongoAccessor query = new FactionQuery(document, mongoResult ->  { 
				MongoQueryResult result = (MongoQueryResult) mongoResult;
				
				if (!result.isFailed())
					new Faction(result.getValue("name", String.class)).queryDatabase(result);
			});
			
			try {
				query.execute();
			} catch (MongoException e) {
				query.getResult().setFailed(true);
				e.printStackTrace();
			}
		}
		
		for (Document document : OnimaMongo.get(OnimaCollection.PLAYER_FACTIONS).find()) {
			MongoAccessor query = new FactionQuery(document, mongoResult -> {
				MongoQueryResult result = (MongoQueryResult) mongoResult;
				
				if (!result.isFailed())
					new PlayerFaction(result.getValue("name", String.class)).queryDatabase(result);
			});
			
			try {
				query.execute();
			} catch (MongoException e) {
				query.getResult().setFailed(true);
				e.printStackTrace();
			}
		}
		
		for (Player player : Methods.getOnlinePlayers(null)) {
			MongoAccessor query = new PlayerQuery(player.getUniqueId(), mongoResult -> {
				MongoQueryResult result = (MongoQueryResult) mongoResult;
				
				if (!result.isFailed() && !result.isEmpty()) {
					APIPlayer apiPlayer = new APIPlayer(player);
					FPlayer fPlayer = new FPlayer(apiPlayer);
					BoardPlayer boardPlayer = new BoardPlayer(apiPlayer);
					
					apiPlayer.queryDatabase(result);
					fPlayer.queryDatabase(result);
					boardPlayer.queryDatabase(result);
					
					apiPlayer.loadLogin();
					fPlayer.loadLogin();
					boardPlayer.loadLogin();
					
					player.setNoDamageTicks(19);
					
					apiPlayer.loadJoin();
					fPlayer.loadJoin();
					boardPlayer.loadJoin();
				}
				
			});
			
			try {
				query.execute();
			} catch (MongoException e) {
				query.getResult().setFailed(true);
				e.printStackTrace();
			}
		}
		
		Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
		Bukkit.getPluginManager().registerEvents(new DatabaseListener(), this);
		Bukkit.getPluginManager().registerEvents(new DisableListener(), this);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> saveAll(), 20L * 60 * 5, 20L * 60 * 5);
		Bukkit.getScheduler().runTaskLater(this, () -> Report.deserialize(), 3 * 20L);
		
		OnimaAPI.setLastSave(System.currentTimeMillis());
		OnimaAPI.sendConsoleMessage("====================§6[§3ACTIVE EN (" + (System.currentTimeMillis() - started) + "ms)§6]§r====================", ConfigurationService.ONIMABOARD_PREFIX);
	}

	public void onDisable() {
		OnimaAPI.sendConsoleMessage("====================§6[§cDESACTIVATION§6]§r====================", ConfigurationService.ONIMABOARD_PREFIX);
	}
	
	private void saveAll() {
		long saveStarted = System.currentTimeMillis();
		
		OnimaAPI.sendConsoleMessage("§3Sauvegarde du serveur en cours...", ConfigurationService.ONIMADB_PREFIX);
		for (Saver saver : new CopyOnWriteArraySet<>(OnimaAPI.getSavers())) {
			if (saver instanceof NoSQLSaver) {
				NoSQLSaver mongoSaver = (NoSQLSaver) saver;
				
				Bukkit.getPluginManager().callEvent(new DatabasePreUpdateEvent(mongoSaver, mongoSaver.shouldDelete() ? Action.DELETE : Action.WRITE, false));
			} else if (saver instanceof FileSaver)
				((FileSaver) saver).serialize();
			
		}
		
		long now = System.currentTimeMillis();
		
		OnimaAPI.sendConsoleMessage("§3Sauvegarde du serveur finie ! §7(" + (now - saveStarted) + "ms)", ConfigurationService.ONIMADB_PREFIX);
		OnimaAPI.setLastSave(now);
	}
	
	public static OnimaDB getInstance() {
		return instance;
	}
	
}
