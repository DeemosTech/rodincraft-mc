package top.gregtao.deemos;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.TntMinecartEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

public class ModelerTNTRenderer extends EntityRenderer<ModelerTNTEntity> {
    public ModelerTNTRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        super(entityRenderDispatcher);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(ModelerTNTEntity tntEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.translate(0.0, 0.5, 0.0);
        if ((float)tntEntity.getFuseTimer() - 110 - g + 1.0f < 10.0f) {
            float h = 1.0f - ((float)tntEntity.getFuseTimer() - 110 - g + 1.0f) / 10.0f;
            h = MathHelper.clamp(h, 0.0f, 1.0f);
            h *= h;
            h *= h;
            float j = 1.0f + h * 0.3f;
            matrixStack.scale(j, j, j);
        }
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0f));
        matrixStack.translate(-0.5, -0.5, 0.5);
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90.0f));
        TntMinecartEntityRenderer.renderFlashingBlock(RodinServer.MODELER_TNTBLOCK.getDefaultState(), matrixStack, vertexConsumerProvider, i,
                tntEntity.getFuseTimer() / 5 % 2 == 0);
        matrixStack.pop();
        super.render(tntEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

//    public static void renderFlashingBlock(BlockState blockState, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, boolean drawFlash, int color) {
//        int i = drawFlash ? OverlayTexture.packUv(OverlayTexture.getU(1.0f), 10) : OverlayTexture.DEFAULT_UV;
//        renderBlockAsEntity(MinecraftClient.getInstance().getBlockRenderManager(), blockState, matrices, vertexConsumers, light, i, color);
//    }
//
//    public static void renderBlockAsEntity(BlockRenderManager manager, BlockState state, MatrixStack matrices,
//                                           VertexConsumerProvider vertexConsumer, int light, int overlay, int color) {
//        BakedModel bakedModel = manager.getModel(state);
//        float r = (float)(color >> 16 & 0xFF) / 255.0f;
//        float g = (float)(color >> 8 & 0xFF) / 255.0f;
//        float b = (float)(color & 0xFF) / 255.0f;
//        manager.getModelRenderer().render(matrices.peek(),
//                vertexConsumer.getBuffer(TexturedRenderLayers.getEntityTranslucentCull()),
//                state, bakedModel, r, g, b, light, overlay);
//    }

    @Override
    public Identifier getTexture(ModelerTNTEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}

