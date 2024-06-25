package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Custom ProtocolLib Wrapper of NMS' WrittenBookContent (added to NMS in 1.20.6).
 * This is used to store metadata about written books.
 *
 * @since 3.10.0
 */
public class WrappedWrittenBookContent extends AbstractWrapper {
    private static final Class<?> WRITTEN_BOOK_CONTENT = MinecraftReflection.getMinecraftClass("world.item.component.WrittenBookContent");

    private static final FieldAccessor TITLE_FIELD = Accessors.getFieldAccessor(WRITTEN_BOOK_CONTENT, WrappedFilterable.getWrappedClass(), true);
    private static final FieldAccessor AUTHOR_FIELD = Accessors.getFieldAccessor(WRITTEN_BOOK_CONTENT, String.class, true);
    private static final FieldAccessor GENERATION_FIELD = Accessors.getFieldAccessor(WRITTEN_BOOK_CONTENT, int.class, true);
    private static final FieldAccessor PAGES_FIELD = Accessors.getFieldAccessor(WRITTEN_BOOK_CONTENT, List.class, true);
    private static final FieldAccessor RESOLVED_FIELD = Accessors.getFieldAccessor(WRITTEN_BOOK_CONTENT, boolean.class, true);

    private static final ConstructorAccessor CONSTRUCTOR = Accessors.getConstructorAccessor(WRITTEN_BOOK_CONTENT, WrappedFilterable.getWrappedClass(), String.class, int.class, List.class, boolean.class);

    private static final EquivalentConverter<List<WrappedFilterable<WrappedChatComponent>>> FILTERABLE_COMPONENT_LIST_CONVERTER = BukkitConverters.getListConverter(WrappedFilterable.getConverter(BukkitConverters.getWrappedChatComponentConverter()));

    private static final EquivalentConverter<WrappedWrittenBookContent> CONVERTER = Converters.ignoreNull(Converters.handle(AbstractWrapper::getHandle, WrappedWrittenBookContent::fromHandle, WrappedWrittenBookContent.class));

    private WrappedWrittenBookContent(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public @NotNull List<WrappedFilterable<WrappedChatComponent>> getPages() {
        return FILTERABLE_COMPONENT_LIST_CONVERTER.getSpecific(PAGES_FIELD.get(this.getHandle()));
    }

    public void setPages(@NotNull List<WrappedFilterable<WrappedChatComponent>> pages) {
        setHandle(CONSTRUCTOR.invoke(
                TITLE_FIELD.get(getHandle()),
                AUTHOR_FIELD.get(getHandle()),
                GENERATION_FIELD.get(getHandle()),
                FILTERABLE_COMPONENT_LIST_CONVERTER.getGeneric(pages),
                RESOLVED_FIELD.get(getHandle())
        ));
    }

    /**
     * Construct a written book content from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped written book content.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedWrittenBookContent fromHandle(Object handle) {
        return new WrappedWrittenBookContent(handle);
    }

    public static EquivalentConverter<WrappedWrittenBookContent> getConverter() {
        return CONVERTER;
    }

    public static Class<?> getWrappedClass() {
        return WRITTEN_BOOK_CONTENT;
    }
}
