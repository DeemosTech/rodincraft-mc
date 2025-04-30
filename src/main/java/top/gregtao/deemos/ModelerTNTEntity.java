package top.gregtao.deemos;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class ModelerTNTEntity extends Entity {
    private static final TrackedData<Integer> FUSE = DataTracker.registerData(ModelerTNTEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(ModelerTNTEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final List<Block> BLOCKS = new ArrayList<>();

    static {
        BLOCKS.add(Blocks.BLACKSTONE);
        BLOCKS.add(Blocks.STONE_BRICKS);
        BLOCKS.add(Blocks.STONE_BRICK_SLAB);
        BLOCKS.add(Blocks.STONE_BRICK_STAIRS);
        BLOCKS.add(Blocks.STONE);
        BLOCKS.add(Blocks.WHITE_WOOL);
        BLOCKS.add(Blocks.GRAY_WOOL);
        BLOCKS.add(Blocks.GRAY_CONCRETE);
        BLOCKS.add(Blocks.WHITE_CONCRETE);
        BLOCKS.add(Blocks.OBSIDIAN);
        BLOCKS.add(Blocks.GRANITE);
        BLOCKS.add(Blocks.IRON_ORE);
    }

    private int fuseTimer = 200, color = 0xFFFFFFFF;
    private UUID uuid;
    private int yDelta = 0, xSize = 0, ySize = 0, zSize = 0;
    private List<Pair<BlockPos, Block>> modelBlocks;
    private Direction facing = Direction.NORTH;

    public ModelerTNTEntity(EntityType<? extends Entity> entityType, World world) {
        super(entityType, world);
        this.inanimate = true;
    }

    public ModelerTNTEntity(World world, UUID uuid, double x, double y, double z, Direction direction, int color) {
        this(RodinServer.MODELER_TNT_ENTITY, world);
        this.setPosition(x, y, z);
        double d = world.random.nextDouble() * 6.2831854820251465;
        this.setVelocity(-Math.sin(d) * 0.02, 0.2f, -Math.cos(d) * 0.02);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.uuid = uuid;
        this.color = color;
        this.facing = direction;
        this.loadModelBlocks();
    }

    public void loadModelBlocks() {
        if (this.uuid == null || this.world.isClient) return;
        this.modelBlocks = new ArrayList<>();
        List<Pair<BlockPos, Block>> original = RodinCraftConfig.JOBS.get(this.uuid);
        if (original != null) {
            for (Pair<BlockPos, Block> pair : original) {
                BlockPos pos = adaptDirection(pair.getLeft(), this.facing);

                this.yDelta = Math.max(-pos.getY(), this.yDelta);
                this.xSize = Math.max(Math.abs(pos.getX()) * 2, this.xSize);
                this.ySize = Math.max(Math.abs(pos.getY()) * 2, this.ySize);
                this.zSize = Math.max(Math.abs(pos.getZ()) * 2, this.zSize);

                this.modelBlocks.add(new Pair<>(pos, pair.getRight()));
            }
        }
    }

    private static BlockPos adaptDirection(BlockPos pos, Direction direction) {
        int nx, nz;
        switch (direction) {
            case NORTH:
                nx = pos.getX();
                nz = pos.getZ();
                break;
            case SOUTH:
                nx = -pos.getX();
                nz = -pos.getZ();
                break;
            case WEST:
                nx = pos.getZ();
                nz = -pos.getX();
                break;
            default:
                nx = -pos.getZ();
                nz = pos.getX();
                break;
        }
        return new BlockPos(nx, pos.getY(), nz);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(FUSE, 200);
        this.dataTracker.startTracking(COLOR, 0xFFFFFFFF);
    }

    @Override
    protected boolean canClimb() {
        return false;
    }

    @Override
    public boolean collides() {
        return !this.removed;
    }

    @Override
    public void tick() {
        --this.fuseTimer;
        if (this.fuseTimer <= 0) {
            this.remove();
            if (!this.world.isClient) {
                this.cleanVoxel();
                this.placeModelBlocks();
            }
        } else if (this.fuseTimer < 120) {
            if (!this.world.isClient && this.fuseTimer % 10 == 0 && !this.modelBlocks.isEmpty()) {
                this.randomVoxel();
            }
            if (this.fuseTimer == 110 && this.world.isClient) {
                this.world.playSound(
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                        2.0f, (1.0f + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2f) * 0.7f,
                        false
                );
            }
        } else {
            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
            }
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.98));
            if (this.onGround) {
                this.setVelocity(this.getVelocity().multiply(0.7, -0.5, 0.7));
            }
            this.updateWaterState();
            if (this.world.isClient) {
                this.world.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    private void placeModelBlocks() {
        for (Pair<BlockPos, Block> pair : this.modelBlocks) {
            this.world.setBlockState(pair.getLeft().add(
                    this.getBlockPos().getX(),
                    this.getBlockPos().getY() + this.yDelta
                    , this.getBlockPos().getZ()
            ), pair.getRight().getDefaultState());
        }
    }

    private void cleanVoxel() {
        for (int x = this.getBlockPos().getX() - this.xSize / 2; x <= this.getBlockPos().getX() + this.xSize / 2; ++x) {
            for (int y = this.getBlockPos().getY(); y <= this.getBlockPos().getY() + this.ySize; ++y) {
                for (int z = this.getBlockPos().getZ() - this.zSize / 2; z <= this.getBlockPos().getZ() + this.zSize / 2; ++z) {
                    this.world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private void randomVoxel() {
        this.cleanVoxel();
        this.placeModelBlocks();
        Random random = new Random();
        for (int x = this.getBlockPos().getX() - this.xSize / 2; x <= this.getBlockPos().getX() + this.xSize / 2; ++x) {
            for (int y = this.getBlockPos().getY(); y <= this.getBlockPos().getY() + this.ySize; ++y) {
                for (int z = this.getBlockPos().getZ() - this.zSize / 2; z <= this.getBlockPos().getZ() + this.zSize / 2; ++z) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (this.random.nextInt(240) < this.fuseTimer) {
                        BlockState state;
                        if (this.random.nextInt(120) < this.fuseTimer) {
                            state = BLOCKS.get(random.nextInt(BLOCKS.size())).getDefaultState();
                        } else {
                            Block block = this.modelBlocks.get(random.nextInt(this.modelBlocks.size())).getRight();
                            if (block instanceof FallingBlock)
                                block = Blocks.STONE;
                            state = block.getDefaultState();
                        }
                        this.world.setBlockState(blockPos, state);
                    }
                }
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.uuid != null)
            nbt.putUuid("JobUuid", this.uuid);
        nbt.putInt("Facing", this.facing.getId());
        nbt.putShort("Fuse", (short)this.getFuseTimer());
        nbt.putInt("Color", this.color);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setFuse(nbt.getShort("Fuse"));
        this.color = nbt.getInt("Color");
        this.dataTracker.set(COLOR, this.color);
        if (nbt.contains("JobUuid"))
            this.uuid = nbt.getUuid("JobUuid");
        this.facing = Direction.byId(nbt.getInt("Facing"));
        this.loadModelBlocks();
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.15f;
    }

    public void setFuse(int fuse) {
        this.dataTracker.set(FUSE, fuse);
        this.fuseTimer = fuse;
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (FUSE.equals(data)) {
            this.fuseTimer = this.getFuse();
        } else if (COLOR.equals(data)) {
            this.color = this.dataTracker.get(COLOR);
        }
    }

    public int getFuse() {
        return this.dataTracker.get(FUSE);
    }

    public int getFuseTimer() {
        return this.fuseTimer;
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.color);
    }
}

