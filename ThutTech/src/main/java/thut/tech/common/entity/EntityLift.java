package thut.tech.common.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thut.api.ThutBlocks;
import thut.api.entity.IMultibox;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;
import thut.tech.common.blocks.lift.TileEntityLiftAccess;
import thut.tech.common.handlers.ConfigHandler;
import thut.tech.common.items.ItemLinker;

public class EntityLift extends EntityLivingBase implements IEntityAdditionalSpawnData, IMultibox
{
    static final DataParameter<Integer>             DESTINATIONFLOORDW = EntityDataManager
            .<Integer> createKey(EntityLift.class, DataSerializers.VARINT);
    static final DataParameter<Integer>             DESTINATIONYDW     = EntityDataManager
            .<Integer> createKey(EntityLift.class, DataSerializers.VARINT);
    static final DataParameter<Integer>             CURRENTFLOORDW     = EntityDataManager
            .<Integer> createKey(EntityLift.class, DataSerializers.VARINT);
    static final DataParameter<Optional<ItemStack>> CAMOBLOCKDW        = EntityDataManager
            .<Optional<ItemStack>> createKey(EntityLift.class, DataSerializers.OPTIONAL_ITEM_STACK);

    public static int                               ACCELERATIONTICKS  = 20;

    private static HashMap<UUID, EntityLift>        lifts              = new HashMap<UUID, EntityLift>();
    private static HashMap<UUID, EntityLift>        lifts2             = new HashMap<UUID, EntityLift>();

    public static void clear()
    {
        lifts2.clear();
        lifts.clear();
    }

    public static EntityLift getLiftFromUUID(UUID uuid, boolean client)
    {
        if (client) return lifts2.get(uuid);
        return lifts.get(uuid);
    }

    public static void removeBlocks(World worldObj, TileEntityLiftAccess te, BlockPos pos)
    {
        int xMin = te.boundMin.intX();
        int zMin = te.boundMin.intZ();
        int xMax = te.boundMax.intX();
        int zMax = te.boundMax.intZ();
        int yMin = te.boundMin.intY();
        int yMax = te.boundMax.intY();
        Vector3 loc = Vector3.getNewVector();
        for (int i = xMin; i <= xMax; i++)
            for (int j = zMin; j <= zMax; j++)
                for (int k = yMin; k <= yMax; k++)
                {
                    worldObj.setBlockToAir(loc.set(pos).add(i, k, j).getPos());
                }
    }

    public static ItemStack[][][] checkBlocks(World worldObj, TileEntityLiftAccess te, BlockPos pos)
    {
        int xMin = te.boundMin.intX();
        int zMin = te.boundMin.intZ();
        int xMax = te.boundMax.intX();
        int zMax = te.boundMax.intZ();
        int yMin = te.boundMin.intY();
        int yMax = te.boundMax.intY();

        ItemStack[][][] ret = new ItemStack[(xMax - xMin) + 1][(yMax - yMin) + 1][(zMax - zMin) + 1];

        Vector3 loc = Vector3.getNewVector().set(pos);
        for (int i = xMin; i <= xMax; i++)
            for (int k = yMin; k <= yMax; k++)
                for (int j = zMin; j <= zMax; j++)
                {
                    if (!(i == 0 && j == 0 && k == 0))
                    {
                        IBlockState state = loc.set(pos).addTo(i, k, j).getBlockState(worldObj);
                        Block b;
                        if (!((b = state.getBlock()) instanceof ITileEntityProvider))
                        {
                            ret[i - xMin][k - yMin][j - zMin] = new ItemStack(b, 1, b.getMetaFromState(state));
                        }
                        else if (k == 0 || !state.getBlock().isAir(state, worldObj, pos)) { return null; }
                    }
                    else
                    {
                        ret[i - xMin][k - yMin][j - zMin] = new ItemStack(ThutBlocks.lift);
                    }
                }
        return ret;
    }

    public Vector3                  boundMin      = Vector3.getNewVector();
    public Vector3                  boundMax      = Vector3.getNewVector();

    public double                   speedUp       = ConfigHandler.LiftSpeedUp;
    public double                   speedDown     = -ConfigHandler.LiftSpeedDown;
    public double                   acceleration  = 0.05;
    public boolean                  up            = true;
    public boolean                  toMoveY       = false;
    public boolean                  moved         = false;
    public boolean                  axis          = true;
    public boolean                  hasPassenger  = false;
    int                             n             = 0;
    int                             passengertime = 10;
    boolean                         first         = true;
    Random                          r             = new Random();

    public UUID                     id            = UUID.randomUUID();
    public UUID                     owner;

    public double                   prevFloorY    = 0;
    public double                   prevFloor     = 0;

    public boolean                  called        = false;
    TileEntityLiftAccess            current;

    Matrix3                         mainBox       = new Matrix3();
    Matrix3                         tempBox       = new Matrix3();

    public HashMap<String, Matrix3> boxes         = new HashMap<String, Matrix3>();
    public List<AxisAlignedBB>      blockBoxes    = Lists.newArrayList();
    public HashMap<String, Vector3> offsets       = new HashMap<String, Vector3>();
    public int[]                    floors        = new int[64];
    Matrix3                         base          = new Matrix3();

    Matrix3                         top           = new Matrix3();
    Matrix3                         wall1         = new Matrix3();

    public ItemStack[][][]          blocks        = null;

    public EntityLift(World par1World)
    {
        super(par1World);
        this.ignoreFrustumCheck = true;
        this.hurtResistantTime = 0;
        this.isImmuneToFire = true;
        for (int i = 0; i < 64; i++)
        {
            floors[i] = -1;
        }
    }

    public EntityLift(World world, double x, double y, double z)
    {
        this(world);
        this.setPosition(x, y, z);
        r.setSeed(100);
        lifts.put(id, this);
    }

    public void accelerate()
    {
        motionX = 0;
        motionZ = 0;
        if (!toMoveY) motionY *= 0.5;
        else
        {
            if (up) motionY = Math.min(speedUp, motionY + acceleration * speedUp);
            else motionY = Math.max(speedDown, motionY + acceleration * speedDown);
        }
    }

    /** Applies a velocity to each of the entities pushing them away from each
     * other. Args: entity */
    @Override
    public void applyEntityCollision(Entity entity)
    {
        Vector3 v = Vector3.getNewVector();
        Vector3 v1 = Vector3.getNewVector();

        blockBoxes.clear();

        int sizeX = blocks.length;
        int sizeY = blocks[0].length;
        int sizeZ = blocks[0][0].length;
        Set<Double> topY = Sets.newHashSet();
        for (int i = 0; i < sizeX; i++)
            for (int k = 0; k < sizeY; k++)
                for (int j = 0; j < sizeZ; j++)
                {
                    ItemStack stack = blocks[i][k][j];
                    if (stack == null || stack.getItem() == null) continue;

                    Block block = Block.getBlockFromItem(stack.getItem());
                    IBlockState state = block.getStateFromMeta(stack.getItemDamage());
                    AxisAlignedBB blockBox = block.getBoundingBox(state, worldObj, null);
                    if (blockBox != null)
                    {
                        AxisAlignedBB box = Matrix3.getAABB(posX + blockBox.minX - 0.5 + boundMin.x + i,
                                posY + blockBox.minY + k, posZ + blockBox.minZ - 0.5 + boundMin.z + j,
                                posX + blockBox.maxX + 0.5 + boundMin.x + i, posY + blockBox.maxY + k,
                                posZ + blockBox.maxZ + 0.5 + boundMin.z + j);
                        blockBoxes.add(box);
                        topY.add(box.maxY);
                    }
                }

        v.setToVelocity(entity).subtractFrom(v1.setToVelocity(this));
        v1.clear();
        Matrix3.doCollision(blockBoxes, entity.getEntityBoundingBox(), entity, 0, v, v1);
        for (Double d : topY)
            if (entity.posY >= d && entity.posY + entity.motionY <= d && motionY <= 0)
            {
                double diff = (entity.posY + entity.motionY) - (d + motionY);
                double check = Math.max(0.5, Math.abs(entity.motionY + motionY));
                if (diff > 0 || diff < -0.5 || Math.abs(diff) > check)
                {
                    entity.motionY = 0;
                }
            }

        boolean collidedY = false;
        if (!v1.isEmpty())
        {
            if (v1.y >= 0)
            {
                entity.onGround = true;
                entity.fallDistance = 0;
                entity.fall(entity.fallDistance, 0);
            }
            else if (v1.y < 0)
            {
                boolean below = entity.posY + entity.height - (entity.motionY + motionY) < posY;
                if (below)
                {
                    v1.y = 0;
                }
            }
            if (v1.x != 0) entity.motionX = 0;
            if (v1.y != 0) entity.motionY = motionY;
            if (v1.z != 0) entity.motionZ = 0;
            if (v1.y != 0) collidedY = true;
            v1.addTo(v.set(entity));
            v1.moveEntity(entity);
        }
        if (entity instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) entity;

            if (player.worldObj.isRemote && ConfigHandler.jitterfix)
            {
                Minecraft.getMinecraft().gameSettings.viewBobbing = false;
            }

            if (Math.abs(player.motionY) < 0.1 && !player.capabilities.isFlying)
            {
                entity.onGround = true;
                entity.fallDistance = 0;
            }
            if (!player.capabilities.isCreativeMode && !player.worldObj.isRemote)
            {
                EntityPlayerMP entityplayer = (EntityPlayerMP) player;
                if (collidedY) entityplayer.connection.floatingTickCount = 0;
            }
        }
    }

    /** Called when the entity is attacked. */
    public boolean attackEntityFrom(DamageSource source, int damage)
    {
        if (damage > 15) { return true; }

        return false;
    }

    @Override
    public Matrix3 bounds(Vector3 target)
    {
        int xMin = boundMin.intX();
        int zMin = boundMin.intZ();
        int xMax = boundMax.intX();
        int zMax = boundMax.intZ();

        tempBox.boxMin().set(xMin, 0, zMin);
        tempBox.boxMax().set(xMax, 1, zMax);

        return tempBox;
    }

    public void call(int floor)
    {
        if (floor == 0 || floor > 64) { return; }
        if (floors[floor - 1] > 0)
        {
            callYValue(floors[floor - 1]);
            setDestinationFloor(floor);
        }
    }

    public void callYValue(int yValue)
    {
        setDestY(yValue);
    }

    /** Returns true if other Entities should be prevented from moving through
     * this Entity. */
    @Override
    public boolean canBeCollidedWith()
    {
        return !this.isDead;
    }

    /** Returns true if this entity should push and be pushed by other entities
     * when colliding. */
    @Override
    public boolean canBePushed()
    {
        return true;
    }

    @Override
    public boolean canRenderOnFire()
    {
        return false;
    }

    public boolean checkBlocks(double dir)
    {
        boolean ret = true;
        Vector3 thisloc = Vector3.getNewVector().set(this).addTo(0, dir, 0);

        if (called)
        {
            if (dir > 0 && thisloc.y > getDestY()) { return false; }
            if (dir < 0 && thisloc.y < getDestY()) { return false; }
        }

        int xMin = boundMin.intX();
        int zMin = boundMin.intZ();
        int xMax = boundMax.intX();
        int zMax = boundMax.intZ();

        Vector3 v = Vector3.getNewVector();
        for (int i = xMin; i <= xMax; i++)
            for (int j = zMin; j <= zMax; j++)
            {
                ret = ret && (v.set(thisloc).addTo(i, 0, j)).clearOfBlocks(worldObj);
            }
        return ret;
    }

    @Override
    public void checkCollision()
    {

        int xMin = boundMin.intX();
        int zMin = boundMin.intZ();
        int xMax = boundMax.intX();
        int zMax = boundMax.intZ();

        List<?> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(posX + (xMin - 1),
                posY, posZ + (zMin - 1), posX + xMax + 1, posY + 64, posZ + zMax + 1));

        if (list != null && !list.isEmpty())
        {
            if (list.size() == 1 && this.getRecursivePassengers() != null
                    && !this.getRecursivePassengers().isEmpty()) { return; }

            for (int i = 0; i < list.size(); ++i)
            {
                Entity entity = (Entity) list.get(i);
                applyEntityCollision(entity);
            }
        }
    }

    public void clearLiquids()
    {

    }

    @SuppressWarnings("unused") // TODO make use of this
    private boolean consumePower()
    {
        boolean power = false;
        // int sizeFactor = size == 1 ? 4 : size == 3 ? 23 : 55;
        double energyCost = 0;// (destinationY - posY)*ENERGYCOST*sizeFactor;
        if (energyCost <= 0) return true;
        if (!power) toMoveY = false;
        return power;

    }

    public void doMotion()
    {
        if (up)
        {
            if (checkBlocks(motionY * (ACCELERATIONTICKS + 1)))
            {
                setPosition(posX, posY + motionY, posZ);
                moved = true;
                return;
            }
            else
            {
                while (motionY >= 0 && !checkBlocks((motionY - acceleration * speedUp / 10) * (ACCELERATIONTICKS + 1)))
                {
                    motionY = motionY - acceleration * speedUp / 10;
                }

                if (checkBlocks(motionY))
                {
                    setPosition(posX, posY + motionY, posZ);
                    moved = true;
                    return;
                }
                else
                {
                    setPosition(posX, Math.abs(posY - getDestY()) < 0.5 ? getDestY() : Math.floor(posY), posZ);
                    called = false;
                    prevFloor = getDestinationFloor();
                    prevFloorY = getDestY();
                    setDestY(-1);
                    setDestinationFloor(0);
                    current = null;
                    motionY = 0;
                    toMoveY = false;
                    moved = false;
                }
            }
        }
        else
        {
            if (checkBlocks(motionY * (ACCELERATIONTICKS + 1)))
            {
                setPosition(posX, posY + motionY, posZ);
                moved = true;
                return;
            }
            else
            {
                while (motionY <= 0
                        && !checkBlocks((motionY - acceleration * speedDown / 10) * (ACCELERATIONTICKS + 1)))
                {
                    motionY = motionY - acceleration * speedDown / 10;
                }

                if (checkBlocks(motionY))
                {
                    setPosition(posX, posY + motionY, posZ);
                    moved = true;
                    return;
                }
                else
                {
                    setPosition(posX, Math.abs(posY - getDestY()) < 0.5 ? getDestY() : Math.floor(posY), posZ);
                    called = false;
                    prevFloor = getDestinationFloor();
                    prevFloorY = getDestY();
                    setDestY(-1);
                    setDestinationFloor(0);
                    current = null;
                    motionY = 0;
                    toMoveY = false;
                    moved = false;
                }
            }
        }
        toMoveY = false;
        moved = false;
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(DESTINATIONFLOORDW, Integer.valueOf(0));
        this.dataManager.register(DESTINATIONYDW, Integer.valueOf(0));
        this.dataManager.register(CURRENTFLOORDW, Integer.valueOf(-1));
        this.dataManager.register(CAMOBLOCKDW, Optional.<ItemStack> absent());
    }

    /** returns the bounding box for this entity */
    public AxisAlignedBB getBoundingBox()
    {
        return null;
    }

    @Override
    public HashMap<String, Matrix3> getBoxes()
    {
        return boxes;
    }

    /** Checks if the entity's current position is a valid location to spawn
     * this entity. */
    public boolean getCanSpawnHere()
    {
        return false;
    }

    /** @return the destinationFloor */
    public int getCurrentFloor()
    {
        return dataManager.get(CURRENTFLOORDW);
    }

    /** @return the destinationFloor */
    public int getDestinationFloor()
    {
        return dataManager.get(DESTINATIONFLOORDW);
    }

    /** @return the destinationFloor */
    public int getDestY()
    {
        return dataManager.get(DESTINATIONYDW);
    }

    @Override
    public HashMap<String, Vector3> getOffsets()
    {
        return offsets;
    }

    @Override
    /** Applies the given player interaction to this Entity. */
    public EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, @Nullable ItemStack stack,
            EnumHand hand)
    {
        return EnumActionResult.PASS;
    }

    /** First layer of player interaction */
    @Override
    public boolean processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand)
    {
        ItemStack item = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) return false;

        if (player.isSneaking() && item != null && item.getItem() instanceof ItemLinker
                && ((owner != null && player.getUniqueID().equals(owner)) || player.capabilities.isCreativeMode))
        {
            if (item.getTagCompound() == null)
            {
                item.setTagCompound(new NBTTagCompound());
            }
            item.getTagCompound().setString("lift", id.toString());

            String message = "msg.liftSet.name";

            if (worldObj.isRemote) player.addChatMessage(new TextComponentTranslation(message));
            return true;
        }
        else if (item != null && item.getItem() instanceof ItemLinker
                && ((owner != null && player.getUniqueID().equals(owner)) || player.capabilities.isCreativeMode))
        {
            if (!worldObj.isRemote && owner != null)
            {
                Entity ownerentity = worldObj.getPlayerEntityByUUID(owner);
                String message = "msg.lift.owner";

                player.addChatMessage(new TextComponentTranslation(message, ownerentity.getName()));
            }
            return true;
        }
        if ((player.isSneaking() && item != null
                && (player.getHeldItem(hand).getItem().getUnlocalizedName().toLowerCase().contains("wrench")
                        || player.getHeldItem(hand).getItem().getUnlocalizedName().toLowerCase().contains("screwdriver")
                        || player.getHeldItem(hand).getItem().getUnlocalizedName()
                                .equals(Items.STICK.getUnlocalizedName())))
                && ((owner != null && player.getUniqueID().equals(owner)) || player.capabilities.isCreativeMode))
        {
            if (!worldObj.isRemote)
            {
                String message = "msg.lift.killed";
                player.addChatMessage(new TextComponentTranslation(message));
                setDead();
            }
            return true;
        }
        else if (player.isSneaking() && item != null && Block.getBlockFromItem(item.getItem()) != null
                && (owner == null || owner.equals(player.getUniqueID())))
        {
            Block block = Block.getBlockFromItem(item.getItem());
            if (block.getStateFromMeta(item.getItemDamage()).isNormalCube())
            {
                ItemStack item2 = item.splitStack(1);
                if (getHeldItem(null) != null && !worldObj.isRemote)
                {
                    this.entityDropItem(getHeldItem(null), 1);
                }
                if (!worldObj.isRemote) setHeldItem(null, item2);
            }
            return true;
        }
        else if (player.isSneaking() && item == null && (owner == null || owner.equals(player.getUniqueID())))
        {
            if (getHeldItem(null) != null && !worldObj.isRemote)
            {
                this.entityDropItem(getHeldItem(null), 1);
            }
            if (!worldObj.isRemote) setHeldItem(null, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPotionApplicable(PotionEffect par1PotionEffect)
    {
        return false;
    }

    @Override
    public void onUpdate()
    {
        this.prevPosY = posY;

        clearLiquids();

        if (motionY == 0)
        {
            this.setPosition(posX, Math.round(posY), posZ);
        }

        if (!checkBlocks(0)) toMoveY = false;

        toMoveY = called = getDestY() > 0;
        up = getDestY() > posY;

        accelerate();
        if (toMoveY)
        {
            doMotion();
        }
        else if (!worldObj.isRemote)
        {
            setPosition(posX, Math.round(posY), posZ);
        }

        checkCollision();
        passengertime = hasPassenger ? 20 : passengertime - 1;
        n++;
    }

    public void passengerCheck()
    {
        List<Entity> list = worldObj.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox());
        if (list.size() > 0)
        {
            hasPassenger = true;
        }
        else
        {
            hasPassenger = false;
        }
    }

    public void readBlocks(NBTTagCompound nbt)
    {
        if (nbt.hasKey("BlocksLength") || nbt.hasKey("BlocksLengthX"))
        {
            int sizeX = nbt.getInteger("BlocksLengthX");
            int sizeZ = nbt.getInteger("BlocksLengthZ");
            int sizeY = nbt.getInteger("BlocksLengthY");
            if (sizeX == 0 || sizeZ == 0)
            {
                sizeX = sizeZ = nbt.getInteger("BlocksLength");
            }
            if (sizeY == 0) sizeY = 1;

            blocks = new ItemStack[sizeX][sizeY][sizeZ];
            for (int i = 0; i < sizeX; i++)
                for (int k = 0; k < sizeY; k++)
                    for (int j = 0; j < sizeZ; j++)
                    {
                        int n = -1;
                        if (nbt.hasKey("block" + i + "," + j))
                        {
                            n = nbt.getInteger("block" + i + "," + j);
                        }
                        else if (nbt.hasKey("block" + i + "," + k + "," + j))
                        {
                            n = nbt.getInteger("block" + i + "," + k + "," + j);
                        }
                        if (n == -1) continue;
                        ItemStack b = new ItemStack(Item.getItemById(n), 1,
                                nbt.getInteger("meta" + i + "," + k + "," + j));
                        blocks[i][k][j] = b;
                    }
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt)
    {
        super.readEntityFromNBT(nbt);
        axis = nbt.getBoolean("axis");
        if (nbt.hasKey("size"))
        {
            double size = nbt.getDouble("size");
            int xMin = (int) (-size / 2);
            int zMin = (int) (-size / 2);
            int xMax = (int) (size / 2);
            int zMax = (int) (size / 2);
            boundMin.x = xMin;
            boundMin.z = zMin;
            boundMax.x = xMax;
            boundMax.z = zMax;
        }
        else if (nbt.hasKey("corners"))
        {
            int[] read = nbt.getIntArray("corners");
            int xMin = read[0];
            int zMin = read[1];
            int xMax = read[2];
            int zMax = read[3];
            boundMin.x = xMin;
            boundMin.z = zMin;
            boundMax.x = xMax;
            boundMax.z = zMax;
        }
        if (nbt.hasKey("bounds"))
        {
            NBTTagCompound bounds = nbt.getCompoundTag("bounds");
            boundMin = Vector3.readFromNBT(bounds, "min");
            boundMax = Vector3.readFromNBT(bounds, "max");
        }

        id = new UUID(nbt.getLong("higher"), nbt.getLong("lower"));
        if (nbt.hasKey("ownerhigher")) owner = new UUID(nbt.getLong("ownerhigher"), nbt.getLong("ownerlower"));

        if (nbt.hasKey("replacement"))
        {
            NBTTagCompound held = nbt.getCompoundTag("replacement");
            setHeldItem(null, ItemStack.loadItemStackFromNBT(held));
        }
        readList(nbt);
        readBlocks(nbt);
    }

    public void readList(NBTTagCompound nbt)
    {
        for (int i = 0; i < 64; i++)
        {
            floors[i] = nbt.getInteger("floors " + i);
            if (floors[i] == 0) floors[i] = -1;
        }
    }

    @Override
    public void readSpawnData(ByteBuf data)
    {
        PacketBuffer buff = new PacketBuffer(data);
        NBTTagCompound tag = new NBTTagCompound();
        try
        {
            tag = buff.readNBTTagCompoundFromBuffer();
            readEntityFromNBT(tag);
            lifts2.put(id, this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setBoxes()
    {
        int xMin = boundMin.intX();
        int zMin = boundMin.intZ();
        int xMax = boundMax.intX();
        int zMax = boundMax.intZ();
        mainBox.boxMin().set(xMin, 0, zMin);
        mainBox.boxMax().set(xMax, 1, zMax);

        // TODO add additional boxes for other blocks.

        Matrix3 m2;
        if (!boxes.containsKey("base"))
        {
            m2 = new Matrix3();
            boxes.put("base", m2);
        }
        else
        {
            m2 = boxes.get("base");
        }

        m2.boxMin().clear();
        m2.boxMax().set(xMax - xMin, 1, zMax - zMin);
        m2.boxRotation().clear();
        boxes.put("base", m2);
    }

    /** @param currentFloor
     *            the destinationFloor to set */
    public void setCurrentFloor(int currentFloor)
    {
        dataManager.set(CURRENTFLOORDW, Integer.valueOf(currentFloor));
    }

    /** Will get destroyed next tick. */
    @Override
    public void setDead()
    {
        if (!worldObj.isRemote && !this.isDead)
        {
            if (blocks != null)
            {
                for (ItemStack[][] barrarr : blocks)
                {
                    for (ItemStack[] barr : barrarr)
                        for (ItemStack b : barr)
                        {
                            if (b != null) this.entityDropItem(b, 0.5f);
                        }
                }
                if (getHeldItem(null) != null) this.entityDropItem(getHeldItem(null), 1);
            }
        }
        super.setDead();
    }

    /** @param destinationFloor
     *            the destinationFloor to set */
    public void setDestinationFloor(int destinationFloor)
    {
        dataManager.set(DESTINATIONFLOORDW, Integer.valueOf(destinationFloor));
    }

    /** @param dest
     *            the destinationFloor to set */
    public void setDestY(int dest)
    {
        dataManager.set(DESTINATIONYDW, Integer.valueOf(dest));
    }

    public void setFoor(TileEntityLiftAccess te, int floor)
    {
        if (te.floor == 0)
        {
            floors[floor - 1] = te.getPos().getY() - 2;
        }
        else if (te.floor != 0)
        {
            floors[te.floor - 1] = -1;
            floors[floor - 1] = te.getPos().getY() - 2;
        }
    }

    @Override
    public void setOffsets()
    {

        Vector3 v2;
        if (!offsets.containsKey("base"))
        {
            v2 = Vector3.getNewVector();
            offsets.put("base", v2);
        }
        else
        {
            v2 = offsets.get("base");
        }
        int xMin = boundMin.intX();
        int zMin = boundMax.intZ();
        v2.set(xMin, 0, zMin);
    }

    public void writeBlocks(NBTTagCompound nbt)
    {
        if (blocks != null)
        {
            nbt.setInteger("BlocksLengthX", blocks.length);
            nbt.setInteger("BlocksLengthY", blocks[0].length);
            nbt.setInteger("BlocksLengthZ", blocks[0][0].length);
            int sizeX = blocks.length;
            int sizeY = blocks[0].length;
            int sizeZ = blocks[0][0].length;
            for (int i = 0; i < sizeX; i++)
                for (int k = 0; k < sizeY; k++)
                    for (int j = 0; j < sizeZ; j++)
                    {
                        ItemStack b = blocks[i][k][j];
                        if (b == null || b.getItem() == null) continue;
                        nbt.setInteger("block" + i + "," + k + "," + j, Item.getIdFromItem(b.getItem()));
                        nbt.setInteger("meta" + i + "," + k + "," + j, b.getItemDamage());
                    }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt)
    {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("axis", axis);

        NBTTagCompound vector = new NBTTagCompound();
        boundMin.writeToNBT(vector, "min");
        boundMax.writeToNBT(vector, "max");
        nbt.setTag("bounds", vector);

        nbt.setLong("lower", id.getLeastSignificantBits());
        nbt.setLong("higher", id.getMostSignificantBits());

        if (owner != null)
        {
            nbt.setLong("ownerlower", owner.getLeastSignificantBits());
            nbt.setLong("ownerhigher", owner.getMostSignificantBits());
        }
        if (getHeldItem(null) != null)
        {
            NBTTagCompound held = new NBTTagCompound();
            getHeldItem(null).writeToNBT(held);
            nbt.setTag("replacement", held);
        }

        writeList(nbt);
        try
        {
            writeBlocks(nbt);
        }
        catch (Exception e)
        {
        }
    }

    public void writeList(NBTTagCompound nbt)
    {
        for (int i = 0; i < 64; i++)
        {
            nbt.setInteger("floors " + i, floors[i]);
        }
    }

    @Override
    public void writeSpawnData(ByteBuf data)
    {
        PacketBuffer buff = new PacketBuffer(data);
        NBTTagCompound tag = new NBTTagCompound();
        writeEntityToNBT(tag);
        buff.writeNBTTagCompoundToBuffer(tag);
        lifts.put(id, this);
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList()
    {
        return Lists.newArrayList();
    }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn)
    {
        return null;
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack)
    {
    }

    @Override
    public ItemStack getHeldItem(EnumHand hand)
    {
        return dataManager.get(CAMOBLOCKDW).orNull();
    }

    @Override
    public void setHeldItem(EnumHand hand, @Nullable ItemStack stack)
    {
        if (stack != null)
        {
            dataManager.set(CAMOBLOCKDW, Optional.of(stack));
        }
        else
        {
            dataManager.set(CAMOBLOCKDW, Optional.<ItemStack> absent());
        }
    }

    @Override
    public EnumHandSide getPrimaryHand()
    {
        return EnumHandSide.LEFT;
    }

}
