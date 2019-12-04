package net.onima.onimadb.insertion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import net.onima.onimaapi.cooldown.utils.Cooldown;
import net.onima.onimaapi.crates.openers.VirtualKey;
import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.OnimaMongo.OnimaCollection;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.players.OfflineAPIPlayer;
import net.onima.onimaapi.players.notes.Note;
import net.onima.onimaapi.players.utils.RestoreRequest;
import net.onima.onimaapi.punishment.utils.Punishment;
import net.onima.onimaapi.saver.inventory.PlayerSaver;
import net.onima.onimaapi.utils.callbacks.VoidCallback;
import net.onima.onimaboard.players.OfflineBoardPlayer;
import net.onima.onimafaction.players.Deathban;
import net.onima.onimafaction.players.OfflineFPlayer;

public class PlayerInsertion extends MongoAccessor {
	
	private OfflineBoardPlayer offline;
	
	{
		result = new MongoResult();
	}
	
	public PlayerInsertion(OfflineBoardPlayer offline, VoidCallback<MongoResult> callback) {
		super(callback);
		
		this.offline = offline;
	}

	@Override
	public void execute() {
		OfflineAPIPlayer offlineApi = offline.getOfflineApiPlayer();
		OfflineFPlayer offlineF = offline.getOfflineFPlayer();
		
		UUID uuid = offlineApi.getUUID();
		Deathban deathban = offlineF.getDeathban();
		List<String> alts = offlineApi.getAlts().stream()
				.map(UUID::toString)
				.collect(Collectors.toCollection(() -> new ArrayList<>(offlineApi.getAlts().size())));
		
		Document playerDoc = new Document("uuid", uuid.toString())
				.append("kills", offlineApi.getKills()).append("deaths", offlineApi.getDeaths())
				.append("ip", offlineApi.getIP()).append("play_time", offlineApi.getPlayTime().getPlayTime())
				.append("vanish", offlineApi.isVanished()).append("mod_mode", offlineApi.isInModMode())
				.append("logger_dead", offlineApi.isLoggerDead()).append("frozen", offlineApi.isFrozen())
				.append("f_map", offlineF.hasfMap()).append("f_bypass", offlineF.hasFactionBypass())
				.append("role", offlineF.getRole().name()).append("chat", offlineF.getChat().name())
				.append("lives", offlineF.getLives()).append("scoreboard", offline.hasBoardToggled())
				.append("staff_board", offline.hasStaffBoard()).append("alts", alts)
				.append("faction_name", offlineF.getFaction() != null ? offlineF.getFaction().getName() : null)
				.append("ip_history", offlineApi.getIpHistory());
		
		playerDoc.append("ores", offlineApi.getMinedOres().getDocument());
		playerDoc.append("options", offlineApi.getOptions().getDocument());
		playerDoc.append("balance", offlineApi.getBalance().getDocument());
		playerDoc.append("rank", offlineApi.getRank().getDocument());
		
		List<Document> cooldownDocs = new ArrayList<>();
		List<Document> dataDocs = new ArrayList<>();
		List<Document> virtualKeyDocs = new ArrayList<>();
		List<Document> restoreRequestDocs = new ArrayList<>();
		List<Document> punishmentDocs = new ArrayList<>();
		List<Document> notesDoc = new ArrayList<>();
		
		for (Cooldown cooldown : offlineApi.getCooldowns())
			cooldownDocs.add(cooldown.getDocument(uuid));
		
		for (PlayerSaver saver : offlineApi.getPlayerDataSaved())
			dataDocs.add(saver.getDocument());
		
		for (VirtualKey key : offlineApi.getVirtualKeys().values())
			virtualKeyDocs.add(key.getDocument());
		
		for (RestoreRequest request : offlineApi.getRestoreRequests())
			restoreRequestDocs.add(request.getDocument());
		
		for (Punishment punishment : offlineApi.getPunishments())
			punishmentDocs.add(punishment.getDocument());
		
		Iterator<Note> iterator = offlineApi.getNotes().iterator();
		
		while (iterator.hasNext()) {
			Note note = iterator.next();
			
			if (note.isExpired())
				iterator.remove();
			else
				notesDoc.add(note.getDocument());
		}
		
		playerDoc.append("deathban", deathban != null ? deathban.getDocument() : null);
		playerDoc.append("cooldowns", cooldownDocs);
		playerDoc.append("data", dataDocs);
		playerDoc.append("virtual_keys", virtualKeyDocs);
		playerDoc.append("restore_request", restoreRequestDocs);
		playerDoc.append("punishments", punishmentDocs);
		playerDoc.append("notes", notesDoc);

		OnimaMongo.get(OnimaCollection.PLAYERS).replaceOne(Filters.eq("uuid", offlineApi.getUUID().toString()), playerDoc, new ReplaceOptions().upsert(true));
		callback.call(result);
	}

}
