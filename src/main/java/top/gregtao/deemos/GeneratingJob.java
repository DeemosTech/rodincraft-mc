package top.gregtao.deemos;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class GeneratingJob {
    public static GeneratingJob newJob(Path path, String prompt, int size, int palette, String name) {
        GeneratingJob job = new GeneratingJob(path, prompt, size, ModelerScreen.Palette.values()[palette], name,
                RodinCraftConfig.TRIAL ? RodinCraftConfig.DEFAULT_API_KEY : RodinCraftConfig.API_KEY);
        RodinClient.LOGGER.info("Reading image: {}", path);
        if (path.getFileName().toString().endsWith(".glb")) {
            job.runAsyncSimpleJob();
        } else {
            job.runAsyncJob();
        }
        return job;
    }

    public static GeneratingJob newJob(String prompt, int size, int palette, String name) {
        GeneratingJob job = new GeneratingJob(null, prompt, size, ModelerScreen.Palette.values()[palette], name,
                RodinCraftConfig.TRIAL ? RodinCraftConfig.DEFAULT_API_KEY : RodinCraftConfig.API_KEY);
        RodinClient.LOGGER.info("Prompt: {}", prompt);
        job.runAsyncJob();
        return job;
    }

    private static final OkHttpClient CLIENT = new OkHttpClient();

    public static final Text TRIAL_END = appendLinkText(new TranslatableText("dmodel.trial_end"));

    public static Text appendLinkText(MutableText text) {
        MutableText tip = ApiKeyScreen.GET_API_KEY.copy();
        return text.append(tip.setStyle(tip.getStyle().withFormatting(Formatting.UNDERLINE).withClickEvent(
                new ClickEvent(ClickEvent.Action.OPEN_URL, "https://hyper3d.ai/"))));
    }

    private final File image;
    private final int size;
    private final UUID uuid;
    private final ModelerScreen.Palette palette;
    private final String name, prompt, apiKey;

    private String message;
    private int progress = 0;
    private boolean isError = false;

    private GeneratingJob(@Nullable Path path, String prompt, int size, ModelerScreen.Palette palette, String name, String apiKey) {
        this.prompt = prompt.trim();
        if (path != null) {
            this.image = path.toFile();
            this.name = name.trim().isEmpty() ? this.image.getName() : name.trim();
        } else {
            this.image = null;
            this.name = name.trim().isEmpty() ? this.prompt : name.trim();
        }
        this.uuid = UUID.randomUUID();
        this.size = size;
        this.message = new TranslatableText("dmodel.generating").getString();
        this.palette = palette;
        this.apiKey = apiKey;
    }

    public int getProgress() {
        return this.progress;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isError() {
        return this.isError;
    }

    private static void checkError(Response response) throws IOException {
        if (response.code() == 500 && response.body() != null) {
            String json = response.body().string();
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            throw new IOException(root.get("details").getAsString());
        }
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Unexpected code " + response);
        }
    }

    private Pair<String, String> upload() throws IOException {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.getGameProfile().getName() == null) throw new IOException("player is null");

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prompt", this.prompt)
                .addFormDataPart("key", this.apiKey)
                .addFormDataPart("player", player.getGameProfile().getName());

        if (this.image != null) {
             builder.addFormDataPart("images", this.image.getName(),
                            RequestBody.create(this.image, MediaType.parse("image/png")));
             RodinClient.LOGGER.info("Calling for a new generating job {}, prompt: {}", this.image.getName(), this.prompt);
        } else {
             builder.addFormDataPart("images", "null");
             RodinClient.LOGGER.info("Calling for a new generating job, prompt: {}", this.prompt);
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(RodinCraftConfig.API_URL + "/upload")
                .post(requestBody)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            checkError(response);
            assert response.body() != null;

            String json = response.body().string();
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            int limit = root.get("limit").getAsInt();
            if (limit <= 0) {
                player.sendMessage(TRIAL_END, false);
                throw new IOException(new TranslatableText("dmodel.trial_end").getString());
            } else if (RodinCraftConfig.TRIAL) {
                player.sendMessage(appendLinkText(new TranslatableText("dmodel.trial_limit", limit)), false);
            }
            return new Pair<>(root.get("subscription_key").getAsString(), root.get("uuid").getAsString());
        }
    }

    private int checkStatus(String subKey) throws IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("subscription_key", subKey);
        params.put("key", this.apiKey);

        Request request = new Request.Builder()
                .url(RodinCraftConfig.API_URL + "/status")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(new Gson().toJson(params), MediaType.parse("application/json")))
                .build();

        RodinClient.LOGGER.info("Checking status: {}", subKey);
        try (Response response = CLIENT.newCall(request).execute()) {
            checkError(response);
            assert response.body() != null;

            String json = response.body().string();
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            String res = root.get("status").getAsString();

            if (res.equals("Done")) return 0;
            if (res.equals("Failed")) return -1;
            return 1;
        }
    }

    private String download(String uuid) throws IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("size", String.valueOf(this.size));
        params.put("palette", this.palette.name().toLowerCase());
        params.put("key", this.apiKey);

        Request request = new Request.Builder()
                .url(RodinCraftConfig.API_URL + "/download")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(new Gson().toJson(params), MediaType.parse("application/json")))
                .build();

        RodinClient.LOGGER.info("Trying downloading and voxelizing: {}", uuid);
        try (Response response = CLIENT.newCall(request).execute()) {
            checkError(response);
            assert response.body() != null;

            String json = response.body().string();
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            boolean success = root.get("success").getAsBoolean();
            if (success) {
                return root.get("data").getAsString();
            }
            return null;
        }
    }

    private String parse2Blocks(File file) throws IOException {
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", file.getName(), RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .addFormDataPart("size", String.valueOf(this.size))
                .addFormDataPart("palette", this.palette.name().toLowerCase());

        Request request = new Request.Builder()
                .url(RodinCraftConfig.API_URL + "/parse")
                .post(requestBodyBuilder.build())
                .build();

        RodinClient.LOGGER.info("Voxelizing: {}", file.getName());
        try (Response response = CLIENT.newCall(request).execute()) {
            checkError(response);
            assert response.body() != null;

            return response.body().string();
        }
    }

    public static List<Pair<BlockPos, Block>> getBlocks(String raw, int sizeLimit) {
        List<Pair<BlockPos, Block>> blocks = new ArrayList<>();
        String[] rawBlocks = raw.trim().split(";");
        for (String rawBlock : rawBlocks) {
            String[] args = rawBlock.split(",");
            if (args.length != 4) continue;
            BlockPos pos = new BlockPos(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            if (pos.getX() > sizeLimit || pos.getY() > sizeLimit || pos.getZ() > sizeLimit) continue;
            Block block;
            if (args[3].equals("air")) {
                block = Blocks.AIR;
            } else {
                block = Registry.BLOCK.get(new Identifier("minecraft", args[3]));
                if (block.is(Blocks.AIR))
                    RodinClient.LOGGER.warn("Unknown block detected: {}", args[3]);
            }
            blocks.add(new Pair<>(pos, block));
        }
        return blocks;
    }

    private void runAsyncSimpleJob() {
        CompletableFuture.runAsync(() -> {
            try {
                this.progress = 30;
                String raw = parse2Blocks(this.image);
                this.progress = 70;
                Networking.clientSender(this.uuid, this.name, raw);
                this.message = new TranslatableText("dmodel.done").getString();
            } catch (Exception e) {
                this.message = e.getMessage();
                this.isError = true;
                RodinClient.LOGGER.error("Error during simple job", e);
                throw new CompletionException(e);
            } finally {
                this.progress = 100;
            }
        });
    }

    private void runAsyncJob() {
        CompletableFuture.supplyAsync(() -> {
            try {
                this.progress = 10;
                Pair<String, String> result = upload();
                this.progress = 30;
                return result;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }).thenCompose(keys -> CompletableFuture.supplyAsync(() -> {
            try {
                for (int i = 1; i <= 150; ++i) {
                    int status = checkStatus(keys.getLeft());
                    if (status == 0) {
                        this.progress = 60;
                        return keys.getRight(); // uuid
                    }
                    if (status == -1) break;
                    Thread.sleep(1000);
                }
                throw new IOException("生成失败！");
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        })).thenCompose(uuid -> CompletableFuture.supplyAsync(() -> {
            try {
                for (int i = 1; i <= 150; ++i) {
                    String data = download(uuid);
                    if (data != null && !data.isEmpty()) return data;
                    Thread.sleep(1000);
                }
                throw new IOException("生成失败！");
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        })).thenAcceptAsync(data -> {
            this.progress = 90;
            Networking.clientSender(this.uuid, this.name, data);
        }).whenComplete((value, e) -> {
            this.progress = 100;
            if (e != null) {
                this.isError = true;
                this.message = e.getMessage();
                RodinClient.LOGGER.error("Error during job", e);
            } else {
                this.message = new TranslatableText("dmodel.done").getString();
            }
        });
    }

}
