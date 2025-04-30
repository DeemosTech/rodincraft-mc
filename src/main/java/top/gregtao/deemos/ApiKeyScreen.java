package top.gregtao.deemos;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class ApiKeyScreen extends BaseScreen {
    public PlaceholderTextField keyField;
    public static final Text GET_API_KEY = new TranslatableText("dmodel.get_api_key").setStyle(
            Style.EMPTY.withFormatting(Formatting.UNDERLINE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://hyper3d.ai/")));

    public ApiKeyScreen() {
        super(new TranslatableText("dmodel.api_key"));
    }

    @Override
    protected void init() {
        super.init();
        this.keyField = new PlaceholderTextField(this.textRenderer,
                this.width / 2 - 120, this.height / 2 - 25, 190, 20,
                Text.of(""), new TranslatableText("dmodel.api_key"));
        this.keyField.setText(RodinCraftConfig.API_KEY);
        this.addButton(this.keyField);
        this.addButton(new ButtonWidget(this.width / 2 + 75, this.height / 2 - 25, 50, 20, new TranslatableText("dmodel.confirm"), (button) -> {
            RodinCraftConfig.API_KEY = this.keyField.getText().trim();
            RodinCraftConfig.writeApiKey();
            this.onClose();
        }));
        int textWidth = this.textRenderer.getWidth(GET_API_KEY), textX = (this.width - textWidth) / 2;
        this.addButton(new PressableTextWidget(textX, this.height / 2, textWidth, 10, false, GET_API_KEY,
                button -> Util.getOperatingSystem().open("https://hyper3d.ai/"), this.textRenderer));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
