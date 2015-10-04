package cl.netgamer.worldportals;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


class Portal
{
	private Location location;
	private int face;
	private String name = "";
	private Location destination = null;
	
	// constructor for events
	Portal(Location location, int face)
	{
		this.location = location;
		this.face = face;
	}
	
	// constructor for importing
	Portal(String key, List<String> fields)
	{
		this.location = locationDecode(key);
		this.face = Integer.parseInt(fields.get(0));
		this.name = fields.get(1);
		this.destination = locationDecode(fields.get(2));
	}
	
	// method for exporting, location goes apart
	Object[] serialize()
	{
		return new Object[] { Integer.valueOf(this.face), this.name, locationEncode(this.destination) };
	}
	
	// setter methods
	
	void setName(String name)
	{
		this.name = name;
	}
	
	void setDestination(Location destination)
	{
		this.destination = destination;
	}
	
	// getter methods
	
	Location getLocation()
	{
		return this.location;
	}
	
	String getEncodedLocation()
	{
		return locationEncode(this.location);
	}
	
	int getFace()
	{
		return this.face;
	}
	
	String getName()
	{
		return this.name;
	}
	
	Location getDestination()
	{
		return this.destination;
	}
	
	// check methods
	
	boolean hasName()
	{
		return !this.name.isEmpty();
	}
	
	boolean hasDestination()
	{
		return this.destination != null;
	}
	
	// bring player here, do paybacks externally
	void teleport(Player player)
	{
		// do teleport
		Location land = this.location.clone().add(0.5D, 1.0D, 0.5D);
		land.setYaw(90.0F * face);
		if (!land.getChunk().isLoaded())
			land.getChunk().load();
		player.teleport(land);
		
		// highlight the event playing some nice effects
		//land.getWorld().playEffect(land, Effect.EXTINGUISH, 0);
		player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 10, 1));
	}
	
	// location conversion methods
	
	private String locationEncode(Location l)
	{
		if (l == null)
			return "";
		
		return ""+
			Bukkit.getWorlds().indexOf(l.getWorld())+
			(l.getBlockX() < 0?"":"+")+l.getBlockX()+
			(l.getBlockY() < 0?"":"+")+l.getBlockY()+
			(l.getBlockZ() < 0?"":"+")+l.getBlockZ();
	}
	
	private Location locationDecode(String encoded)
	{
		if (encoded.isEmpty())
			return null;
		
		String[] fields = encoded.split("(?=[+-])", -1);
		return new Location(		
			Bukkit.getWorlds().get(Integer.parseInt(fields[0])), 
			Double.parseDouble(fields[1]), 
			Double.parseDouble(fields[2]), 
			Double.parseDouble(fields[3])
		);
	}
}
