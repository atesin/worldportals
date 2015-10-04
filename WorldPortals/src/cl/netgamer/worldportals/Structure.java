package cl.netgamer.worldportals;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class Structure
{
	// check if surrounding structure matches with a portal one
	int match(Location portal, float yaw)
	{
		// start matching center column
		Block base = portal.getBlock();
		if (base.getType() != Material.OBSIDIAN)
			return -1;
		if (base.getRelative(BlockFace.UP).getType() != Material.FIRE)
			return -1;
		if (base.getRelative(BlockFace.UP, 2).getType() != Material.AIR)
			return -1;
		if (base.getRelative(BlockFace.UP, 3).getType() != Material.OBSIDIAN) {
			return -1;
		}
		
		// try to guess portal facing from player yaw, correct yaw to be positive to avoid remainder issues
		int face = Math.round((yaw + 540) / 90);
		int ref = Math.round((yaw + 585) / 90);
		
		// loop possible facings, check if side columns match with a portal
		Block l = null;
		Block r = null;
		for (int f : new int[]{face, face == ref ? face - 1 : face + 1})
		{
			// find side columns bases
			if (f % 2 == 0)
			{
				l = base.getRelative(BlockFace.EAST);
				r = base.getRelative(BlockFace.WEST);
			}
			else
			{
				l = base.getRelative(BlockFace.NORTH);
				r = base.getRelative(BlockFace.SOUTH);
			}
			
			// check if sides columns are full obsidian
			if (
				l.getRelative(BlockFace.UP).getType() == Material.OBSIDIAN &&
				r.getRelative(BlockFace.UP).getType() == Material.OBSIDIAN &&
				l.getRelative(BlockFace.UP, 2).getType() == Material.OBSIDIAN &&
				r.getRelative(BlockFace.UP, 2).getType() == Material.OBSIDIAN)
				return f % 4;
		}
		// no full obsidian side columns found
		return -1;
	}
	
	Set<Location> build(Location base, boolean enable, final int face)
	{
		Block portal = base.getBlock();
		
		if (enable)
		{
			// build enabled center column
			portal.setType(Material.SEA_LANTERN);
			portal.getRelative(BlockFace.UP).setType(Material.STATIONARY_WATER);
			portal.getRelative(BlockFace.UP, 2).setType(Material.STATIONARY_WATER);
			
			// spawn armor stand with own name
			ArmorStand labelName = (ArmorStand)base.getWorld().spawnEntity(base.clone().add(0.5D, 1.1D, 0.5D), EntityType.ARMOR_STAND);
			labelName.setSmall(true);
			labelName.setVisible(false);
			labelName.setGravity(false);
			labelName.setCustomName("\u00A7L?");
			labelName.setCustomNameVisible(true);
			
			// spawn armor stand with destination name and carry the previous stand 
			ArmorStand labelDest = (ArmorStand)base.getWorld().spawnEntity(base.clone().add(0.5D, 0.7D, 0.5D), EntityType.ARMOR_STAND);
			labelDest.setSmall(true);
			labelDest.setVisible(false);
			labelDest.setGravity(false);
			labelDest.setCustomName("?");
			labelDest.setCustomNameVisible(true);
		}
		else
		{
			// build disabled center column
			portal.setType(Material.OBSIDIAN);
			portal.getRelative(BlockFace.UP).setType(Material.AIR);
			portal.getRelative(BlockFace.UP, 2).setType(Material.AIR);
	 		
			// find and delete (hopely few) label armor stands
			Location labelName = base.clone().add(0.5D, 1.1D, 0.5D);
			Location labelDest = base.clone().add(0.5D, 0.7D, 0.5D);
			for (Entity e: labelName.getWorld().getEntitiesByClass(ArmorStand.class))
				if (e.getLocation().equals(labelName) || e.getLocation().equals(labelDest))
					e.remove();
		}
		
		return getBlocks(base, face);
	}
	
	// locations of blocks making the specified portal
	Set<Location> getBlocks(Location base, int face)
	{	
		// collect portal blocks, start adding center column locations
		Set<Location> blocks = new HashSet<Location>();
		blocks.add(base);
		blocks.add(base.clone().add(0.0D, 1.0D, 0.0D));
		blocks.add(base.clone().add(0.0D, 2.0D, 0.0D));
		blocks.add(base.clone().add(0.0D, 3.0D, 0.0D));
		
		// now add sides columns according structure facing
		Location l = null;
		Location r = null;
		if (face % 2 == 0)
		{
			l = base.clone().add(1.0D, 0.0D, 0.0D);
			r = base.clone().add(-1.0D, 0.0D, 0.0D);
		}
		else
		{
			l = base.clone().add(0.0D, 0.0D, 1.0D);
			r = base.clone().add(0.0D, 0.0D, -1.0D);
		}
		blocks.add(l.add(0.0D, 1.0D, 0.0D));
		blocks.add(l.clone().add(0.0D, 1.0D, 0.0D));
		blocks.add(r.add(0.0D, 1.0D, 0.0D));
		blocks.add(r.clone().add(0.0D, 1.0D, 0.0D));
		
		// return structure blocks locations
		return blocks;
	}
	
	// I DON'T RECOMMEND METADATA TO STORE INFO INSIDE BLOCKS BECAUSE IS NOT PERSISTENT
	
	// set portal names labels, "!" deletes, skip empty
	void setLabels(Portal portal, String name, String dest)
	{
		// guess armor stands locations
		Location labelName = portal.getLocation().clone().add(0.5D, 1.1D, 0.5D);
		Location labelDest = portal.getLocation().clone().add(0.5D, 0.7D, 0.5D);
		
		// loop (hopely few) armor stands, set its name or skip
		for (Entity e: labelName.getWorld().getEntitiesByClass(ArmorStand.class))
		{
			if (!name.isEmpty() && e.getLocation().equals(labelName))
				e.setCustomName("\u00A7L"+name);
			if (!dest.isEmpty() && e.getLocation().equals(labelDest))
				e.setCustomName(dest);
		}
	}
}
