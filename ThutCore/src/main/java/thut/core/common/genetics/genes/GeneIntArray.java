package thut.core.common.genetics.genes;

import java.util.Arrays;

import net.minecraft.nbt.NBTTagCompound;
import thut.api.entity.genetics.Gene;

public abstract class GeneIntArray implements Gene
{
    protected int[] value = new int[0];

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue()
    {
        return (T) value;
    }

    @Override
    public <T> void setValue(T value)
    {
        this.value = (int[]) value;
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("V", value);
        return tag;
    }

    @Override
    public void load(NBTTagCompound tag)
    {
        value = tag.getIntArray("V");
    }

    @Override
    public String toString()
    {
        return "" + Arrays.toString(value);
    }

}
