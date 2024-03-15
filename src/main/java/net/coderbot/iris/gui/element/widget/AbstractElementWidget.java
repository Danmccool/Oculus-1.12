package net.coderbot.iris.gui.element.widget;

import net.coderbot.iris.gui.NavigationController;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.coderbot.iris.shaderpack.option.menu.OptionMenuElement;

public abstract class AbstractElementWidget<T extends OptionMenuElement> {
    public static final AbstractElementWidget<OptionMenuElement> EMPTY = new AbstractElementWidget<OptionMenuElement>(null) {
        @Override
        public void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        }
    };
    protected final T element;

    public AbstractElementWidget(T element) {
        this.element = element;
    }

    public void init(ShaderPackScreen screen, NavigationController navigation) {
    }

    public abstract void render(int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }
}
