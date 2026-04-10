package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import lombok.val;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom ProtocolLib Wrapper of NMS' ItemContainerContents (added to NMS in 1.20.6).
 * This is used to store the items that are inside a container (e.g., shulker box).
 *
 * @since 3.10.0
 */
public class WrappedItemContainerContents extends AbstractWrapper {
    private static final Class<?> ITEM_CONTAINER_CONTENTS = MinecraftReflection.getMinecraftClass("world.item.component.ItemContainerContents");
    private static final FieldAccessor ITEMS_FIELD;
    private static final boolean IS_OPTIONAL_LIST;
    private static final ConstructorAccessor CONSTRUCTOR = Accessors.getConstructorAccessor(
            FuzzyReflection.fromClass(ITEM_CONTAINER_CONTENTS, true).getConstructor(
                    FuzzyMethodContract.newBuilder()
                            .parameterExactType(List.class)
                            .build()
            )
    );

    private static final EquivalentConverter<ItemStack> ITEM_STACK_CONVERTER = BukkitConverters.getItemStackConverter();
    private static final EquivalentConverter<List<ItemStack>> ITEM_STACK_LIST_CONVERTER = BukkitConverters.getListConverter(ITEM_STACK_CONVERTER);

    private static final EquivalentConverter<ItemStack> ITEM_STACK_TEMPLATE_CONVERTER;
    private static final EquivalentConverter<List<Optional<ItemStack>>> ITEM_STACK_TEMPLATE_LIST_OPTIONAL_CONVERTER;

    private static final EquivalentConverter<WrappedItemContainerContents> CONVERTER = Converters.ignoreNull(Converters.handle(AbstractWrapper::getHandle, WrappedItemContainerContents::fromHandle, WrappedItemContainerContents.class));

    static {
        val itemsField = FuzzyReflection.fromClass(ITEM_CONTAINER_CONTENTS, true).getFieldListByType(MinecraftReflection.getNonNullListClass());
        if (!itemsField.isEmpty()) {
            // up to MC 1.21.11
            ITEMS_FIELD = Accessors.getFieldAccessor(itemsField.get(0));
            IS_OPTIONAL_LIST = false;
            ITEM_STACK_TEMPLATE_CONVERTER = null;
            ITEM_STACK_TEMPLATE_LIST_OPTIONAL_CONVERTER = null;
        } else {
            // MC 26.1+
            ITEMS_FIELD = Accessors.getFieldAccessor(ITEM_CONTAINER_CONTENTS, List.class, true);
            IS_OPTIONAL_LIST = true;
            ITEM_STACK_TEMPLATE_CONVERTER = new EquivalentConverter<ItemStack>() {
                private final Class<?> ITEM_STACK_TEMPLATE_CLASS = MinecraftReflection.getMinecraftClass("world.item.ItemStackTemplate");
                private final MethodAccessor ITEM_TO_TEMPLATE = Accessors.getMethodAccessor(FuzzyReflection.fromClass(ITEM_STACK_TEMPLATE_CLASS).getMethodByReturnTypeAndParameters("fromNonEmptyStack", ITEM_STACK_TEMPLATE_CLASS, MinecraftReflection.getItemStackClass()));
                private final MethodAccessor TEMPLATE_TO_ITEM = Accessors.getMethodAccessor(FuzzyReflection.fromClass(ITEM_STACK_TEMPLATE_CLASS).getMethodByReturnTypeAndParameters("create", MinecraftReflection.getItemStackClass()));

                @Override
                public Object getGeneric(ItemStack specific) {
                    Object nmsItem = ITEM_STACK_CONVERTER.getGeneric(specific);
                    return ITEM_TO_TEMPLATE.invoke(null, nmsItem);
                }

                @Override
                public ItemStack getSpecific(Object generic) {
                    Object nmsItem = TEMPLATE_TO_ITEM.invoke(generic);
                    return ITEM_STACK_CONVERTER.getSpecific(nmsItem);
                }

                @Override
                public Class<ItemStack> getSpecificType() {
                    return ItemStack.class;
                }
            };
            ITEM_STACK_TEMPLATE_LIST_OPTIONAL_CONVERTER = BukkitConverters.getListConverter(Converters.optional(ITEM_STACK_TEMPLATE_CONVERTER));
        }
    }

    private WrappedItemContainerContents(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public @NotNull Stream<@Nullable ItemStack> getItems() {
        if (IS_OPTIONAL_LIST) {
            return ITEM_STACK_TEMPLATE_LIST_OPTIONAL_CONVERTER.getSpecific(ITEMS_FIELD.get(this.getHandle()))
                    .stream()
                    .map(item -> item.orElse(null));
        }
        return ITEM_STACK_LIST_CONVERTER.getSpecific(ITEMS_FIELD.get(this.getHandle())).stream();
    }

    public void setItems(@NotNull List<@Nullable ItemStack> items) {
        if (IS_OPTIONAL_LIST) {
            this.handle = CONSTRUCTOR.invoke(ITEM_STACK_TEMPLATE_LIST_OPTIONAL_CONVERTER.getGeneric(items.stream().map(Optional::ofNullable).collect(Collectors.toList())));
        } else {
            this.handle = CONSTRUCTOR.invoke(ITEM_STACK_LIST_CONVERTER.getGeneric(items));
        }
    }

    /**
     * Construct item container contents from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped item container contents.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedItemContainerContents fromHandle(Object handle) {
        return new WrappedItemContainerContents(handle);
    }

    public static EquivalentConverter<WrappedItemContainerContents> getConverter() {
        return CONVERTER;
    }

    public static Class<?> getWrappedClass() {
        return ITEM_CONTAINER_CONTENTS;
    }
}
