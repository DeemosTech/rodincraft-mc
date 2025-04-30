package top.gregtao.deemos.mixin;

import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.gregtao.deemos.RodinServer;

@Mixin(ItemColors.class)
public class ItemColorsMixin {

    @Inject(method = "create(Lnet/minecraft/client/color/block/BlockColors;)Lnet/minecraft/client/color/item/ItemColors;", at = @At("RETURN"), cancellable = true)
    private static void injected(BlockColors blockColors, CallbackInfoReturnable<ItemColors> cir) {
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            if (world == null || pos == null) {
                return -1;
            }
            return BiomeColors.getGrassColor(world, state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos);
        });
        ItemColors colors = cir.getReturnValue();
        colors.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                NbtCompound tag = stack.getTag();
                if (tag != null && tag.contains("Color")) {
                    return tag.getInt("Color");
                }
            }
            return -1;
        }, RodinServer.MODELER_TNT);
        cir.setReturnValue(colors);
    }
}
