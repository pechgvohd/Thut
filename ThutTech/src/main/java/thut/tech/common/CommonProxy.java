package thut.tech.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.server.FMLServerHandler;

public class CommonProxy  implements IGuiHandler
{

	public void initClient() {}
	
	public void loadConfiguration() {}
	
    public EntityPlayer getPlayer(String playerName)
    {
        if (playerName != null)
        {
            return getWorld().getPlayerEntityByName(playerName);
        }
        else
        {
            return null;
        }
    }
    
	public EntityPlayer getPlayer()
	{
		return null;
	}
    
    public boolean isOnClientSide()
    {
        return false;
    }

    public World getWorld()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().worldServers[0];
    }
    
    public void registerEntities()
    {

		try {
			Class<?> registry = Class.forName("powercrystals.minefactoryreloaded.MFRRegistry");
			if(registry != null)
			{
//			FarmingRegistry.registerSafariNetBlacklist(EntityLift.class);
//			FarmingRegistry.registerSafariNetBlacklist(EntityTurret.class);
//			FarmingRegistry.registerSafariNetBlacklist(EntityBeam.class);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
		//	System.out.println("[ThutConcrete] MFR not found, lift not added to the non-existant safari net blacklist.");
		//	e.printStackTrace();
		}
    }
    
	@Override
	public Object getServerGuiElement(int guiID, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}
	
	@Override
	public Object getClientGuiElement(int guiID, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}

	public void loadSounds() {}
	
	public void preinit(FMLPreInitializationEvent event)
	{
	    
	}
    
}