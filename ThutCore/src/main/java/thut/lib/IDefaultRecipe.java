package thut.lib;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public interface IDefaultRecipe extends IRecipe
{
    @Override
    default ItemStack[] getRemainingItems(InventoryCrafting inv)
    {
        ItemStack[] aitemstack = new ItemStack[inv.getSizeInventory()];
        for (int i = 0; i < aitemstack.length; ++i)
        {
            ItemStack itemstack = inv.getStackInSlot(i);
            aitemstack[i] = toKeep(i, itemstack, inv);
            if (!CompatWrapper.isValid(aitemstack[i])) aitemstack[i] = CompatWrapper.nullStack;
        }
        return aitemstack;
    }

    default ItemStack toKeep(int slot, ItemStack stackIn, InventoryCrafting inv)
    {
        return net.minecraftforge.common.ForgeHooks.getContainerItem(stackIn);
    }
}
