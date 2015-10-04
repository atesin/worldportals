package cl.netgamer.worldportals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class CommandPortal implements CommandExecutor
{
	// PROPERTIES AND CONSTRUCTORS
	
	private Main main;
	
	CommandPortal(Main main)
	{
		this.main = main;
	}
	
	// PROCESS COMMAND

	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// other command
		if (!cmd.getName().equalsIgnoreCase("portal"))
			return true;
		
		// help
		if (args.length == 0)
			return main.help(sender, "portal");
		
		// just for players, except for REBUILD command
		if (!(sender instanceof Player))
		{	
			if (args[0].equals("REBUILD"))
			{
				// must be "REBUILD CONFIRM"
				if (args.length == 1 || !args[1].equals("CONFIRM"))
					return !main.msg(sender, 0, "sure rebuild all portals? confirm with 'REBUILD CONFIRM'");
				return main.rebuildPortals(sender);
			}
			// not a rebuild command, exit
			return !main.msg(sender, 0, "notAPlayer");
		}
		
		// too many arguments
		if (args.length > 2)
			return !main.msg(sender, 0, "argsNotMatch");
		
		// name portal
		return main.setPortalNames((Player) sender, args);
	}
}
