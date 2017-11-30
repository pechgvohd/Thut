package thut.api.terrain;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

public class CapabilityTerrain
{
    @CapabilityInject(ITerrainProvider.class)
    public static final Capability<ITerrainProvider> TERRAIN_CAP = null;

    public static interface ITerrainProvider
    {
        TerrainSegment getTerrainSegement(BlockPos blockLocation);

        void setTerrainSegment(TerrainSegment segment, int chunkY);

        TerrainSegment getTerrainSegment(int chunkY);

        BlockPos getChunkPos();
    }

    public static class DefaultProvider
            implements ITerrainProvider, ICapabilityProvider, INBTSerializable<NBTTagCompound>
    {
        private BlockPos         pos;
        private final Chunk      chunk;
        private TerrainSegment[] segments = new TerrainSegment[16];

        public DefaultProvider(Chunk chunk)
        {
            this.chunk = chunk;
        }

        @Override
        public TerrainSegment getTerrainSegement(BlockPos blockLocation)
        {
            int chunkY = blockLocation.getY() / 16;
            if (chunkY >= segments.length) chunkY = segments.length - 1;
            if (chunkY < 0) chunkY = 0;
            return getTerrainSegment(chunkY);
        }

        @Override
        public void setTerrainSegment(TerrainSegment segment, int chunkY)
        {
            if (chunkY >= segments.length) chunkY = segments.length - 1;
            if (chunkY < 0) chunkY = 0;
            segments[chunkY] = segment;
        }

        @Override
        public TerrainSegment getTerrainSegment(int chunkY)
        {
            if (chunkY >= segments.length) chunkY = segments.length - 1;
            if (chunkY < 0) chunkY = 0;
            TerrainSegment ret = segments[chunkY];
            if (ret == null)
            {
                ret = segments[chunkY] = new TerrainSegment(getChunkPos().getX(), chunkY, getChunkPos().getZ());
            }
            return ret;
        }

        @Override
        public BlockPos getChunkPos()
        {
            if (pos == null)
            {
                pos = new BlockPos(chunk.x, 0, chunk.z);
            }
            return pos;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing)
        {
            return capability == CapabilityTerrain.TERRAIN_CAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing)
        {
            if (hasCapability(TERRAIN_CAP, facing)) return (T) this;
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT()
        {
            NBTTagCompound nbt = new NBTTagCompound();
            for (int i = 0; i < 16; i++)
            {
                TerrainSegment t = this.getTerrainSegment(i);
                if (t == null) continue;
                t.checkToSave();
                if (!t.toSave)
                {
                    continue;
                }
                NBTTagCompound terrainTag = new NBTTagCompound();
                t.saveToNBT(terrainTag);
                nbt.setTag("" + i, terrainTag);
            }
            NBTTagList biomeList = new NBTTagList();
            for (BiomeType t : BiomeType.values())
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("name", t.name);
                tag.setInteger("id", t.getType());
                biomeList.appendTag(tag);
            }
            nbt.setTag("ids", biomeList);
            return nbt;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt)
        {
            BlockPos pos = this.getChunkPos();
            int x = pos.getX();
            int z = pos.getZ();
            Map<Integer, Integer> idReplacements = Maps.newHashMap();
            NBTTagList tags = (NBTTagList) nbt.getTag("ids");
            for (int i = 0; i < tags.tagCount(); i++)
            {
                NBTTagCompound tag = tags.getCompoundTagAt(i);
                String name = tag.getString("name");
                int id = tag.getInteger("id");
                BiomeType type = BiomeType.getBiome(name, false);
                if (type.getType() != id)
                {
                    idReplacements.put(id, type.getType());
                }
            }
            boolean hasReplacements = !idReplacements.isEmpty();
            for (int i = 0; i < 16; i++)
            {
                NBTTagCompound terrainTag = null;
                try
                {
                    terrainTag = nbt.getCompoundTag(i + "");
                }
                catch (Exception e)
                {

                }
                TerrainSegment t = null;
                if (terrainTag != null && !terrainTag.hasNoTags() && !TerrainSegment.noLoad)
                {
                    t = new TerrainSegment(x, i, z);
                    if (hasReplacements) t.idReplacements = idReplacements;
                    TerrainSegment.readFromNBT(t, terrainTag);
                    this.setTerrainSegment(t, i);
                    t.idReplacements = null;
                }
                if (t == null)
                {
                    t = new TerrainSegment(x, i, z);
                    this.setTerrainSegment(t, i);
                }
            }
        }

    }

    public static class Storage implements Capability.IStorage<ITerrainProvider>
    {

        @Override
        public NBTBase writeNBT(Capability<ITerrainProvider> capability, ITerrainProvider instance, EnumFacing side)
        {
            if (instance instanceof DefaultProvider) return ((DefaultProvider) instance).serializeNBT();
            return null;
        }

        @Override
        public void readNBT(Capability<ITerrainProvider> capability, ITerrainProvider instance, EnumFacing side,
                NBTBase base)
        {
            if (instance instanceof DefaultProvider && base instanceof NBTTagCompound)
                ((DefaultProvider) instance).deserializeNBT((NBTTagCompound) base);
        }
    }
}