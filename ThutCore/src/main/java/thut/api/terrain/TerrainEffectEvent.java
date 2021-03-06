package thut.api.terrain;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class TerrainEffectEvent extends EntityEvent
{
    public final String  identifier;
    public final boolean entry;

    public TerrainEffectEvent(EntityLivingBase entity, String identifier, boolean entry)
    {
        super(entity);
        this.identifier = identifier;
        this.entry = entry;
    }

}
