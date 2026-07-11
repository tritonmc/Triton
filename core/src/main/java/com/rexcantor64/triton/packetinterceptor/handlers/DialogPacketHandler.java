package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.ConfirmationDialog;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogListDialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogTypes;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog;
import com.github.retrooper.packetevents.protocol.dialog.ServerLinksDialog;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBodyTypes;
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.BooleanInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.InputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.InputControlTypes;
import com.github.retrooper.packetevents.protocol.dialog.input.NumberRangeInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.SingleOptionInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerShowDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.language.parser.TranslationResult;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.utils.ItemStackTranslationUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

@RequiredArgsConstructor
public class DialogPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;
    private final @NotNull ItemStackTranslationUtils itemHandler;

    final ActionButton CONFIRMATION_DIALOG_DEFAULT_BUTTON = new ActionButton(
            new CommonButtonData(Component.empty(), null, 150),
            null
    );

    public DialogPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getDialogsSyntax();
        this.itemHandler = new ItemStackTranslationUtils(this.parser, config.getItemsSyntax(), config.isBooks());
    }

    public void onConfigShowDialogPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperConfigServerShowDialog(event);
        translateDialog(packet.getDialog(), languagePlayer).ifPresent(dialog -> {
            packet.setDialog(dialog);
            event.markForReEncode(true);
        });
    }

    public void onPlayShowDialogPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerShowDialog(event);
        translateDialog(packet.getDialog(), languagePlayer).ifPresent(dialog -> {
            packet.setDialog(dialog);
            event.markForReEncode(true);
        });
    }

    @VisibleForTesting
    @NotNull Optional<Dialog> translateDialog(@NotNull Dialog dialog, @NotNull Localized locale) {
        val type = dialog.getType();
        if (type.equals(DialogTypes.MULTI_ACTION)) {
            return translateDialog((MultiActionDialog) dialog, locale);
        } else if (type.equals(DialogTypes.DIALOG_LIST)) {
            return translateDialog((DialogListDialog) dialog, locale);
        } else if (type.equals(DialogTypes.CONFIRMATION)) {
            return translateDialog((ConfirmationDialog) dialog, locale);
        } else if (type.equals(DialogTypes.NOTICE)) {
            return translateDialog((NoticeDialog) dialog, locale);
        } else if (type.equals(DialogTypes.SERVER_LINKS)) {
            return translateDialog((ServerLinksDialog) dialog, locale);

        }
        return Optional.empty();
    }

    private @NotNull Optional<Dialog> translateDialog(@NotNull MultiActionDialog dialog, @NotNull Localized locale) {
        val commonData = translateCommonDialogData(dialog.getCommon(), locale);
        val actions = translateButtonList(dialog.getActions(), locale);
        val exitActionResult = dialog.getExitAction() == null ? TranslationResult.<ActionButton>unchanged() : translateButton(dialog.getExitAction(), locale);

        if (!exitActionResult.isUnchanged() || actions.isPresent() || commonData.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new MultiActionDialog(
                    commonData.orElseGet(dialog::getCommon),
                    actions.orElseGet(dialog::getActions),
                    exitActionResult.mapToObj(Function.identity(), dialog::getExitAction, () -> null),
                    dialog.getColumns()
            ));
        }
        return Optional.empty();
    }

    private @NotNull Optional<Dialog> translateDialog(@NotNull DialogListDialog dialog, @NotNull Localized locale) {
        val commonData = translateCommonDialogData(dialog.getCommon(), locale);
        val exitActionResult = dialog.getExitAction() == null ? TranslationResult.<ActionButton>unchanged() : translateButton(dialog.getExitAction(), locale);

        if (!exitActionResult.isUnchanged() || commonData.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new DialogListDialog(
                    commonData.orElseGet(dialog::getCommon),
                    dialog.getDialogs(), // TODO translate inner dialogs?
                    exitActionResult.mapToObj(Function.identity(), dialog::getExitAction, () -> null),
                    dialog.getColumns(),
                    dialog.getButtonWidth()
            ));
        }
        return Optional.empty();
    }

    private @NotNull Optional<Dialog> translateDialog(@NotNull ConfirmationDialog dialog, @NotNull Localized locale) {
        val commonData = translateCommonDialogData(dialog.getCommon(), locale);
        val yesResult = translateButton(dialog.getYesButton(), locale);
        val noResult = translateButton(dialog.getNoButton(), locale);

        if (!yesResult.isUnchanged() || !noResult.isUnchanged() || commonData.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new ConfirmationDialog(
                    commonData.orElseGet(dialog::getCommon),
                    yesResult.mapToObj(Function.identity(), dialog::getYesButton, () -> CONFIRMATION_DIALOG_DEFAULT_BUTTON),
                    noResult.mapToObj(Function.identity(), dialog::getNoButton, () -> CONFIRMATION_DIALOG_DEFAULT_BUTTON)
            ));
        }
        return Optional.empty();
    }

    private @NotNull Optional<Dialog> translateDialog(@NotNull NoticeDialog dialog, @NotNull Localized locale) {
        val commonData = translateCommonDialogData(dialog.getCommon(), locale);
        val buttonResult = translateButton(dialog.getAction(), locale);

        if (!buttonResult.isUnchanged() || commonData.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new NoticeDialog(
                    commonData.orElseGet(dialog::getCommon),
                    buttonResult.mapToObj(Function.identity(), dialog::getAction, () -> NoticeDialog.DEFAULT_ACTION)
            ));
        }
        return Optional.empty();
    }

    private @NotNull Optional<Dialog> translateDialog(@NotNull ServerLinksDialog dialog, @NotNull Localized locale) {
        val commonData = translateCommonDialogData(dialog.getCommon(), locale);
        val exitActionResult = dialog.getExitAction() == null ? TranslationResult.<ActionButton>unchanged() : translateButton(dialog.getExitAction(), locale);

        if (!exitActionResult.isUnchanged() || commonData.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new ServerLinksDialog(
                    commonData.orElseGet(dialog::getCommon),
                    exitActionResult.mapToObj(Function.identity(), dialog::getExitAction, () -> null),
                    dialog.getColumns(),
                    dialog.getButtonWidth()
            ));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    @NotNull Optional<@NotNull CommonDialogData> translateCommonDialogData(@NotNull CommonDialogData data, @NotNull Localized locale) {
        val title = parser.translateComponent(data.getTitle(), locale, this.syntax);
        val externalTitle = data.getExternalTitle() == null ?
                TranslationResult.<Component>unchanged() :
                parser.translateComponent(data.getExternalTitle(), locale, this.syntax);

        val body = translateDialogBodyList(data.getBody(), locale);
        val inputs = translateInputList(data.getInputs(), locale);

        if (!title.isUnchanged() || !externalTitle.isUnchanged() || body.isPresent() || inputs.isPresent()) {
            // noinspection NullableProblems
            return Optional.of(new CommonDialogData(
                    title.mapToObj(Function.identity(), data::getTitle, Component::empty),
                    externalTitle.mapToObj(Function.identity(), data::getExternalTitle, () -> null),
                    data.isCanCloseWithEscape(),
                    data.isPause(),
                    data.getAfterAction(),
                    body.orElseGet(data::getBody),
                    inputs.orElseGet(data::getInputs)
            ));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    @NotNull TranslationResult<@NotNull DialogBody> translateDialogBody(@NotNull DialogBody body, @NotNull Localized locale) {
        val type = body.getType();
        if (type.equals(DialogBodyTypes.PLAIN_MESSAGE)) {
            return translateDialogBody((PlainMessageDialogBody) body, locale);
        } else if (type.equals(DialogBodyTypes.ITEM)) {
            return translateDialogBody((ItemDialogBody) body, locale);
        }
        return TranslationResult.unchanged();
    }

    private @NotNull TranslationResult<@NotNull DialogBody> translateDialogBody(@NotNull PlainMessageDialogBody body, @NotNull Localized locale) {
        return parser.translateComponent(body.getMessage().getContents(), locale, this.syntax)
                .map(newText -> new PlainMessageDialogBody(new PlainMessage(newText, body.getMessage().getWidth())));
    }

    private @NotNull TranslationResult<@NotNull DialogBody> translateDialogBody(@NotNull ItemDialogBody body, @NotNull Localized locale) {
        val item = this.itemHandler.translateItem(body.getItem(), locale);
        val description = body.getDescription() == null ?
                TranslationResult.<PlainMessage>unchanged() :
                this.parser.translateComponent(body.getDescription().getContents(), locale, this.syntax)
                        .map(component -> new PlainMessage(component, body.getDescription().getWidth()));

        if (item.isModified() || !description.isUnchanged()) {
            // noinspection NullableProblems
            return TranslationResult.changed(
                    new ItemDialogBody(
                            item.isModified() ? item.getModified() : body.getItem(),
                            description.mapToObj(Function.identity(), body::getDescription, () -> null),
                            body.isShowDecorations(),
                            body.isShowTooltip(),
                            body.getWidth(),
                            body.getHeight()
                    )
            );
        }
        return TranslationResult.unchanged();
    }

    @VisibleForTesting
    @NotNull TranslationResult<@NotNull Input> translateInput(@NotNull Input input, @NotNull Localized locale) {
        return translateInputControl(input.getControl(), locale).map(control -> new Input(input.getKey(), control));
    }

    private @NotNull TranslationResult<@NotNull InputControl> translateInputControl(@NotNull InputControl input, @NotNull Localized locale) {
        val type = input.getType();
        if (type.equals(InputControlTypes.TEXT)) {
            return translateInputControl((TextInputControl) input, locale);
        } else if (type.equals(InputControlTypes.BOOLEAN)) {
            return translateInputControl((BooleanInputControl) input, locale);
        } else if (type.equals(InputControlTypes.NUMBER_RANGE)) {
            return translateInputControl((NumberRangeInputControl) input, locale);
        } else if (type.equals(InputControlTypes.SINGLE_OPTION)) {
            return translateInputControl((SingleOptionInputControl) input, locale);
        }
        return TranslationResult.unchanged();
    }

    private @NotNull TranslationResult<@NotNull InputControl> translateInputControl(@NotNull TextInputControl input, @NotNull Localized locale) {
        return parser.translateComponent(input.getLabel(), locale, this.syntax)
                .map(label -> new TextInputControl(
                        input.getWidth(),
                        label,
                        input.isLabelVisible(),
                        input.getInitial(),
                        input.getMaxLength(),
                        input.getMultiline()
                ));
    }

    private @NotNull TranslationResult<@NotNull InputControl> translateInputControl(@NotNull BooleanInputControl input, @NotNull Localized locale) {
        return parser.translateComponent(input.getLabel(), locale, this.syntax)
                .map(label -> new BooleanInputControl(
                        label,
                        input.isInitial(),
                        input.getOnTrue(),
                        input.getOnFalse()
                ));
    }

    private @NotNull TranslationResult<@NotNull InputControl> translateInputControl(@NotNull NumberRangeInputControl input, @NotNull Localized locale) {
        return parser.translateComponent(input.getLabel(), locale, this.syntax)
                .map(label -> new NumberRangeInputControl(
                        input.getWidth(),
                        label,
                        input.getLabelFormat(),
                        input.getRangeInfo()
                ));
    }

    private @NotNull TranslationResult<@NotNull InputControl> translateInputControl(@NotNull SingleOptionInputControl input, @NotNull Localized locale) {
        val labelResult = parser.translateComponent(input.getLabel(), locale, this.syntax);
        if (labelResult.isToRemove()) {
            return TranslationResult.remove();
        }

        val entries = translateSingleOptionEntryList(input.getOptions(), locale);

        if (labelResult.isChanged() || entries.isPresent()) {
            return TranslationResult.changed(new SingleOptionInputControl(
                    input.getWidth(),
                    entries.orElseGet(input::getOptions),
                    labelResult.getResult().orElseGet(input::getLabel),
                    input.isLabelVisible()
            ));
        }
        return TranslationResult.unchanged();
    }

    @VisibleForTesting
    @NotNull TranslationResult<SingleOptionInputControl.@NotNull Entry> translateSingleOptionEntry(
            @NotNull SingleOptionInputControl.Entry entry,
            @NotNull Localized locale
    ) {
        if (entry.getDisplay() == null) {
            return TranslationResult.unchanged();
        }
        return parser.translateComponent(entry.getDisplay(), locale, this.syntax)
                .map(display -> new SingleOptionInputControl.Entry(
                        entry.getId(),
                        display,
                        entry.isInitial()
                ));
    }

    private <T> @NotNull Optional<@NotNull List<@NotNull T>> translateList(
            @NotNull List<@NotNull T> list,
            @NotNull BiFunction<T, Localized, TranslationResult<T>> translateFn,
            @NotNull Localized locale
    ) {
        val changed = new AtomicBoolean(false);
        val newList = new ArrayList<T>(list.size());
        for (T entry : list) {
            translateFn.apply(entry, locale)
                    .ifChangedOrToRemove(() -> changed.set(true))
                    .ifUnchanged(() -> newList.add(entry))
                    .ifChanged(newList::add);
        }
        if (changed.get()) {
            return Optional.of(newList);
        }
        return Optional.empty();
    }

    @VisibleForTesting
    @NotNull Optional<@NotNull List<@NotNull DialogBody>> translateDialogBodyList(
            @NotNull List<@NotNull DialogBody> bodyList,
            @NotNull Localized locale
    ) {
        return translateList(bodyList, this::translateDialogBody, locale);
    }

    @VisibleForTesting
    @NotNull Optional<@NotNull List<@NotNull Input>> translateInputList(
            @NotNull List<@NotNull Input> inputList,
            @NotNull Localized locale
    ) {
        return translateList(inputList, this::translateInput, locale);
    }

    @VisibleForTesting
    @NotNull Optional<@NotNull List<SingleOptionInputControl.@NotNull Entry>> translateSingleOptionEntryList(
            @NotNull List<SingleOptionInputControl.@NotNull Entry> entryList,
            @NotNull Localized locale
    ) {
        return translateList(entryList, this::translateSingleOptionEntry, locale);
    }

    @VisibleForTesting
    @NotNull Optional<@NotNull List<@NotNull ActionButton>> translateButtonList(
            @NotNull List<@NotNull ActionButton> buttons,
            @NotNull Localized locale
    ) {
        return translateList(buttons, this::translateButton, locale);
    }

    @VisibleForTesting
    @NotNull TranslationResult<ActionButton> translateButton(@NotNull ActionButton button, @NotNull Localized locale) {
        return translateButton(button.getButton(), locale).map(newButton -> new ActionButton(newButton, button.getAction()));
    }

    private @NotNull TranslationResult<CommonButtonData> translateButton(@NotNull CommonButtonData button, @NotNull Localized locale) {
        val labelResult = parser.translateComponent(button.getLabel(), locale, this.syntax);
        val tooltipResult = button.getTooltip() == null ? TranslationResult.<Component>unchanged() : parser.translateComponent(button.getTooltip(), locale, this.syntax);

        if (labelResult.isUnchanged() && tooltipResult.isUnchanged()) {
            return TranslationResult.unchanged();
        }
        if (labelResult.isToRemove()) {
            // if label is to remove, remove the entire button
            return TranslationResult.remove();
        }

        // noinspection NullableProblems
        return TranslationResult.changed(
                new CommonButtonData(
                        labelResult.getResult().orElse(button.getLabel()),
                        tooltipResult.mapToObj(Function.identity(), button::getTooltip, () -> null),
                        button.getWidth()
                )
        );
    }
}
