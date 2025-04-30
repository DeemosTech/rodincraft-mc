package top.gregtao.deemos;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PlaceholderTextField extends TextFieldWidget {
    private final Text placeholder;
    private final TextRenderer textRenderer;

    public PlaceholderTextField(TextRenderer textRenderer, int x, int y, int width, int height, Text text, Text placeholder) {
        super(textRenderer, x, y, width, height, text);
        this.placeholder = placeholder;
        this.textRenderer = textRenderer;
        this.setMaxLength(128);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderButton(matrices, mouseX, mouseY, delta);
        if (this.getText().isEmpty()) {
            this.textRenderer.drawWithShadow(matrices, this.placeholder,
                    this.x + 4, this.y + (float) (this.height - 8) / 2, 0x77AAAAAA);
        }
    }
}
