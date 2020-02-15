package net.onima.onimadb.listener;

import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import net.onima.onimaapi.OnimaAPI;
import net.onima.onimaapi.caching.UUIDCache;
import net.onima.onimaapi.disguise.DisguiseManager;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent;
import net.onima.onimaapi.event.mongo.DatabasePreUpdateEvent.Action;
import net.onima.onimaapi.mongo.saver.NoSQLSaver;
import net.onima.onimaapi.players.APIPlayer;
import net.onima.onimaapi.saver.FileSaver;

public class DisableListener implements Listener {
	
	@EventHandler
	public void onDisable(PluginDisableEvent event) {
		for (String name : DisguiseManager.getDisguisedPlayers().values())
			APIPlayer.getPlayer(UUIDCache.getUUID(name)).getDisguiseManager().undisguise();

		if (event.getPlugin().getName().equalsIgnoreCase("OnimaDB")) {
			new CopyOnWriteArraySet<>(OnimaAPI.getSavers()).forEach(saver -> {
				if (saver instanceof NoSQLSaver) {
					NoSQLSaver mongoSaver = (NoSQLSaver) saver;
					
					Bukkit.getPluginManager().callEvent(new DatabasePreUpdateEvent(mongoSaver, mongoSaver.shouldDelete() ? Action.DELETE : Action.WRITE, false));
				} else if (saver instanceof FileSaver)
					((FileSaver) saver).serialize();
			});
			
			new CopyOnWriteArraySet<>(OnimaAPI.getShutdownSavers()).forEach(saver -> {
				if (saver instanceof NoSQLSaver) {
					NoSQLSaver mongoSaver = (NoSQLSaver) saver;
					
					Bukkit.getPluginManager().callEvent(new DatabasePreUpdateEvent(mongoSaver, mongoSaver.shouldDelete() ? Action.DELETE : Action.WRITE, false));
				} else if (saver instanceof FileSaver)
					((FileSaver) saver).serialize();
			});
		}
	}

}
