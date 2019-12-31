package net.onima.onimadb.query;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;

import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoQueryResult;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.utils.callbacks.VoidCallback;

public class PlayerQuery extends MongoAccessor {
	
	private UUID uuid;
	
	{
		result = new MongoQueryResult();
	}
	
	public PlayerQuery(UUID uuid, VoidCallback<MongoResult> callback) {
		super(callback);
		
		this.uuid = uuid;
	}

	@Override
	public void execute() {
		Document document = OnimaMongo.getPlayer(uuid);
		Map<String, Object> map = new HashMap<>();
		
		if (document != null) {
			map.put("player", document);
			map.put("ores", document.get("ores", Document.class));
			map.put("options", document.get("options", Document.class));
			map.put("rank", document.get("rank", Document.class));
			map.put("balance", document.get("balance", Document.class));
			map.put("player_data", document.getList("data", Document.class));
			map.put("cooldowns", document.getList("cooldowns", Document.class));
			map.put("virtual_keys", document.getList("virtual_keys", Document.class));
			map.put("restore_request", document.getList("restore_request", Document.class));
			map.put("deathban", document.get("deathban", Document.class));
			map.put("punishments", document.getList("punishments", Document.class));
			map.put("notes", document.getList("notes", Document.class));
			map.put("ignored", document.getList("ignored", String.class));
		}
		
		((MongoQueryResult) result).setMap(map);
		callback.call(result);
	}
	
}
