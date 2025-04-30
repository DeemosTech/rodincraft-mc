package top.gregtao.deemos;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class BaseScreen extends Screen {
    public static final Text POWERED_BY = new LiteralText("Powered by the best 3D GenAI ")
            .append(new LiteralText("Hyper3D.ai"));

    public BaseScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        int poweredWidth = this.textRenderer.getWidth(POWERED_BY);
        this.addButton(new PressableTextWidget(this.width - 4 - poweredWidth, this.height - 12, poweredWidth, 10,
                true, POWERED_BY, widget -> Util.getOperatingSystem().open("https://hyper3d.ai/"), this.textRenderer));
    }
}
