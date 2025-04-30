package top.gregtao.deemos;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.MathHelper;

public class PressableTextWidget extends ButtonWidget {
    private final TextRenderer textRenderer;
    private final Text text;
    private final Text hoverText;

    public PressableTextWidget(int x, int y, int width, int height, boolean underline, Text text, PressAction onPress, TextRenderer textRenderer) {
        super(x, y, width, height, text, onPress);
        this.textRenderer = textRenderer;
        this.text = text;
        this.hoverText = underline ? Texts.setStyleIfAbsent(text.shallowCopy(), Style.EMPTY.withUnderline(true)) : this.text;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text text = this.isHovered() ? this.hoverText : this.text;
        drawTextWithShadow(matrices, this.textRenderer, text, this.x, this.y, 16777215 | MathHelper.ceil(this.alpha * 255.0F) << 24);
    }
}

