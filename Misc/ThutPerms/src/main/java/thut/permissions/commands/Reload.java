package thut.permissions.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.permissions.ThutPerms;

public class Reload extends CommandBase
{

    public Reload()
    {
    }

    @Override
    public String getCommandName()
    {
        return "reloadPerms";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/reloadPerms";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        ThutPerms.loadPerms();
        sender.addChatMessage(new TextComponentString("Reloaded Permissions from File"));
    }

}
