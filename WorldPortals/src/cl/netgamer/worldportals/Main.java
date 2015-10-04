package cl.netgamer.worldportals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class Main
	extends JavaPlugin
{
	Structure strc;
	
	private int healthCost;
	private List<Integer> allowedWorlds;
	private boolean interWorlds;
	private boolean canCchangeDestination;
	private Data data;
	private Map<Location, Portal> portals = new HashMap<Location, Portal>();
	private Map<Location, Set<Location>> blocks = new HashMap<Location, Set<Location>>();
	//private Map<String, Location> entities = new HashMap<String, Location>();
	//private Map<String, BukkitTask> entityTasks = new HashMap<String, BukkitTask>();
	private Map<String, Object> lang;
	private Map<String, BukkitTask> teleports = new HashMap<String, BukkitTask>();
	
	private String headerFix;
	
	private String headerVar;
	private String title;

	// priority: 0, 1, 2 = err, warn, info
	String[] pr = new String[]{"\u00A7D", "\u00A7E", "\u00A7B"};

	
	
	public void onEnable()
	{
		strc = new Structure();
		new Events(this);
		getCommand("listportals").setExecutor(new CommandList(this));
		getCommand("portal").setExecutor(new CommandPortal(this));
		
		saveDefaultConfig();
		healthCost = getConfig().getInt("healthCost");
		allowedWorlds = getConfig().getIntegerList("allowedWorlds");
		interWorlds = getConfig().getBoolean("interWorlds");
		canCchangeDestination = getConfig().getBoolean("canCchangeDestination");
		
		// load configuration sections
		lang = getConfig().getConfigurationSection("lang").getValues(false);
		TextTable header = new TextTable((String)lang.get("header"), pr[2], 16, 21);
		headerFix = header.getPage(0, true);
		headerVar = header.getPage(0, false);
		title = pr[1]+(String)lang.get("title");
		
		// load portals data
		data = new Data(this);
		portals = data.loadPortals();
		for (Location base : portals.keySet())
			addBlocks(base, strc.getBlocks(base, portals.get(base).getFace()));
	}
	
	
	//******* HERE COMES THE INTERESTING PART **********/
	
	
	// BLOCKS
	
	// specified blocks belong a portal from now
	private void addBlocks(Location base, Set<Location> updatedBlocks)
	{
		for (Location block : updatedBlocks)
		{
			if (!blocks.containsKey(block))
				blocks.put(block, new HashSet<Location>());
			blocks.get(block).add(base);
		}
	}
	
	// specified blocks don't belong a portal anymore
	private void delBlocks(Location base, Set<Location> updatedBlocks)
	{
		for (Location block : updatedBlocks)
		{
			blocks.get(block).remove(base);
			if (blocks.get(block).size() == 0)
				blocks.remove(block);
		}
	}
	
	// STRUCTURES
	
	// create a portal if structure is valid
	void structureCreate(Player player, Block block)
	{
		// get portal facing if valid
		Location base = block.getLocation().add(0.0D, -1.0D, 0.0D);
		int face = strc.match(base, player.getLocation().getYaw());
		if (face < 0)
			return;
		
		// allow portals in this world?
		if (!allowedWorlds.contains(Integer.valueOf(getServer().getWorlds().indexOf(base.getWorld()))))
		{
			base.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 0);
			base.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
			return;
		}
		
		// create and save portal
		Portal portal = new Portal(base, face);
		data.savePortal(portal);
		portals.put(base, portal);
		
		// take note of portal blocks
		addBlocks(base, strc.build(base, true, face));
	}
	
	// destroy a portal if were valid
	void structureDestroy(Block block)
	{
		// ignore non portal blocks
		if (!isPortalBlock(block))
			return;
		
		// destroy involved portals, clone set to avoid race condition while deleting
		Portal portal = null;
		for (Location base: new HashSet<Location>(blocks.get(block.getLocation())))
		{
			// delete blocks and data before delete portal, then update references
			portal = portals.get(base);
			delBlocks(base, strc.build(base, false, portal.getFace()));
			data.deletePortal(portal);
			portals.remove(base);
			updateReferences(base, "?");
		}
	}
	
	// NAMES
	
	// process portal name/dest command
	boolean setPortalNames(Player player, String[] names)
	{
		// get target portal
		Portal portal = getTargetPortal(player);
		if (portal == null)
			return false;
		
		// prevent name overwtiting
		if (portal.hasName() && names.length == 2)
			return msg(player, 0, "argsNotMatch");
		
		// defaults
		String name = "";
		String dest = "";
		
		// get name, dest or both
		if (names.length == 2)
		{
			name = names[0];
			dest = names[1];
		}
		
		// just one, assuming name if portal does not have one yet
		else if (portal.hasName())
			dest = names[0];
		else
			name = names[0];
		
		// prevent self linked portal
		if (dest.equalsIgnoreCase(name)) {
			return msg(player, 0, "selfLink");
		}
		
		// validate name
		if (!name.isEmpty())
		{
			// spelling
			if (!name.matches("^[a-zA-Z0-9-]{1,15}$"))
				return msg(player, 0, "invalidName");
			
			// dismiss number-looking names
			try
			{
				Double.parseDouble(name);
				return msg(player, 0, "lookLikeNumber");
			}
			catch (NumberFormatException e){}
			
			// ensure unique name
			for (Portal p : portals.values())
				if (p.getName().equalsIgnoreCase(name))
					return msg(player, 0, "busyName");
		}
		
		// validate destination
		Portal destination = null;
		if (!dest.isEmpty())
		{
			// prevent overwrite destination if forbidden
			if (!canCchangeDestination && portals.containsKey(portal.getDestination())) {
				return msg(player, 0, "alreadySet");
			}
			
			// prevent self linked portal
			if (dest.equalsIgnoreCase(portal.getName())) {
				return msg(player, 0, "selfLink");
			}
			
			// find destination by name
			for (Portal p : portals.values())
				if (dest.equalsIgnoreCase(p.getName()))
				{
					destination = p;
					break;
				}
			
			// destination portal not found
			if (destination == null)
				return msg(player, 0, "noSuchDestination");
			
			// prevent link to another world if forbidden
			if (!interWorlds && !portal.getLocation().getWorld().equals(destination.getLocation().getWorld()))
				return msg(player, 0, "differentWorlds");
		}
		
		// set name if apply
		if (!name.isEmpty())
		{
			portal.setName(name);
			updateReferences(portal.getLocation(), name);
		}
		
		// set destination if apply
		if (dest.isEmpty())
			msg(player, 1, "noDestination");
		else
			portal.setDestination(destination.getLocation());
		
		// set portal
		data.savePortal(portal);
		strc.setLabels(portal, name, dest);
		return msg(player, 2, "portalSet");
	}
	
	// update destination label on portals that point to this
	private void updateReferences(Location base, String name)
	{
		// iterate portals, match destination, change destination label
		for (Portal p : portals.values())
			if (p.hasDestination() && p.getDestination().equals(base))
				strc.setLabels(p, "", name);
	}
	
	// PLAYER MOVEMENT
	
	// check if player exits/enters a portal
	void portalWalk(Player player, Location from, Location to)
	{
		// process portal exit
		from = new Location(from.getWorld(), from.getBlockX(), from.getBlockY()-1, from.getBlockZ());
		if (portals.containsKey(from))
			portalLeave(player, portals.get(from));
		
		// process portal enter
		to = new Location(to.getWorld(), to.getBlockX(), to.getBlockY()-1, to.getBlockZ());
		if (portals.containsKey(to))
			portalEnter(player, portals.get(to));
	}
	
	// possible teleport schedule
	private void portalEnter(final Player player, Portal portal)
	{
		// check if portal has a destination, if exists, and has a name
		if (!portal.hasDestination())
			return;
		if (!portals.containsKey(portal.getDestination()))
			return;
		
		Portal destination = portals.get(portal.getDestination());
		if (!destination.hasName())
			return;
		
		final Location dest = destination.getLocation();
		
		// wooble screen and schedule a teleport 4 seconds later
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 1));
		BukkitTask task = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				//  clean this task from the list to prevent memory leaks
				teleports.remove(player.getName());
				
				// remember is asynchronous and anything could had changed!
				if (!player.isOnline() || player.isDead())
					return;
				
				// check destination again
				if (portals.containsKey(dest))
				{
					Portal objective = portals.get(dest);
					
					// do paybacks if enough health
					if (player.getHealth() <= healthCost)
					{
						player.getLocation().getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 0);
						player.removePotionEffect(PotionEffectType.CONFUSION);
						return;
					}
					player.damage(healthCost);
					
					// teleport and remove sickness effect
					objective.teleport(player);
				}
				player.removePotionEffect(PotionEffectType.CONFUSION);
			}
		}.runTaskLater(this, 80);
		
		// store the new task to get accessible
		teleports.put(player.getName(), task);
	}
	
	// possible teleport cancel
	private void portalLeave(Player player, Portal portal)
	{
		// delete scheduled task and cancel effects
		String name = player.getName();
		if (teleports.containsKey(name))
		{
			teleports.get(name).cancel();
			teleports.remove(name);
			player.removePotionEffect(PotionEffectType.CONFUSION);
		}
	}
	
	// COMMANDS
	
	// list portals
	boolean list(CommandSender sender, String pattern, int page)
	{
		pattern = pattern.toLowerCase();
		String table = "";
		
		// build table with portals that match pattern
		for (Portal p : portals.values())
		{
			// pattern match
			if (!p.getName().toLowerCase().contains(pattern))
				continue;
			
			// add lines to table
			//table += table.isEmpty() ? "" : "\n";
			table += "\n"+(p.hasName() ? p.getName() : pr[0]+" ?")+"\t"+p.getEncodedLocation()+"\t";
			if (portals.containsKey(p.getDestination()) && portals.get(p.getDestination()).hasName())
				table += portals.get(p.getDestination()).getName();
			else
				table += pr[0]+" ?";
		}
		TextTable tab = new TextTable(table, pr[2], new int[]{16, 21});
		int pages = tab.setPageHeight(8);
		
		if (sender instanceof Player)
			sender.sendMessage(String.format(title, pattern, page, pages)+"\n"+headerVar+tab.getPage(page, false));
		else
			sender.sendMessage(String.format(title, pattern, page, pages)+"\n"+headerFix+tab.getPage(page, true));
		return true;
	}
	
	// display help
	boolean help(CommandSender sender, String node)
	{
		String text = pr[1]+(String) getConfig().getString("help."+node);
		text = text.replaceAll("[\r\n]+(?!-)", "\n"+pr[1]);
		text = text.replaceAll("[\r\n]+-", "\n"+pr[2]+"-");
		sender.sendMessage(text);
		return true;
	}

	// UTILITY

	// check if block belongs a portal
	boolean isPortalBlock(Block block)
	{
		return blocks.containsKey(block.getLocation());
	}
	
	// get portal selected with crosshair
	private Portal getTargetPortal(Player player)
	{
		Block target = player.getTargetBlock((Set<Material>)null, 5);
		Portal portal = null;
		
		if (target.getType() == org.bukkit.Material.AIR)
			msg(player, 0, "tooFar");
		else if (target.getType() != org.bukkit.Material.STATIONARY_WATER)
			msg(player, 0, "notAPortal");
		else if (!blocks.containsKey(target.getLocation()))
			msg(player, 0, "notAPortal");
		else
			for (Location in : blocks.get(target.getLocation()))
				portal = (Portal)portals.get(in);
		return portal;
	}
	
	// send custom message
	boolean msg(CommandSender sender, int priority, String msg)
	{
		if (lang.containsKey(msg))
			sender.sendMessage(pr[priority]+(String)lang.get(msg));
		else
			sender.sendMessage(pr[priority]+msg);
		return priority != 0;		
	}
	
	// rebuild all portals
	boolean rebuildPortals(CommandSender sender)
	{
		// destroy all portals first
		msg(sender, 1, "Destroying portals...");
		for (Portal p : portals.values())
			strc.build(p.getLocation(), false, p.getFace());
		// rebuild portals
		msg(sender, 1, "Rebuilding portals...");
		for (Portal p : portals.values())
			strc.build(p.getLocation(), true, p.getFace());
		// set labels
		msg(sender, 1, "Setting labels...");
		for (Portal p : portals.values())
			strc.setLabels(p, p.getName(), portals.containsKey(p.getDestination())?portals.get(p.getDestination()).getName():"");
		return msg(sender, 2, "Done.");
	}
}

// change projects path
// http://www.joeflash.ca/blog/2008/11/moving-a-fb-workspace-update.html
