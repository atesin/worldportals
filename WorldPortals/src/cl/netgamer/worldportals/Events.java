package cl.netgamer.worldportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;

public final class Events implements org.bukkit.event.Listener
{
	private Main main;
	//private boolean teleportEntities;
	
	public Events(Main main)
	{
		this.main = main;
		main.getServer().getPluginManager().registerEvents(this, main);
		//teleportEntities = main.getConfig().getBoolean("teleportEntities");
	}
	
	// possible portal activation
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e)
	{
		// fire over obsidian?
		if (e.getBlock().getType() == Material.FIRE && e.getBlock().getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN)
		{
			// for some strange reason the event is called twice
			if (e.getBlock().hasMetadata("placed"))
				e.getBlock().removeMetadata("placed", main);
			else
			{
				e.getBlock().setMetadata("placed", new FixedMetadataValue(main, true));
				main.structureCreate(e.getPlayer(), e.getBlock());
			}
		}
	}
	
	// possible portal destroying by hand
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		if (e.getBlock().getType() == Material.SEA_LANTERN || e.getBlock().getType() == Material.OBSIDIAN || e.getBlock().getType() == Material.STATIONARY_WATER)
			main.structureDestroy(e.getBlock());
	}
	
	// possible portal destroying by explosion
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event)
	{
		for (Block block : event.blockList())
			if (block.getType() == Material.SEA_LANTERN || block.getType() == Material.OBSIDIAN || block.getType() == Material.STATIONARY_WATER)
				main.structureDestroy(block);
	}
	
	// check if player stepped to another block for possible portal entering/exiting
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e)
	{
		// check if player stepped from/to another block
		Location f = e.getFrom();
		Location t = e.getTo();
		if (f.getBlockX() != t.getBlockX() || f.getBlockY() != t.getBlockY() || f.getBlockZ() != t.getBlockZ())
			main.portalWalk(e.getPlayer(), f, t);
	}
	
	// self contained cascade
	@EventHandler
	public void onBlockFromTo(BlockFromToEvent e)
	{
		if (main.isPortalBlock(e.getBlock()))
			e.setCancelled(true);
	}
	
	// prevent water cascade grabbing with bucket
	@EventHandler
	public void onPlayerBucketFill(PlayerBucketFillEvent e)
	{
		if (main.isPortalBlock(e.getBlockClicked()))
			e.setCancelled(true);
	}
	
	// prevent placing armor on invisible armor stands
	@EventHandler
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e)
	{
		if (!e.getRightClicked().isVisible())
			e.setCancelled(true);
	}
}
