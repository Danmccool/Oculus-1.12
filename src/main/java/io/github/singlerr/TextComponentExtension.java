package io.github.singlerr;


import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

public class TextComponentExtension {

    public static <T extends ITextComponent> T withStyle(T component, TextFormatting color) {
        if(color == TextFormatting.BOLD){
            component.getStyle().setBold(true);
            return component;
        }
        component.getStyle().setColor(color);
        return component;
    }

    public static <T extends ITextComponent> T withStyle(T component, TextFormatting color, TextFormatting italic) {
        component.getStyle().setColor(color);
        component.getStyle().setItalic(true);
        return component;
    }
}
