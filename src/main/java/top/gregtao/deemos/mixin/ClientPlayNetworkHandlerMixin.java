package top.gregtao.deemos.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.gregtao.deemos.RodinServer;
import top.gregtao.deemos.ModelerTNTEntity;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onEntitySpawn(Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;)V", at = @At(value = "TAIL"))
    private void injected(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        EntityType<?> entityType = packet.getEntityTypeId();
        ClientWorld world = MinecraftClient.getInstance().world;
        if (entityType == RodinServer.MODELER_TNT_ENTITY && world != null) {
            Entity entity = new ModelerTNTEntity(world, null, packet.getX(), packet.getY(), packet.getZ(), Direction.NORTH, packet.getEntityData());
            int entity22 = packet.getId();
            entity.updateTrackedPosition(packet.getX(), packet.getY(), packet.getZ());
            entity.refreshPositionAfterTeleport(packet.getX(), packet.getY(), packet.getZ());
            entity.pitch = (float)(packet.getPitch() * 360) / 256.0f;
            entity.yaw = (float)(packet.getYaw() * 360) / 256.0f;
            entity.setEntityId(entity22);
            entity.setUuid(packet.getUuid());
            world.addEntity(entity22, entity);
        }
    }
}
