package top.gregtao.deemos;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.UUID;

public class ModelerTNT extends Item {

    public ModelerTNT(Settings settings) {
        super(settings);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world instanceof ServerWorld) {
            ItemStack itemStack = context.getStack();
            BlockPos blockPos = context.getBlockPos();
            Direction direction = context.getSide();
            BlockState blockState = world.getBlockState(blockPos);

            BlockPos pos;
            if (blockState.getCollisionShape(world, blockPos).isEmpty()) {
                pos = blockPos;
            } else {
                pos = blockPos.offset(direction);
            }

            NbtCompound nbt = itemStack.getOrCreateTag();
            if (nbt.contains("JobUuid")) {
                UUID uuid = nbt.getUuid("JobUuid");
                int color = nbt.contains("Color") ? nbt.getInt("Color") : 0xFFFFFFFF;
                if (RodinCraftConfig.JOBS.containsKey(uuid)) {
                    ModelerTNTEntity entity = new ModelerTNTEntity(
                            world, uuid,
                            (double) pos.getX() + 0.5, pos.getY(), (double) pos.getZ() + 0.5,
                            context.getPlayerFacing(), color
                    );
                    world.spawnEntity(entity);
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
                } else if (context.getPlayer() != null) {
                    context.getPlayer().sendMessage(new TranslatableText("dmodel.expired"), true);
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}
