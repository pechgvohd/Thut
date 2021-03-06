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
import pokecube.alternative.container.belt.BeltPlayerData;
import pokecube.alternative.container.belt.IPokemobBelt;
import pokecube.core.items.pokecubes.PokecubeManager;
import thut.lib.CompatWrapper;

public class PacketPokemobGui implements IMessage, IMessageHandler<PacketPokemobGui, IMessage>
{
    public NBTTagCompound data;

    public PacketPokemobGui()
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
            e.printStackTrace();
        }
    }

    @Override
    public IMessage onMessage(final PacketPokemobGui message, final MessageContext ctx)
    {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                processMessage(ctx.getServerHandler().playerEntity, message);
            }
        });
        return null;
    }

    void processMessage(EntityPlayerMP player, PacketPokemobGui message)
    {
        IPokemobBelt cap = BeltPlayerData.getBelt(player);
        boolean heal = message.data.getBoolean("H");
        if (heal)
        {
            for (int i = 0; i < 6; i++)
            {
                ItemStack stack = cap.getCube(i);
                PokecubeManager.heal(stack);
            }
        }
        else
        {
            int index = message.data.getInteger("S");
            ItemStack stack = cap.getCube(index);
            if (CompatWrapper.isValid(stack) && !CompatWrapper.isValid(player.inventory.getItemStack()))
            {
                cap.setCube(index, CompatWrapper.nullStack);
                player.inventory.setItemStack(stack);
                player.updateHeldItem();
            }
            else if (!CompatWrapper.isValid(stack) && CompatWrapper.isValid(player.inventory.getItemStack())
                    && PokecubeManager.isFilled(player.inventory.getItemStack()))
            {
                cap.setCube(index, player.inventory.getItemStack());
                player.inventory.setItemStack(CompatWrapper.nullStack);
                player.updateHeldItem();
            }
        }
        PacketSyncBelt packet = new PacketSyncBelt(cap, player.getEntityId());
        PacketHandler.INSTANCE.sendToAll(packet);
        return;
    }

}