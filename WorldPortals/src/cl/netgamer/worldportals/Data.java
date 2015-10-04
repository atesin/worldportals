package cl.netgamer.worldportals;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

// bridge between plugin and data file
public class Data
{
	private ConfigAccessor file;
	private FileConfiguration conf;
	
	public Data(Main main)
	{
		file = new ConfigAccessor(main, "data.yml");
		file.saveDefaultConfig();
		conf = file.getConfig();
	}
	
	void savePortal(Portal portal)
	{
		conf.set(portal.getEncodedLocation(), portal.serialize());
		file.saveConfig();
	}
	
	void deletePortal(Portal portal)
	{
		conf.set(portal.getEncodedLocation(), null);
		file.saveConfig();
	}
	
	Map<Location, Portal> loadPortals()
	{
		Map<Location, Portal> portals = new HashMap<Location, Portal>();
		for (String key : conf.getKeys(false))
		{
			Portal portal = new Portal(key, conf.getStringList(key));
			portals.put(portal.getLocation(), portal);
		}
		return portals;
	}
}
