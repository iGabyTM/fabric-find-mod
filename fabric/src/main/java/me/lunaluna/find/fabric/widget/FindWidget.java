package me.lunaluna.find.fabric.widget;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.regex.Pattern;

public class FindWidget extends TextFieldWidget {

    private static Cache<String, Pattern> patternCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .build();
    public static String search = "";

    public FindWidget(int x, int y) {
        super(MinecraftClient.getInstance().textRenderer, x, y, 174, 18, Text.empty());
        setText(search);
        setChangedListener(string -> search = string);
    }

    private boolean matchString(String string) {
        return matchString(string, getText());
    }

    private boolean matchString(String string, String text) {
        text = text.toLowerCase();
        string = string.toLowerCase();

        if (text.startsWith("^")) {
            return string.startsWith(text.substring(1)) || string.trim().startsWith(text.substring(1));
        }

        if (text.startsWith("re:")) {
            try {
                var regex = text.substring("re:".length());
                var cached = patternCache.getIfPresent(regex);

                if (cached != null) {
                    return cached.matcher(text).matches();
                }

                var pattern = Pattern.compile(regex);
                patternCache.put(regex, pattern);
                return pattern.matcher(string).matches();
            } catch (Exception e) {
                return false;
            }
        }

        for (String token : text.split(" "))
            if (!string.contains(token))
                return false;
        return true;
    }

    public boolean matches(@Nullable ItemStack stack) {
        String text = getText();
        if (Strings.isBlank(text))
            return true;
        if (stack == null)
            return false;

        Item item = stack.getItem();
        if (matchString(item.getName().getString()))
            return true;

        ComponentMap components = stack.getComponents();
        if (components == null)
            return false;

        if (components.contains(DataComponentTypes.CUSTOM_NAME)) {
            var customName = components.getOrDefault(DataComponentTypes.CUSTOM_NAME, ScreenTexts.EMPTY).getString();

            // Search only in name
            if (text.startsWith("n:")) {
                return customName != null && matchString(customName, text.substring(2));
            }

            if (customName != null && matchString(customName)) {
                return true;
            }
        }

        if (components.contains(DataComponentTypes.LORE)) {
            var lore = components.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
                .lines()
                .stream()
                .map(Text::getString)
                .map(String::toLowerCase);

            // Search lore only
            if (text.startsWith("l:")) {
                return lore.anyMatch(line -> matchString(line, text.substring(2)));
            }

            /*if (lore.anyMatch(this::matchString)) {
                return true;
            }*/
        }

        if (components.contains(DataComponentTypes.ENCHANTMENTS)) {
            ItemEnchantmentsComponent enchantments = components.get(DataComponentTypes.ENCHANTMENTS);
            for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                String enchantmentName = Enchantment.getName(enchantment, enchantments.getLevel(enchantment))
                        .getString();

                if (matchString(enchantmentName)) {
                    return true;
                }
            }
        }
        if (components.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
            ItemEnchantmentsComponent enchantments = components.get(DataComponentTypes.STORED_ENCHANTMENTS);
            for (RegistryEntry<Enchantment> enchantment : enchantments.getEnchantments()) {
                String enchantmentName = Enchantment.getName(enchantment, enchantments.getLevel(enchantment))
                        .getString();

                if (matchString(enchantmentName)) {
                    return true;
                }
            }
        }
        if (components.contains(DataComponentTypes.POTION_CONTENTS)) {
            PotionContentsComponent potionContents = components.get(DataComponentTypes.POTION_CONTENTS);
            for (StatusEffectInstance effect : potionContents.getEffects()) {
                if (matchString(effect.getEffectType().value().getName().getString()))
                    return true;
            }
        }
        if (components.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponent container = components.get(DataComponentTypes.CONTAINER);
            for (ItemStack containedItem : container.iterateNonEmpty()) {
                if (matches(containedItem))
                    return true;
            }

        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible() || !isVisible())
            return false; // Disables mouse clicking when not visible
        boolean inTextBox = mouseX >= (double) this.getX() && mouseX < (double) (this.getX() + this.width)
                && mouseY >= (double) this.getY() && mouseY < (double) (this.getY() + this.height);
        if (inTextBox && button == GLFW.GLFW_MOUSE_BUTTON_2) {
            setText(""); // Clears text on right click
            return super.mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_1);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext matrices, int mouseX, int mouseY, float delta) {
        if (visible())
            super.render(matrices, mouseX, mouseY, delta);
    }

    public boolean visible() {
        return isFocused() || Strings.isNotBlank(search);
    }
}
