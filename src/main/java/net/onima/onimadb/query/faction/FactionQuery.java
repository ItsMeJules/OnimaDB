package net.onima.onimadb.query.faction;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoQueryResult;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.utils.callbacks.VoidCallback;

public class FactionQuery extends MongoAccessor {
	
	private Document document;
	
	{
		result = new MongoQueryResult();
	}
	
	public FactionQuery(Document document, VoidCallback<MongoResult> callback) {
		super(callback);
		
		this.document = document;
	}

	@Override
	public void execute() {
		Map<String, Object> map = new HashMap<>();
		
		if (document != null) {
			map.put("name", document.getString("name"));
			map.put("faction", document);
		}
		
		((MongoQueryResult) result).setMap(map);
		callback.call(result);
	}
	
}
