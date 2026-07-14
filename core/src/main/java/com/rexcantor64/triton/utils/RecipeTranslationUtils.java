package com.rexcantor64.triton.utils;

import com.github.retrooper.packetevents.protocol.recipe.RecipeDisplayEntry;
import com.github.retrooper.packetevents.protocol.recipe.display.FurnaceRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.RecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.RecipeDisplayTypes;
import com.github.retrooper.packetevents.protocol.recipe.display.ShapedCraftingRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.ShapelessCraftingRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.SmithingRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.StonecutterRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.CompositeSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.DyedSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.ItemStackSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.OnlyWithComponentSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.SlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.SlotDisplayTypes;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.SmithingTrimSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.WithAnyPotionSlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.WithRemainderSlotDisplay;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@NotNullByDefault
public class RecipeTranslationUtils {
    private final ItemStackTranslationUtils itemHandler;

    public RecipeTranslationUtils(MessageParser parser, MainConfig.FeatureSyntax syntax) {
        this.itemHandler = new ItemStackTranslationUtils(parser, syntax, false);
    }

    /**
     * Translate the recipe contains in the given display entry.
     * The given entry is not modified.
     *
     * @param entry  The recipe to translate.
     * @param locale The locale to translate to.
     * @return An empty optional if no placeholders were found in the recipe, otherwise returns the translated entry.
     * @since 4.1.0
     */
    public Optional<RecipeDisplayEntry> translateRecipeDisplayEntry(RecipeDisplayEntry entry, Localized locale) {
        return translateRecipeDisplay(entry.getDisplay(), locale).map(display ->
                new RecipeDisplayEntry(
                        entry.getId(),
                        display,
                        entry.getGroup(),
                        entry.getCategory(),
                        entry.getIngredients()
                )
        );
    }

    /**
     * Translate the recipe contains in the given recipe display.
     * The given entry is not modified.
     *
     * @param display The recipe to translate.
     * @param locale  The locale to translate to.
     * @return An empty optional if no placeholders were found in the recipe, otherwise returns the translated entry.
     * @since 4.1.0
     */
    public Optional<RecipeDisplay<?>> translateRecipeDisplay(RecipeDisplay<?> display, Localized locale) {
        val type = display.getType();
        if (type == RecipeDisplayTypes.CRAFTING_SHAPED) {
            return translateRecipeDisplay((ShapedCraftingRecipeDisplay) display, locale);
        } else if (type == RecipeDisplayTypes.CRAFTING_SHAPELESS) {
            return translateRecipeDisplay((ShapelessCraftingRecipeDisplay) display, locale);
        } else if (type == RecipeDisplayTypes.FURNACE) {
            return translateRecipeDisplay((FurnaceRecipeDisplay) display, locale);
        } else if (type == RecipeDisplayTypes.STONECUTTER) {
            return translateRecipeDisplay((StonecutterRecipeDisplay) display, locale);
        } else if (type == RecipeDisplayTypes.SMITHING) {
            return translateRecipeDisplay((SmithingRecipeDisplay) display, locale);
        }
        return Optional.empty();
    }

    private Optional<RecipeDisplay<?>> translateRecipeDisplay(ShapedCraftingRecipeDisplay display, Localized locale) {
        val ingredients = translateSlotDisplayList(display.getIngredients(), locale);
        val result = translateSlotDisplay(display.getResult(), locale);
        if (ingredients.isPresent() || result.isPresent()) {
            return Optional.of(new ShapedCraftingRecipeDisplay(
                    display.getWidth(),
                    display.getHeight(),
                    ingredients.orElseGet(display::getIngredients),
                    result.orElseGet(display::getResult),
                    display.getCraftingStation()
            ));
        }
        return Optional.empty();
    }

    private Optional<RecipeDisplay<?>> translateRecipeDisplay(ShapelessCraftingRecipeDisplay display, Localized locale) {
        val ingredients = translateSlotDisplayList(display.getIngredients(), locale);
        val result = translateSlotDisplay(display.getResult(), locale);
        if (ingredients.isPresent() || result.isPresent()) {
            return Optional.of(new ShapelessCraftingRecipeDisplay(
                    ingredients.orElseGet(display::getIngredients),
                    result.orElseGet(display::getResult),
                    display.getCraftingStation()
            ));
        }
        return Optional.empty();
    }

    private Optional<RecipeDisplay<?>> translateRecipeDisplay(FurnaceRecipeDisplay display, Localized locale) {
        val ingredient = translateSlotDisplay(display.getIngredient(), locale);
        val fuel = translateSlotDisplay(display.getFuel(), locale);
        val result = translateSlotDisplay(display.getResult(), locale);
        if (ingredient.isPresent() || fuel.isPresent() || result.isPresent()) {
            return Optional.of(new FurnaceRecipeDisplay(
                    ingredient.orElseGet(display::getIngredient),
                    fuel.orElseGet(display::getFuel),
                    result.orElseGet(display::getResult),
                    display.getCraftingStation(),
                    display.getDuration(),
                    display.getExperience()
            ));
        }
        return Optional.empty();
    }

    private Optional<RecipeDisplay<?>> translateRecipeDisplay(StonecutterRecipeDisplay display, Localized locale) {
        val input = translateSlotDisplay(display.getInput(), locale);
        val result = translateSlotDisplay(display.getResult(), locale);
        if (input.isPresent() || result.isPresent()) {
            return Optional.of(new StonecutterRecipeDisplay(
                    input.orElseGet(display::getInput),
                    result.orElseGet(display::getResult),
                    display.getCraftingStation()
            ));
        }
        return Optional.empty();
    }

    private Optional<RecipeDisplay<?>> translateRecipeDisplay(SmithingRecipeDisplay display, Localized locale) {
        val template = translateSlotDisplay(display.getTemplate(), locale);
        val base = translateSlotDisplay(display.getBase(), locale);
        val addition = translateSlotDisplay(display.getAddition(), locale);
        val result = translateSlotDisplay(display.getResult(), locale);
        if (template.isPresent() || base.isPresent() || addition.isPresent() || result.isPresent()) {
            return Optional.of(new SmithingRecipeDisplay(
                    template.orElseGet(display::getTemplate),
                    base.orElseGet(display::getBase),
                    addition.orElseGet(display::getAddition),
                    result.orElseGet(display::getResult),
                    display.getCraftingStation()
            ));
        }
        return Optional.empty();
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(SlotDisplay<?> slot, Localized locale) {
        val type = slot.getType();
        if (type == SlotDisplayTypes.ITEM_STACK) {
            return translateSlotDisplay((ItemStackSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.SMITHING_TRIM) {
            return translateSlotDisplay((SmithingTrimSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.WITH_REMAINDER) {
            return translateSlotDisplay((WithRemainderSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.COMPOSITE) {
            return translateSlotDisplay((CompositeSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.WITH_ANY_POTION) {
            return translateSlotDisplay((WithAnyPotionSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.ONLY_WITH_COMPONENT) {
            return translateSlotDisplay((OnlyWithComponentSlotDisplay) slot, locale);
        } else if (type == SlotDisplayTypes.DYED) {
            return translateSlotDisplay((DyedSlotDisplay) slot, locale);
        }
        return Optional.empty();
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(ItemStackSlotDisplay slot, Localized locale) {
        val result = this.itemHandler.translateItem(slot.getStack(), locale);
        if (result.isModified()) {
            return Optional.of(new ItemStackSlotDisplay(result.getModified()));
        }
        return Optional.empty();
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(SmithingTrimSlotDisplay slot, Localized locale) {
        val base = translateSlotDisplay(slot.getBase(), locale);
        val material = translateSlotDisplay(slot.getMaterial(), locale);
        if (base.isPresent() || material.isPresent()) {
            // noinspection ConstantValue
            if (slot.getTrimPattern() == null) {
                return Optional.of(new SmithingTrimSlotDisplay(
                        base.orElseGet(slot::getBase),
                        material.orElseGet(slot::getMaterial),
                        slot.getPattern()
                ));
            } else {
                return Optional.of(new SmithingTrimSlotDisplay(
                        base.orElseGet(slot::getBase),
                        material.orElseGet(slot::getMaterial),
                        slot.getTrimPattern()
                ));
            }
        }
        return Optional.empty();
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(WithRemainderSlotDisplay slot, Localized locale) {
        val input = translateSlotDisplay(slot.getInput(), locale);
        val remainder = translateSlotDisplay(slot.getRemainder(), locale);
        if (input.isPresent() || remainder.isPresent()) {
            return Optional.of(new WithRemainderSlotDisplay(
                    input.orElseGet(slot::getInput),
                    remainder.orElseGet(slot::getRemainder)
            ));
        }
        return Optional.empty();
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(CompositeSlotDisplay slot, Localized locale) {
        return translateSlotDisplayList(slot.getContents(), locale).map(CompositeSlotDisplay::new);
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(WithAnyPotionSlotDisplay slot, Localized locale) {
        return translateSlotDisplay(slot.getDisplay(), locale).map(WithAnyPotionSlotDisplay::new);
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(OnlyWithComponentSlotDisplay slot, Localized locale) {
        return translateSlotDisplay(slot.getSource(), locale)
                .map(source -> new OnlyWithComponentSlotDisplay(
                        source,
                        slot.getComponent()
                ));
    }

    private Optional<SlotDisplay<?>> translateSlotDisplay(DyedSlotDisplay slot, Localized locale) {
        val source = translateSlotDisplay(slot.getSource(), locale);
        val target = translateSlotDisplay(slot.getTarget(), locale);
        if (source.isPresent() || target.isPresent()) {
            return Optional.of(new DyedSlotDisplay(
                    source.orElseGet(slot::getSource),
                    target.orElseGet(slot::getTarget)
            ));
        }
        return Optional.empty();
    }

    private Optional<List<SlotDisplay<?>>> translateSlotDisplayList(
            List<SlotDisplay<?>> list,
            Localized locale
    ) {
        boolean changed = false;
        val newList = new ArrayList<SlotDisplay<?>>(list.size());
        for (val entry : list) {
            val result = translateSlotDisplay(entry, locale);
            if (result.isPresent()) {
                newList.add(result.get());
                changed = true;
            } else {
                newList.add(entry);
            }
        }
        if (changed) {
            return Optional.of(newList);
        }
        return Optional.empty();
    }

}
