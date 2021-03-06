package thut.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.ForgeVersion.CheckResult;
import net.minecraftforge.common.ForgeVersion.Status;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import thut.api.maths.Vector3;
import thut.api.network.PacketHandler;
import thut.api.terrain.BiomeDatabase;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;
import thut.core.client.render.particle.ParticleFactory;
import thut.core.common.CommonProxy;
import thut.reference.Reference;

public class ClientProxy extends CommonProxy
{
    public static class UpdateNotifier
    {
        public UpdateNotifier()
        {
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onPlayerJoin(TickEvent.PlayerTickEvent event)
        {
            if (event.player.getEntityWorld().isRemote && event.player == FMLClientHandler.instance().getClientPlayerEntity())
            {
                MinecraftForge.EVENT_BUS.unregister(this);
                Object o = Loader.instance().getIndexedModList().get(Reference.MOD_ID);
                CheckResult result = ForgeVersion.getResult(((ModContainer) o));
                if (result.status == Status.OUTDATED)
                {
                    ITextComponent mess = getOutdatedMessage(result, "Thut Core");
                    (event.player).addChatMessage(mess);
                }

            }
        }
    }

    public static int       renderPass;

    public static Minecraft mc;

    public static ITextComponent getOutdatedMessage(CheckResult result, String name)
    {
        String linkName = "[" + TextFormatting.GREEN + name + " " + result.target + TextFormatting.WHITE;
        String link = "" + result.url;
        String linkComponent = "{\"text\":\"" + linkName + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\""
                + link + "\"}}";

        String info = "\"" + TextFormatting.RED + "New " + name
                + " version available, please update before reporting bugs.\nClick the green link for the page to download.\n"
                + "\"";
        String mess = "[" + info + "," + linkComponent + ",\"]\"]";
        return ITextComponent.Serializer.jsonToComponent(mess);
    }

    public ClientProxy()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public EntityPlayer getPlayer()
    {
        return getPlayer(null);
    }

    @Override
    public EntityPlayer getPlayer(String playerName)
    {
        if (isOnClientSide())
        {
            if (playerName != null) { return getWorld().getPlayerEntityByName(playerName); }
            return Minecraft.getMinecraft().thePlayer;
        }
        return super.getPlayer(playerName);
    }

    @Override
    public World getWorld()
    {
        if (isOnClientSide()) { return Minecraft.getMinecraft().theWorld; }
        return super.getWorld();
    }

    @Override
    public void initClient()
    {
        mc = FMLClientHandler.instance().getClient();
        PacketHandler.provider = this;
        new UpdateNotifier();
        ParticleFactory.initVanillaParticles();
    }

    @Override
    public boolean isOnClientSide()
    {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    @Override
    public void loadSounds()
    {
        try
        {

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void preinit(FMLPreInitializationEvent e)
    {
        // ModelLoaderRegistry.registerLoader(ModelFluid.FluidLoader.instance);
    }

    @SubscribeEvent
    public void textOverlay(RenderGameOverlayEvent.Text event)
    {
        boolean debug = Minecraft.getMinecraft().gameSettings.showDebugInfo;
        if (!debug) return;
        TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(Minecraft.getMinecraft().thePlayer);
        Vector3 v = Vector3.getNewVector().set(Minecraft.getMinecraft().thePlayer);
        String msg = "Sub-Biome: " + BiomeDatabase.getReadableNameFromType(t.getBiome(v));
        // Until forge stops sending the same event, with the same list 8 times,
        // this is needed
        for (String s : event.getLeft())
        {
            if (s != null && s.equals(msg)) return;
        }
        event.getLeft().add("");
        event.getLeft().add(msg);
    }
}
