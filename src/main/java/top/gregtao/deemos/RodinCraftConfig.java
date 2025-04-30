package top.gregtao.deemos;

import net.minecraft.block.Block;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RodinCraftConfig {
    public static final File FOLDER = new File("models");
    public static final File KEY_FILE = new File(FOLDER, "api_key");

    public static final HashMap<UUID, List<Pair<BlockPos, Block>>> JOBS = readJobs();

    static final String API_URL = "https://rodincraft.deemos.com";
    public static String DEFAULT_API_KEY = "trial";
    public static boolean TRIAL = true;
    public static String API_KEY = "";

    public static void readApiKey() {
        try {
            if (!FOLDER.exists() && !FOLDER.mkdirs()) throw new IOException("Failed to create folder");
            if (!KEY_FILE.exists()) {
                if (!KEY_FILE.createNewFile()) throw new IOException("Failed to create file");
                Files.write(KEY_FILE.toPath(), (TRIAL + ";").getBytes(StandardCharsets.UTF_8));
                return;
            }

            String[] args = new String(Files.readAllBytes(KEY_FILE.toPath())).trim().split(";");
            TRIAL = Boolean.parseBoolean(args[0]);
            API_KEY = args.length < 2 || args[1].isEmpty() ? DEFAULT_API_KEY : args[1];
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeApiKey() {
        try {
            if (!FOLDER.exists() && !FOLDER.mkdirs()) throw new IOException("Failed to create folder");
            if (!KEY_FILE.exists() && !KEY_FILE.createNewFile()) throw new IOException("Failed to create file");

            Files.write(KEY_FILE.toPath(), (TRIAL + ";" + API_KEY).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<UUID, List<Pair<BlockPos, Block>>> readJobs() {
        HashMap<UUID, List<Pair<BlockPos, Block>>> jobs = new HashMap<>();
        try {
            if (!FOLDER.exists() && !FOLDER.mkdirs()) throw new IOException("Failed to create folder");
            File[] files = FOLDER.listFiles((dir, name) -> name.endsWith(".dat"));

            if (files != null) {
                for (File file : files) {
                    String raw = new String(Files.readAllBytes(file.toPath()));
                    UUID uuid = UUID.fromString(file.getName().replace(".dat", ""));
                    jobs.put(uuid, GeneratingJob.getBlocks(raw, 400));
                }
            }
            return jobs;
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveJob(UUID uuid, List<Pair<BlockPos, Block>> blocks) {
        try {
            if (!FOLDER.exists() && !FOLDER.mkdirs()) throw new IOException("Failed to create folder");
            File file = new File(FOLDER, uuid.toString() + ".dat");
            if (!file.createNewFile()) throw new IOException("Failed to create file");

            Files.write(file.toPath(), blocks.stream().map(pair -> {
                BlockPos pos = pair.getLeft();
                Block block = pair.getRight();
                return String.format("%d,%d,%d,%s", pos.getX(), pos.getY(), pos.getZ(), Registry.BLOCK.getId(block).getPath());
            }).collect(Collectors.joining(";")).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
