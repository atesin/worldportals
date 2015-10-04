package cl.netgamer.worldportals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandList implements CommandExecutor
{
	// PROPERTIES AND CONSTRUCTORS
	
	private Main main;
	

	public CommandList(Main main)
	{
		this.main = main;
	}
	
	// PROCESS COMMAND

	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// other command
		if (!cmd.getName().equalsIgnoreCase("listportals"))
			return true;
		
		// help
		if (args.length > 0 && args[0].equals("?"))
		{
			if (args.length == 1)
				return main.help(sender, "list");
			else
				return !main.msg(sender, 0, "argsNotMatch");
		}
		
		// too many arguments
		if (args.length > 2)
			return !main.msg(sender, 0, "argsNotMatch");
		
		// defaults
		String pattern = "";
		int page = 0;
		if (sender instanceof Player)
			page = 1;
		
		// with 1 parameter
		if(args.length == 1)
		{
			// guess page or or unchecked pattern?
			try
			{
				page = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException e)
			{
				pattern = args[0];
			}
		}
		
		// with 2 parameters
		if(args.length == 2)
		{
			// get unchecked pattern
			pattern = args[0];
			
			// try to get page
			try
			{
				page = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException e)
			{
				return !main.msg(sender, 0, "notANumber");
			}
		}
		
		/* // validate pattern
		if (!pattern.matches("^[a-zA-Z0-9-]{0,15}$"))
			return !main.msg(sender, 0, "invalidName"); */
		
		// list portals
		return main.list(sender, pattern, page);
	}
}
