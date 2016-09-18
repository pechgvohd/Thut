package pokecube.alternative.network;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import pokecube.alternative.capabilities.IBeltCapability;
import pokecube.alternative.event.EventHandlerCommon;
import pokecube.core.items.pokecubes.PokecubeManager;

public class PacketClickPokemobSlot implements IMessage, IMessageHandler<PacketClickPokemobSlot, IMessage>
{
    public NBTTagCompound data;

    public PacketClickPokemobSlot()
    {
        data = new NBTTagCompound();
    }

    @Override
    public void toBytes(ByteBuf buffer)
    {
        PacketBuffer buf = new PacketBuffer(buffer);
        buf.writeNBTTagCompoundToBuffer(data);
    }

    @Override
    public void fromBytes(ByteBuf buffer)
    {
        PacketBuffer buf = new PacketBuffer(buffer);
        try
        {
            data = buf.readNBTTagCompoundFromBuffer();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public IMessage onMessage(final PacketClickPokemobSlot message, final MessageContext ctx)
    {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(new Runnable()
        {
            public void run()
            {
                processMessage(ctx.getServerHandler().playerEntity, message);
            }
        });
        return null;
    }

    void processMessage(EntityPlayerMP player, PacketClickPokemobSlot message)
    {
        IBeltCapability cap = player.getCapability(EventHandlerCommon.BELTAI_CAP, null);
        int index = message.data.getInteger("S");
        ItemStack stack = cap.getCube(index);
        System.out.println(message.data + " " + stack);
        if (stack != null && player.inventory.getItemStack() == null)
        {
            cap.setCube(index, null);
            player.inventory.setItemStack(stack);
            player.updateHeldItem();
        }
        else if (stack == null && player.inventory.getItemStack() != null
                && PokecubeManager.isFilled(player.inventory.getItemStack()))
        {
            cap.setCube(index, player.inventory.getItemStack());
            player.inventory.setItemStack(null);
            player.updateHeldItem();
        }
        PacketSyncBelt packet = new PacketSyncBelt(cap, player.getEntityId());
        PacketHandler.INSTANCE.sendToAll(packet);
        return;
    }

}