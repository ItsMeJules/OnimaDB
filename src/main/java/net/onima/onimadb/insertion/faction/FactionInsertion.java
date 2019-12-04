package net.onima.onimadb.insertion.faction;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.OnimaMongo.OnimaCollection;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.utils.callbacks.VoidCallback;
import net.onima.onimafaction.faction.Faction;

public class FactionInsertion extends MongoAccessor {

	private Faction faction;
	
	{
		result = new MongoResult();
	}

	public FactionInsertion(Faction faction, VoidCallback<MongoResult> callback) {
		super(callback);
	
		this.faction = faction;
	}

	@Override
	public void execute() {
		OnimaMongo.get(OnimaCollection.FACTIONS).replaceOne(Filters.eq("uuid", faction.getUUID().toString()), faction.getDocument(), new ReplaceOptions().upsert(true));
		callback.call(result);
	}

}
