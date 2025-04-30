package top.gregtao.deemos;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Networking {
    public static Identifier CHANNEL = new Identifier("dmodel", "channel");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL, Networking::serverDataReceiver);
    }

    public static void serverDataReceiver(MinecraftServer server, ServerPlayerEntity player,
                                         ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        String[] strings = buf.readString(100000000).split("\\|");
        if (strings.length != 3) return;
        player.sendMessage(new TranslatableText("dmodel.created", strings[1]), false);
        List<Pair<BlockPos, Block>> modelBlocks = GeneratingJob.getBlocks(strings[2], 400);
        UUID uuid = UUID.fromString(strings[0]);
        BlockPos modelSize = calcModelSize(modelBlocks);

        RodinCraftConfig.JOBS.put(uuid, modelBlocks);
        RodinCraftConfig.saveJob(uuid, modelBlocks);

        ItemStack stack = new ItemStack(RodinServer.MODELER_TNT);
        NbtCompound nbt = stack.getOrCreateTag();
        nbt.putUuid("JobUuid", uuid);
        nbt.putInt("Color", 0xFF000000 | new Random().nextInt(0xFFFFFF));
        stack.setTag(nbt);
        stack.setCustomName(Text.of(strings[1] + String.format(": %dx%dx%d", modelSize.getX(), modelSize.getY(), modelSize.getZ())));
        player.giveItemStack(stack);
    }

    private static BlockPos calcModelSize(List<Pair<BlockPos, Block>> modelBlocks) {
        int xSize = 0, ySize = 0, zSize = 0;
        for (Pair<BlockPos, Block> pair : modelBlocks) {
            BlockPos pos = pair.getLeft();

            xSize = Math.max(Math.abs(pos.getX()) * 2, xSize);
            ySize = Math.max(Math.abs(pos.getY()) * 2, ySize);
            zSize = Math.max(Math.abs(pos.getZ()) * 2, zSize);
        }
        return new BlockPos(xSize, ySize, zSize);
    }

    public static void clientSender(UUID uuid, String name, String blocks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(uuid + "|" + name + "|" + blocks, 100000000);
        ClientPlayNetworking.send(CHANNEL, buf);
    }
}
