package top.gregtao.deemos;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.CyclingOption;
import net.minecraft.client.option.DoubleOption;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ModelerScreen extends BaseScreen {
    public static final String[] SUFFIXES = new String[]{".png", ".jpg", ".gif", ".bmp", ".jpeg", ".webp", ".tiff", ".raw", ".glb", ".gltf"};
    public static final Text RODIN_CRAFT = new LiteralText("RodinCraft");

    private static final List<Slot> SLOTS = Lists.newArrayList();

    public static final int[] SIZE = new int[]{20, 20, 20, 20, 20};
    public static final int[] PALETTE = new int[]{0, 0, 0, 0, 0};
    public static final String[] NAMES = new String[]{"", "", "", "", ""};
    public static final String[] PROMPTS = new String[]{"", "", "", "", ""};

    public static final DoubleOption[] SIZE_OPTION = new DoubleOption[5];
    public static final CyclingOption[] PALETTE_OPTION = new CyclingOption[5];
    public PlaceholderTextField[] names = new PlaceholderTextField[5];
    public PlaceholderTextField[] prompts = new PlaceholderTextField[5];

    public static final CyclingOption TRIAL_OPTION = new CyclingOption("dmodel.option.trial", (gameOptions, amount) ->
            RodinCraftConfig.TRIAL = !RodinCraftConfig.TRIAL,
            (gameOptions, option) -> new TranslatableText("options.generic_value",
                    new TranslatableText("dmodel.option.trial"),
                    new TranslatableText("dmodel.option.trial." + RodinCraftConfig.TRIAL)));

    public double mouseX = 0, mouseY = 0;

    static {
        for (int i = 0; i < 5; ++i) {
            SLOTS.add(new Slot());
            int finalI = i;
            SIZE_OPTION[i] = new DoubleOption(
                    "dmodel.option.size", 5, 60, 1f,
                    gameOptions -> (double) SIZE[finalI], (gameOptions, value) -> SIZE[finalI] = value.intValue(),
                    (gameOptions, option) -> {
                        double value = option.get(gameOptions);
                        return new TranslatableText("options.generic_value", new TranslatableText("dmodel.option.size"), value);
                    });
            PALETTE_OPTION[i] = new CyclingOption("dmodel.option.palette", (gameOptions, amount) ->
                    PALETTE[finalI] = (PALETTE[finalI] + amount) % 3,
                    (gameOptions, option) ->
                            new TranslatableText("options.generic_value", new TranslatableText("dmodel.option.palette"),
                                    new TranslatableText("dmodel.option.palette." + PALETTE[finalI]))
            );
        }
    }

    public ModelerScreen() {
        super(Text.of("Drop file here"));
    }

    @Override
    protected void init() {
        super.init();
        SLOTS.forEach(slot -> {
            slot.tick();
            if (slot.isEmpty()) slot.clear();
        });
        this.updatePosition(this.height);

        for (int i = 0, height = (this.height - 30) / 5; i < 5; ++i) {
            int finalI = i;
            this.addButton(SIZE_OPTION[i].createButton(MinecraftClient.getInstance().options, this.width / 2 - 200, 30 + height * i, 75));

            this.addButton(PALETTE_OPTION[i].createButton(MinecraftClient.getInstance().options, this.width / 2 - 120, 30 + height * i, 75));
            this.names[i] = new PlaceholderTextField(this.textRenderer,
                    this.width / 2 - 40, 30 + height * i, 40, 20,
                    Text.of(""), new TranslatableText("dmodel.name"));
            this.names[i].setText(NAMES[i]);
            this.addButton(this.names[i]);

            this.prompts[i] = new PlaceholderTextField(this.textRenderer,
                    this.width / 2 + 5, 30 + height * i, 150, 20,
                    Text.of(""), new TranslatableText("dmodel.prompt"));
            this.prompts[i].setText(PROMPTS[i]);
            this.addButton(this.prompts[i]);

            this.addButton(new ButtonWidget(this.width / 2 + 160, 30 + height * i, 40, 20, new TranslatableText("dmodel.generate"), (button) -> {
                String prompt = this.prompts[finalI].getText();
                Slot slot = SLOTS.get(finalI);
                if (prompt.trim().isEmpty() || !slot.isEmpty()) return;
                PROMPTS[finalI] = prompt;
                NAMES[finalI] = this.names[finalI].getText();
                slot.setJob(GeneratingJob.newJob(prompt, SIZE[finalI], PALETTE[finalI], NAMES[finalI]));
            }));
        }

        this.addButton(TRIAL_OPTION.createButton(MinecraftClient.getInstance().options, this.width - 50, 0, 50));
        this.addButton(new ButtonWidget(this.width - 100, 0, 50, 20, new TranslatableText("dmodel.api_key"), (button) -> {
            if (this.client != null) this.client.openScreen(new ApiKeyScreen());
        }));

        int titleWidth = this.textRenderer.getWidth(RODIN_CRAFT), titleX = (this.width - titleWidth) / 2;
        this.addButton(new PressableTextWidget(titleX, 5, titleWidth, 10, false, RODIN_CRAFT, button -> {
            if (this.client != null) this.client.openScreen(new ApiKeyScreen());
        }, this.textRenderer));
    }

    @Override
    public void tick() {
        super.tick();
        SLOTS.forEach(Slot::tick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    @Override
    public void filesDragged(List<Path> paths) {
        if (this.client == null || this.client.player == null || paths.isEmpty()) return;
        String name = paths.get(0).toString().toLowerCase();
        if (Arrays.stream(SUFFIXES).noneMatch(name::endsWith)) return;

        int index = MathHelper.clamp((int) ((this.mouseY - 20) * 5 / (this.height - 30)), 0, 4);
        Slot slot = SLOTS.get(index);
        if (slot.isEmpty()) {
            PROMPTS[index] = this.prompts[index].getText();
            NAMES[index] = this.names[index].getText();
            slot.setJob(GeneratingJob.newJob(paths.get(0), PROMPTS[index], SIZE[index], PALETTE[index], NAMES[index]));
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        this.updatePosition(height);
    }

    public void updatePosition(int height) {
        int slotHeight = (height - 30) / 5;
        for (int i = 0; i < 5; ++i) {
            Slot slot = SLOTS.get(i);
            slot.updatePosition(20 + slotHeight * i, slotHeight);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        SLOTS.forEach(slot -> slot.render(matrices, this.width, this.textRenderer));
        this.textRenderer.draw(matrices, new TranslatableText("dmodel.generate_tips"), (float) this.width / 2 - 200, 18, 0xFFFFFF);
    }

    private static class Slot {
        private GeneratingJob job;
        private int progress = 0;
        private int y = 0, height = 0;
        private String message = "";
        private boolean isError = false;

        public void updatePosition(int y, int height) {
            this.y = y;
            this.height = height;
        }

        public void clear() {
            this.progress = 0;
            this.message = "";
            this.isError = false;
        }

        public boolean isEmpty() {
            return this.job == null;
        }

        public void setJob(GeneratingJob job) {
            this.clear();
            this.job = job;
        }

        public void tick() {
            if (this.job != null) {
                this.progress = this.job.getProgress();
                this.message = this.job.getMessage();
                this.isError = this.job.isError();
                if (this.progress == 100) {
                    this.job = null;
                }
            }
        }

        public void render(MatrixStack matrices, int width, TextRenderer textRenderer) {
            int barHeight = 2;
            int y = this.y + this.height - 9;

            fill(matrices, 0, y, width, y + barHeight, 0x55AAAAAA);
            if (this.progress > 0) {
                int filled = (int) (width * (this.progress / 100.0));
                fill(matrices, 0, y, filled, y + barHeight, this.isError ? 0xFFFF3333 : 0xFF00FF00);
                drawCenteredText(matrices, textRenderer, this.message + ": " + this.progress + "%",
                        width / 2, y - 3, 0xFFFFFF);
            }
        }
    }

    public enum Palette {
        ALL,
        COLOR,
        BUILD
    }
}
