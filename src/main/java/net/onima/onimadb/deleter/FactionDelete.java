package net.onima.onimadb.deleter;

import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;

import net.onima.onimaapi.mongo.OnimaMongo;
import net.onima.onimaapi.mongo.OnimaMongo.OnimaCollection;
import net.onima.onimaapi.mongo.api.MongoAccessor;
import net.onima.onimaapi.mongo.api.result.MongoDeleteResult;
import net.onima.onimaapi.mongo.api.result.MongoResult;
import net.onima.onimaapi.utils.callbacks.VoidCallback;
import net.onima.onimafaction.faction.Faction;

public class FactionDelete extends MongoAccessor {
	
	private Faction faction;

	{
		result = new MongoDeleteResult();
	}

	public FactionDelete(Faction faction, VoidCallback<MongoResult> callback) {
		super(callback);
		
		this.faction = faction;
	}

	@Override
	public void execute() throws MongoException {
		((MongoDeleteResult) result).setDocumentsDeleted(OnimaMongo.get(OnimaCollection.FACTIONS).deleteOne(Filters.eq("uuid", faction.getUUID().toString())).getDeletedCount());
		callback.call(result);
	}

}
