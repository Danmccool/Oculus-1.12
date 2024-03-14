package io.github.singlerr;


import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextFormatting;

public class TextComponentExtension {

    public static <T extends ITextComponent> T withStyle(T component, TextFormatting formatting){
        component.getStyle().setColor(formatting);
        return component;
    }
}
