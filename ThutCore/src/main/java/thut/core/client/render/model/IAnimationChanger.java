package thut.core.client.render.model;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;

public interface IAnimationChanger
{
    int getColourForPart(String partIdentifier, Entity entity, int default_);

    boolean isPartHidden(String part, Entity entity, boolean default_);

    String modifyAnimation(EntityLiving entity, float partialTicks, String phase);
}
