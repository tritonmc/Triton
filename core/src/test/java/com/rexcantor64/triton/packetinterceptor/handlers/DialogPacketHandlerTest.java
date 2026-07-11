package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.ConfirmationDialog;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.DialogListDialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogTypes;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog;
import com.github.retrooper.packetevents.protocol.dialog.ServerLinksDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.Action;
import com.github.retrooper.packetevents.protocol.dialog.action.StaticAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBodyTypes;
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.BooleanInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.dialog.input.InputControlTypes;
import com.github.retrooper.packetevents.protocol.dialog.input.NumberRangeInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.SingleOptionInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntityRefSet;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.language.parser.TranslationResult;
import com.rexcantor64.triton.test.DefaultFeatureSyntax;
import com.rexcantor64.triton.test.MockAdventureParser;
import com.rexcantor64.triton.test.MockPacketEventsAPI;
import com.rexcantor64.triton.utils.ItemStackTranslationUtils;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DialogPacketHandlerTest {

    private final AdventureParser parser = new MockAdventureParser();
    private final DialogPacketHandler handler = new DialogPacketHandler(
            parser,
            new DefaultFeatureSyntax(),
            new ItemStackTranslationUtils(parser, new DefaultFeatureSyntax(), true)
    );
    private final Localized localized = () -> {
        throw new RuntimeException("unavailable during testing");
    };

    private final Action ACTION = new StaticAction(com.github.retrooper.packetevents.protocol.chat.clickevent.ClickEvent.fromAdventure(ClickEvent.runCommand("help")));
    private final ActionButton ACTION_BUTTON_CHANGED = new ActionButton(new CommonButtonData(Component.text("[lang]action.button.label[/lang]"), null, 10), ACTION);
    private final ActionButton ACTION_BUTTON_TO_REMOVE = new ActionButton(new CommonButtonData(Component.text("[lang]disabled.line[/lang]"), null, 10), ACTION);
    private final ActionButton ACTION_BUTTON_UNCHANGED = new ActionButton(new CommonButtonData(Component.text("lorem ipsum"), null, 10), ACTION);

    private final SingleOptionInputControl.Entry SINGLE_OPTION_ENTRY_CHANGED = new SingleOptionInputControl.Entry("a", Component.text("[lang]entry.display[/lang]"), true);
    private final SingleOptionInputControl.Entry SINGLE_OPTION_ENTRY_TO_REMOVE = new SingleOptionInputControl.Entry("b", Component.text("[lang]disabled.line[/lang]"), false);
    private final SingleOptionInputControl.Entry SINGLE_OPTION_ENTRY_UNCHANGED = new SingleOptionInputControl.Entry("c", Component.text("lorem ipsum"), false);

    private final TextInputControl.MultilineOptions MULTILINE_OPTIONS = new TextInputControl.MultilineOptions(50, 5);
    private final Input TEXT_INPUT_CHANGED = new Input("text-a", new TextInputControl(10, Component.text("[lang]input.text.label[/lang]"), true, "initial", 20, MULTILINE_OPTIONS));
    private final Input TEXT_INPUT_TO_REMOVE = new Input("text-b", new TextInputControl(10, Component.text("[lang]disabled.line[/lang]"), true, "initial", 20, MULTILINE_OPTIONS));
    private final Input TEXT_INPUT_UNCHANGED = new Input("text-c", new TextInputControl(10, Component.text("lorem ipsum"), true, "initial", 20, MULTILINE_OPTIONS));
    private final Input BOOLEAN_INPUT_CHANGED = new Input("boolean-a", new BooleanInputControl(Component.text("[lang]input.boolean.label[/lang]"), true, "true-a", "false-a"));
    private final Input BOOLEAN_INPUT_TO_REMOVE = new Input("boolean-b", new BooleanInputControl(Component.text("[lang]disabled.line[/lang]"), true, "true-b", "false-b"));
    private final Input BOOLEAN_INPUT_UNCHANGED = new Input("boolean-c", new BooleanInputControl(Component.text("lorem ipsum"), true, "true-c", "false-b"));
    private final NumberRangeInputControl.RangeInfo RANGE_INFO = new NumberRangeInputControl.RangeInfo(1, 10, 4F, 2F);
    private final Input NUMBER_RANGE_INPUT_CHANGED = new Input("numrange-a", new NumberRangeInputControl(10, Component.text("[lang]input.numrange.label[/lang]"), "format", RANGE_INFO));
    private final Input NUMBER_RANGE_INPUT_TO_REMOVE = new Input("numrange-b", new NumberRangeInputControl(10, Component.text("[lang]disabled.line[/lang]"), "format", RANGE_INFO));
    private final Input NUMBER_RANGE_INPUT_UNCHANGED = new Input("numrange-c", new NumberRangeInputControl(10, Component.text("lorem ipsum"), "format", RANGE_INFO));
    private final Input SINGLE_OPTION_INPUT_CHANGED = new Input("singleopt-a", new SingleOptionInputControl(10, Collections.emptyList(), Component.text("[lang]input.singleopt.label[/lang]"), true));
    private final Input SINGLE_OPTION_INPUT_TO_REMOVE = new Input("singleopt-b", new SingleOptionInputControl(10, Collections.emptyList(), Component.text("[lang]disabled.line[/lang]"), true));
    private final Input SINGLE_OPTION_INPUT_UNCHANGED = new Input("singleopt-c", new SingleOptionInputControl(10, Collections.emptyList(), Component.text("lorem ipsum"), true));

    private final DialogBody TEXT_BODY_CHANGED = new PlainMessageDialogBody(new PlainMessage(Component.text("[lang]body.text[/lang]"), 10));
    private final DialogBody TEXT_BODY_TO_REMOVE = new PlainMessageDialogBody(new PlainMessage(Component.text("[lang]disabled.line[/lang]"), 10));
    private final DialogBody TEXT_BODY_UNCHANGED = new PlainMessageDialogBody(new PlainMessage(Component.text("lorem ipsum"), 10));

    private final ItemStack ITEM_CHANGED = new ItemStack.Builder().type(ItemTypes.STONE).component(ComponentTypes.CUSTOM_NAME, Component.text("[lang]item.custom.name[/lang]")).build();
    private final ItemStack ITEM_UNCHANGED = new ItemStack.Builder().type(ItemTypes.STONE).component(ComponentTypes.CUSTOM_NAME, Component.text("lorem ipsum")).build();

    private final DialogBody ITEM_BODY_ITEM_CHANGED = new ItemDialogBody(ITEM_CHANGED, null, false, true, 10, 20);
    private final DialogBody ITEM_BODY_DESCRIPTION_CHANGED = new ItemDialogBody(ITEM_UNCHANGED, new PlainMessage(Component.text("[lang]body.item.description[/lang]"), 10), false, true, 10, 20);
    private final DialogBody ITEM_BODY_DESCRIPTION_TO_REMOVE = new ItemDialogBody(ITEM_UNCHANGED, new PlainMessage(Component.text("[lang]disabled.line[/lang]"), 10), false, true, 10, 20);
    private final DialogBody ITEM_BODY_UNCHANGED = new ItemDialogBody(ITEM_UNCHANGED, null, false, true, 10, 20);

    private final List<DialogBody> BODY_LIST_CHANGED = List.of(TEXT_BODY_TO_REMOVE);
    private final List<DialogBody> BODY_LIST_UNCHANGED = List.of(TEXT_BODY_UNCHANGED);
    private final List<Input> INPUT_LIST_CHANGED = List.of(TEXT_INPUT_TO_REMOVE);
    private final List<Input> INPUT_LIST_UNCHANGED = List.of(TEXT_INPUT_UNCHANGED);
    private final CommonDialogData DATA_TITLE_CHANGED = new CommonDialogData(Component.text("[lang]data.title[/lang]"), null, true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_UNCHANGED);
    private final CommonDialogData DATA_TITLE_TO_REMOVE = new CommonDialogData(Component.text("[lang]disabled.line[/lang]"), null, true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_UNCHANGED);
    private final CommonDialogData DATA_EXTERNAL_TITLE_CHANGED = new CommonDialogData(Component.text("lorem ipsum"), Component.text("[lang]data.external.title[/lang]"), true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_UNCHANGED);
    private final CommonDialogData DATA_EXTERNAL_TITLE_TO_REMOVE = new CommonDialogData(Component.text("lorem ipsum"), Component.text("[lang]disabled.line[/lang]"), true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_UNCHANGED);
    private final CommonDialogData DATA_BODY_CHANGED = new CommonDialogData(Component.text("lorem ipsum"), Component.text("lorem ipsum"), true, false, DialogAction.CLOSE, BODY_LIST_CHANGED, INPUT_LIST_UNCHANGED);
    private final CommonDialogData DATA_INPUTS_CHANGED = new CommonDialogData(Component.text("lorem ipsum"), null, true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_CHANGED);
    private final CommonDialogData DATA_UNCHANGED = new CommonDialogData(Component.text("lorem ipsum"), null, true, false, DialogAction.CLOSE, BODY_LIST_UNCHANGED, INPUT_LIST_UNCHANGED);

    private final List<ActionButton> ACTION_BUTTON_LIST_CHANGED = List.of(ACTION_BUTTON_TO_REMOVE);
    private final List<ActionButton> ACTION_BUTTON_LIST_UNCHANGED = List.of(ACTION_BUTTON_UNCHANGED);
    private final Dialog MULTI_ACTION_DIALOG_DATA_CHANGED = new MultiActionDialog(DATA_TITLE_CHANGED, ACTION_BUTTON_LIST_UNCHANGED, null, 10);
    private final Dialog MULTI_ACTION_DIALOG_ACTIONS_CHANGED = new MultiActionDialog(DATA_UNCHANGED, ACTION_BUTTON_LIST_CHANGED, ACTION_BUTTON_UNCHANGED, 10);
    private final Dialog MULTI_ACTION_DIALOG_EXIT_CHANGED = new MultiActionDialog(DATA_UNCHANGED, ACTION_BUTTON_LIST_UNCHANGED, ACTION_BUTTON_CHANGED, 10);
    private final Dialog MULTI_ACTION_DIALOG_EXIT_TO_REMOVE = new MultiActionDialog(DATA_UNCHANGED, ACTION_BUTTON_LIST_UNCHANGED, ACTION_BUTTON_TO_REMOVE, 10);
    private final Dialog MULTI_ACTION_DIALOG_UNCHANGED = new MultiActionDialog(DATA_UNCHANGED, ACTION_BUTTON_LIST_UNCHANGED, ACTION_BUTTON_UNCHANGED, 10);

    private final MappedEntityRefSet<Dialog> MAPPED_ENTITY_REF = new MappedEntitySet<>(Collections.emptyList());
    private final Dialog DIALOG_LIST_DIALOG_DATA_CHANGED = new DialogListDialog(DATA_TITLE_CHANGED, MAPPED_ENTITY_REF, null, 10, 20);
    private final Dialog DIALOG_LIST_DIALOG_EXIT_CHANGED = new DialogListDialog(DATA_UNCHANGED, MAPPED_ENTITY_REF, ACTION_BUTTON_CHANGED, 10, 20);
    private final Dialog DIALOG_LIST_DIALOG_EXIT_TO_REMOVE = new DialogListDialog(DATA_UNCHANGED, MAPPED_ENTITY_REF, ACTION_BUTTON_TO_REMOVE, 10, 20);
    private final Dialog DIALOG_LIST_DIALOG_UNCHANGED = new DialogListDialog(DATA_UNCHANGED, MAPPED_ENTITY_REF, ACTION_BUTTON_UNCHANGED, 10, 20);

    private final Dialog CONFIRMATION_DIALOG_DATA_CHANGED = new ConfirmationDialog(DATA_TITLE_CHANGED, ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_UNCHANGED);
    private final Dialog CONFIRMATION_DIALOG_YES_CHANGED = new ConfirmationDialog(DATA_UNCHANGED, ACTION_BUTTON_CHANGED, ACTION_BUTTON_UNCHANGED);
    private final Dialog CONFIRMATION_DIALOG_YES_TO_REMOVE = new ConfirmationDialog(DATA_UNCHANGED, ACTION_BUTTON_TO_REMOVE, ACTION_BUTTON_UNCHANGED);
    private final Dialog CONFIRMATION_DIALOG_NO_CHANGED = new ConfirmationDialog(DATA_UNCHANGED, ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_CHANGED);
    private final Dialog CONFIRMATION_DIALOG_NO_TO_REMOVE = new ConfirmationDialog(DATA_UNCHANGED, ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_TO_REMOVE);
    private final Dialog CONFIRMATION_DIALOG_UNCHANGED = new ConfirmationDialog(DATA_UNCHANGED, ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_UNCHANGED);

    private final Dialog NOTICE_DIALOG_DATA_CHANGED = new NoticeDialog(DATA_TITLE_CHANGED, ACTION_BUTTON_UNCHANGED);
    private final Dialog NOTICE_DIALOG_ACTION_CHANGED = new NoticeDialog(DATA_UNCHANGED, ACTION_BUTTON_CHANGED);
    private final Dialog NOTICE_DIALOG_ACTION_TO_REMOVE = new NoticeDialog(DATA_UNCHANGED, ACTION_BUTTON_TO_REMOVE);
    private final Dialog NOTICE_DIALOG_UNCHANGED = new NoticeDialog(DATA_UNCHANGED, ACTION_BUTTON_UNCHANGED);

    private final Dialog SERVER_LINKS_DIALOG_DATA_CHANGED = new ServerLinksDialog(DATA_TITLE_CHANGED, ACTION_BUTTON_UNCHANGED, 10, 20);
    private final Dialog SERVER_LINKS_DIALOG_ACTION_CHANGED = new ServerLinksDialog(DATA_UNCHANGED, ACTION_BUTTON_CHANGED, 10, 20);
    private final Dialog SERVER_LINKS_DIALOG_ACTION_TO_REMOVE = new ServerLinksDialog(DATA_UNCHANGED, ACTION_BUTTON_TO_REMOVE, 10, 20);
    private final Dialog SERVER_LINKS_DIALOG_UNCHANGED = new ServerLinksDialog(DATA_UNCHANGED, ACTION_BUTTON_UNCHANGED, 10, 20);

    @BeforeAll
    public static void setupPacketEvents() {
        PacketEvents.setAPI(new MockPacketEventsAPI());
    }

    @Test
    public void testActionButtonChanged() {
        TranslationResult<ActionButton> result = handler.translateButton(ACTION_BUTTON_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val button = result.getResultRaw();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testActionButtonToRemove() {
        TranslationResult<ActionButton> result = handler.translateButton(ACTION_BUTTON_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testActionButtonUnchanged() {
        TranslationResult<ActionButton> result = handler.translateButton(ACTION_BUTTON_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testActionButtonChangedWithTooltip() {
        val originalButton = new ActionButton(new CommonButtonData(Component.text("[lang]action.button.label[/lang]"), Component.text("[lang]action.button.tooltip[/lang]"), 10), ACTION);
        TranslationResult<ActionButton> result = handler.translateButton(originalButton, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val button = result.getResultRaw();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertEquals(Component.text("replaced(action.button.tooltip)"), button.getButton().getTooltip().compact());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testActionButtonUnchangedWithTooltip() {
        val originalButton = new ActionButton(new CommonButtonData(Component.text("lorem ipsum"), Component.text("[lang]action.button.tooltip[/lang]"), 10), ACTION);
        TranslationResult<ActionButton> result = handler.translateButton(originalButton, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val button = result.getResultRaw();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("lorem ipsum"), button.getButton().getLabel().compact());
        assertEquals(Component.text("replaced(action.button.tooltip)"), button.getButton().getTooltip().compact());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testActionButtonUnchangedWithToRemoveTooltip() {
        val originalButton = new ActionButton(new CommonButtonData(Component.text("lorem ipsum"), Component.text("[lang]disabled.line[/lang]"), 10), ACTION);
        TranslationResult<ActionButton> result = handler.translateButton(originalButton, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val button = result.getResultRaw();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("lorem ipsum"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testActionButtonListChanged() {
        val originalList = Arrays.asList(ACTION_BUTTON_CHANGED, ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_TO_REMOVE);

        val resultOpt = handler.translateButtonList(originalList, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(2, result.size()); // one button is removed from the list

        val button1 = result.get(0);
        assertSame(ACTION, button1.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button1.getButton().getLabel().compact());
        assertNull(button1.getButton().getTooltip());
        assertEquals(10, button1.getButton().getWidth());

        assertSame(ACTION_BUTTON_UNCHANGED, result.get(1));
    }

    @Test
    public void testActionButtonListUnchanged() {
        val originalList = Arrays.asList(ACTION_BUTTON_UNCHANGED, ACTION_BUTTON_UNCHANGED);

        val resultOpt = handler.translateButtonList(originalList, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testSingleOptionEntryChanged() {
        val result = handler.translateSingleOptionEntry(SINGLE_OPTION_ENTRY_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val entry = result.getResultRaw();
        assertEquals("a", entry.getId());
        assertEquals(Component.text("replaced(entry.display)"), entry.getDisplay().compact());
        assertTrue(entry.isInitial());
    }

    @Test
    public void testSingleOptionEntryToRemove() {
        val result = handler.translateSingleOptionEntry(SINGLE_OPTION_ENTRY_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testSingleOptionEntryUnchanged() {
        val result = handler.translateSingleOptionEntry(SINGLE_OPTION_ENTRY_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testSingleOptionEntryListChanged() {
        val originalList = Arrays.asList(SINGLE_OPTION_ENTRY_CHANGED, SINGLE_OPTION_ENTRY_UNCHANGED, SINGLE_OPTION_ENTRY_TO_REMOVE);

        val resultOpt = handler.translateSingleOptionEntryList(originalList, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(2, result.size()); // one entry is removed from the list

        val entry1 = result.get(0);
        assertEquals("a", entry1.getId());
        assertEquals(Component.text("replaced(entry.display)"), entry1.getDisplay().compact());
        assertTrue(entry1.isInitial());

        assertSame(SINGLE_OPTION_ENTRY_UNCHANGED, result.get(1));
    }

    @Test
    public void testSingleOptionEntryListUnchanged() {
        val originalList = Arrays.asList(SINGLE_OPTION_ENTRY_UNCHANGED, SINGLE_OPTION_ENTRY_UNCHANGED);

        val resultOpt = handler.translateSingleOptionEntryList(originalList, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testTextInputChanged() {
        val result = handler.translateInput(TEXT_INPUT_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val input = result.getResultRaw();
        assertEquals("text-a", input.getKey());
        assertEquals(InputControlTypes.TEXT, input.getControl().getType());
        val textInput = (TextInputControl) input.getControl();
        assertEquals(10, textInput.getWidth());
        assertEquals(Component.text("replaced(input.text.label)"), textInput.getLabel().compact());
        assertTrue(textInput.isLabelVisible());
        assertEquals("initial", textInput.getInitial());
        assertEquals(20, textInput.getMaxLength());
        assertSame(MULTILINE_OPTIONS, textInput.getMultiline());
    }

    @Test
    public void testTextInputToRemove() {
        val result = handler.translateInput(TEXT_INPUT_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testTextInputUnchanged() {
        val result = handler.translateInput(TEXT_INPUT_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testBooleanInputChanged() {
        val result = handler.translateInput(BOOLEAN_INPUT_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val input = result.getResultRaw();
        assertEquals("boolean-a", input.getKey());
        assertEquals(InputControlTypes.BOOLEAN, input.getControl().getType());
        val booleanInput = (BooleanInputControl) input.getControl();
        assertEquals(Component.text("replaced(input.boolean.label)"), booleanInput.getLabel().compact());
        assertTrue(booleanInput.isInitial());
        assertEquals("true-a", booleanInput.getOnTrue());
        assertEquals("false-a", booleanInput.getOnFalse());
    }

    @Test
    public void testBooleanInputToRemove() {
        val result = handler.translateInput(BOOLEAN_INPUT_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testBooleanInputUnchanged() {
        val result = handler.translateInput(BOOLEAN_INPUT_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testNumberRangeInputChanged() {
        val result = handler.translateInput(NUMBER_RANGE_INPUT_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val input = result.getResultRaw();
        assertEquals("numrange-a", input.getKey());
        assertEquals(InputControlTypes.NUMBER_RANGE, input.getControl().getType());
        val numberRangeInput = (NumberRangeInputControl) input.getControl();
        assertEquals(10, numberRangeInput.getWidth());
        assertEquals(Component.text("replaced(input.numrange.label)"), numberRangeInput.getLabel().compact());
        assertEquals("format", numberRangeInput.getLabelFormat());
        assertSame(RANGE_INFO, numberRangeInput.getRangeInfo());
    }

    @Test
    public void testNumberRangeInputToRemove() {
        val result = handler.translateInput(NUMBER_RANGE_INPUT_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testNumberRangeInputUnchanged() {
        val result = handler.translateInput(NUMBER_RANGE_INPUT_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testSingleOptionInputChanged() {
        val result = handler.translateInput(SINGLE_OPTION_INPUT_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val input = result.getResultRaw();
        assertEquals("singleopt-a", input.getKey());
        assertEquals(InputControlTypes.SINGLE_OPTION, input.getControl().getType());
        val singleOptionInput = (SingleOptionInputControl) input.getControl();
        assertEquals(10, singleOptionInput.getWidth());
        assertEquals(0, singleOptionInput.getOptions().size());
        assertEquals(Component.text("replaced(input.singleopt.label)"), singleOptionInput.getLabel().compact());
        assertTrue(singleOptionInput.isLabelVisible());
    }

    @Test
    public void testSingleOptionInputToRemove() {
        val result = handler.translateInput(SINGLE_OPTION_INPUT_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testSingleOptionInputUnchanged() {
        val result = handler.translateInput(SINGLE_OPTION_INPUT_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testSingleOptionInputWithChangedEntries() {
        val options = Arrays.asList(SINGLE_OPTION_ENTRY_CHANGED, SINGLE_OPTION_ENTRY_TO_REMOVE);
        val inputBefore = new Input("singleopt-c", new SingleOptionInputControl(10, options, Component.text("lorem ipsum"), true));
        val result = handler.translateInput(inputBefore, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val input = result.getResultRaw();
        assertEquals("singleopt-c", input.getKey());
        assertEquals(InputControlTypes.SINGLE_OPTION, input.getControl().getType());
        val singleOptionInput = (SingleOptionInputControl) input.getControl();
        assertEquals(10, singleOptionInput.getWidth());
        assertEquals(1, singleOptionInput.getOptions().size());
        assertEquals(Component.text("lorem ipsum"), singleOptionInput.getLabel().compact());
        assertTrue(singleOptionInput.isLabelVisible());

        val option1 = singleOptionInput.getOptions().get(0);
        assertEquals("a", option1.getId());
        assertEquals(Component.text("replaced(entry.display)"), option1.getDisplay().compact());
        assertTrue(option1.isInitial());
    }

    @Test
    public void testSingleOptionInputWithUnchangedEntries() {
        val options = List.of(SINGLE_OPTION_ENTRY_UNCHANGED);
        val inputBefore = new Input("singleopt-c", new SingleOptionInputControl(10, options, Component.text("lorem ipsum"), true));
        val result = handler.translateInput(inputBefore, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testInputListChanged() {
        val originalList = Arrays.asList(TEXT_INPUT_CHANGED, BOOLEAN_INPUT_UNCHANGED, NUMBER_RANGE_INPUT_TO_REMOVE);

        val resultOpt = handler.translateInputList(originalList, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(2, result.size()); // one input is removed from the list

        val input1 = result.get(0);
        assertEquals("text-a", input1.getKey());
        assertEquals(InputControlTypes.TEXT, input1.getControl().getType());
        val textInput1 = (TextInputControl) input1.getControl();
        assertEquals(10, textInput1.getWidth());
        assertEquals(Component.text("replaced(input.text.label)"), textInput1.getLabel().compact());
        assertTrue(textInput1.isLabelVisible());
        assertEquals("initial", textInput1.getInitial());
        assertEquals(20, textInput1.getMaxLength());
        assertSame(MULTILINE_OPTIONS, textInput1.getMultiline());

        assertSame(BOOLEAN_INPUT_UNCHANGED, result.get(1));
    }

    @Test
    public void testInputListUnchanged() {
        val originalList = Arrays.asList(TEXT_INPUT_UNCHANGED, BOOLEAN_INPUT_UNCHANGED, NUMBER_RANGE_INPUT_UNCHANGED, SINGLE_OPTION_INPUT_UNCHANGED);

        val resultOpt = handler.translateInputList(originalList, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testTextBodyChanged() {
        val result = handler.translateDialogBody(TEXT_BODY_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val body = result.getResultRaw();
        assertEquals(DialogBodyTypes.PLAIN_MESSAGE, body.getType());
        val bodyText = (PlainMessageDialogBody) body;
        assertEquals(Component.text("replaced(body.text)"), bodyText.getMessage().getContents().compact());
        assertEquals(10, bodyText.getMessage().getWidth());
    }

    @Test
    public void testTextBodyToRemove() {
        val result = handler.translateDialogBody(TEXT_BODY_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testTextBodyUnchanged() {
        val result = handler.translateDialogBody(TEXT_BODY_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testItemBodyItemChanged() {
        val result = handler.translateDialogBody(ITEM_BODY_ITEM_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val body = result.getResultRaw();
        assertEquals(DialogBodyTypes.ITEM, body.getType());
        val bodyItem = (ItemDialogBody) body;
        val customName = bodyItem.getItem().getComponent(ComponentTypes.CUSTOM_NAME);
        assertTrue(customName.isPresent());
        assertEquals(Component.text("replaced(item.custom.name)"), customName.get().compact());
        assertEquals(ItemTypes.STONE, bodyItem.getItem().getType());
        assertNull(bodyItem.getDescription());
        assertFalse(bodyItem.isShowDecorations());
        assertTrue(bodyItem.isShowTooltip());
        assertEquals(10, bodyItem.getWidth());
        assertEquals(20, bodyItem.getHeight());
    }

    @Test
    public void testItemBodyDescriptionChanged() {
        val result = handler.translateDialogBody(ITEM_BODY_DESCRIPTION_CHANGED, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val body = result.getResultRaw();
        assertEquals(DialogBodyTypes.ITEM, body.getType());
        val bodyItem = (ItemDialogBody) body;
        assertSame(ITEM_UNCHANGED, bodyItem.getItem());
        assertEquals(Component.text("replaced(body.item.description)"), bodyItem.getDescription().getContents().compact());
        assertEquals(10, bodyItem.getDescription().getWidth());
        assertFalse(bodyItem.isShowDecorations());
        assertTrue(bodyItem.isShowTooltip());
        assertEquals(10, bodyItem.getWidth());
        assertEquals(20, bodyItem.getHeight());
    }

    @Test
    public void testItemBodyDescriptionToRemove() {
        val result = handler.translateDialogBody(ITEM_BODY_DESCRIPTION_TO_REMOVE, localized);

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        val body = result.getResultRaw();
        assertEquals(DialogBodyTypes.ITEM, body.getType());
        val bodyItem = (ItemDialogBody) body;
        assertSame(ITEM_UNCHANGED, bodyItem.getItem());
        assertNull(bodyItem.getDescription());
        assertFalse(bodyItem.isShowDecorations());
        assertTrue(bodyItem.isShowTooltip());
        assertEquals(10, bodyItem.getWidth());
        assertEquals(20, bodyItem.getHeight());
    }

    @Test
    public void testItemBodyUnchanged() {
        val result = handler.translateDialogBody(ITEM_BODY_UNCHANGED, localized);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testBodyListChanged() {
        val originalList = Arrays.asList(TEXT_BODY_CHANGED, ITEM_BODY_UNCHANGED, TEXT_BODY_TO_REMOVE);

        val resultOpt = handler.translateDialogBodyList(originalList, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(2, result.size()); // one body is removed from the list

        val body1 = result.get(0);
        assertEquals(DialogBodyTypes.PLAIN_MESSAGE, body1.getType());
        val bodyText1 = (PlainMessageDialogBody) body1;
        assertEquals(Component.text("replaced(body.text)"), bodyText1.getMessage().getContents().compact());
        assertEquals(10, bodyText1.getMessage().getWidth());

        assertSame(ITEM_BODY_UNCHANGED, result.get(1));
    }

    @Test
    public void testBodyListUnchanged() {
        val originalList = Arrays.asList(TEXT_BODY_UNCHANGED, ITEM_BODY_UNCHANGED);

        val resultOpt = handler.translateDialogBodyList(originalList, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testCommonDialogDataTitleChanged() {
        val resultOpt = handler.translateCommonDialogData(DATA_TITLE_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.text("replaced(data.title)"), result.getTitle().compact());
        assertNull(result.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, result.getBody());
        assertSame(INPUT_LIST_UNCHANGED, result.getInputs());
    }

    @Test
    public void testCommonDialogDataTitleToRemove() {
        val resultOpt = handler.translateCommonDialogData(DATA_TITLE_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.empty(), result.getTitle().compact());
        assertNull(result.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, result.getBody());
        assertSame(INPUT_LIST_UNCHANGED, result.getInputs());
    }

    @Test
    public void testCommonDialogDataExternalTitleChanged() {
        val resultOpt = handler.translateCommonDialogData(DATA_EXTERNAL_TITLE_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.text("lorem ipsum"), result.getTitle().compact());
        assertEquals(Component.text("replaced(data.external.title)"), result.getExternalTitle().compact());
        assertSame(BODY_LIST_UNCHANGED, result.getBody());
        assertSame(INPUT_LIST_UNCHANGED, result.getInputs());
    }

    @Test
    public void testCommonDialogDataExternalTitleToRemove() {
        val resultOpt = handler.translateCommonDialogData(DATA_EXTERNAL_TITLE_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.text("lorem ipsum"), result.getTitle().compact());
        assertNull(result.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, result.getBody());
        assertSame(INPUT_LIST_UNCHANGED, result.getInputs());
    }

    @Test
    public void testCommonDialogDataBodyChanged() {
        val resultOpt = handler.translateCommonDialogData(DATA_BODY_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.text("lorem ipsum"), result.getTitle().compact());
        assertEquals(Component.text("lorem ipsum"), result.getExternalTitle().compact());
        assertEquals(0, result.getBody().size());
        assertSame(INPUT_LIST_UNCHANGED, result.getInputs());
    }

    @Test
    public void testCommonDialogDataInputsChanged() {
        val resultOpt = handler.translateCommonDialogData(DATA_INPUTS_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(Component.text("lorem ipsum"), result.getTitle().compact());
        assertNull(result.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, result.getBody());
        assertEquals(0, result.getInputs().size());
    }

    @Test
    public void testCommonDialogDataUnchanged() {
        val resultOpt = handler.translateCommonDialogData(DATA_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testMultiActionDialogDataChanged() {
        val resultOpt = handler.translateDialog(MULTI_ACTION_DIALOG_DATA_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.MULTI_ACTION, result.getType());
        val multiActionResult = (MultiActionDialog) result;

        val data = multiActionResult.getCommon();
        assertEquals(Component.text("replaced(data.title)"), data.getTitle().compact());
        assertNull(data.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, data.getBody());
        assertSame(INPUT_LIST_UNCHANGED, data.getInputs());

        assertSame(ACTION_BUTTON_LIST_UNCHANGED, multiActionResult.getActions());
        assertNull(multiActionResult.getExitAction());
    }

    @Test
    public void testMultiActionDialogActionsChanged() {
        val resultOpt = handler.translateDialog(MULTI_ACTION_DIALOG_ACTIONS_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.MULTI_ACTION, result.getType());
        val multiActionResult = (MultiActionDialog) result;
        assertSame(DATA_UNCHANGED, multiActionResult.getCommon());
        assertEquals(0, multiActionResult.getActions().size());
        assertSame(ACTION_BUTTON_UNCHANGED, multiActionResult.getExitAction());
    }

    @Test
    public void testMultiActionDialogExitChanged() {
        val resultOpt = handler.translateDialog(MULTI_ACTION_DIALOG_EXIT_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.MULTI_ACTION, result.getType());
        val multiActionResult = (MultiActionDialog) result;
        assertSame(DATA_UNCHANGED, multiActionResult.getCommon());
        assertSame(ACTION_BUTTON_LIST_UNCHANGED, multiActionResult.getActions());

        val button = multiActionResult.getExitAction();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testMultiActionDialogExitToRemove() {
        val resultOpt = handler.translateDialog(MULTI_ACTION_DIALOG_EXIT_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.MULTI_ACTION, result.getType());
        val multiActionResult = (MultiActionDialog) result;
        assertSame(DATA_UNCHANGED, multiActionResult.getCommon());
        assertSame(ACTION_BUTTON_LIST_UNCHANGED, multiActionResult.getActions());
        assertNull(multiActionResult.getExitAction());
    }

    @Test
    public void testMultiActionDialogUnchanged() {
        val resultOpt = handler.translateDialog(MULTI_ACTION_DIALOG_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testDialogListDialogDataChanged() {
        val resultOpt = handler.translateDialog(DIALOG_LIST_DIALOG_DATA_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.DIALOG_LIST, result.getType());
        val dialogListResult = (DialogListDialog) result;

        val data = dialogListResult.getCommon();
        assertEquals(Component.text("replaced(data.title)"), data.getTitle().compact());
        assertNull(data.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, data.getBody());
        assertSame(INPUT_LIST_UNCHANGED, data.getInputs());

        assertSame(MAPPED_ENTITY_REF, dialogListResult.getDialogs());
        assertNull(dialogListResult.getExitAction());

        assertEquals(10, dialogListResult.getColumns());
        assertEquals(20, dialogListResult.getButtonWidth());
    }

    @Test
    public void testDialogListDialogExitChanged() {
        val resultOpt = handler.translateDialog(DIALOG_LIST_DIALOG_EXIT_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.DIALOG_LIST, result.getType());
        val dialogListResult = (DialogListDialog) result;
        assertSame(DATA_UNCHANGED, dialogListResult.getCommon());
        assertSame(MAPPED_ENTITY_REF, dialogListResult.getDialogs());

        val button = dialogListResult.getExitAction();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());

        assertEquals(10, dialogListResult.getColumns());
        assertEquals(20, dialogListResult.getButtonWidth());
    }

    @Test
    public void testDialogListDialogExitToRemove() {
        val resultOpt = handler.translateDialog(DIALOG_LIST_DIALOG_EXIT_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.DIALOG_LIST, result.getType());
        val dialogListResult = (DialogListDialog) result;
        assertSame(DATA_UNCHANGED, dialogListResult.getCommon());
        assertSame(MAPPED_ENTITY_REF, dialogListResult.getDialogs());
        assertNull(dialogListResult.getExitAction());

        assertEquals(10, dialogListResult.getColumns());
        assertEquals(20, dialogListResult.getButtonWidth());
    }

    @Test
    public void testDialogListDialogUnchanged() {
        val resultOpt = handler.translateDialog(DIALOG_LIST_DIALOG_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testConfirmationDialogDataChanged() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_DATA_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.CONFIRMATION, result.getType());
        val confirmationResult = (ConfirmationDialog) result;

        val data = confirmationResult.getCommon();
        assertEquals(Component.text("replaced(data.title)"), data.getTitle().compact());
        assertNull(data.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, data.getBody());
        assertSame(INPUT_LIST_UNCHANGED, data.getInputs());

        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getYesButton());
        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getNoButton());
    }

    @Test
    public void testConfirmationDialogYesChanged() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_YES_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.CONFIRMATION, result.getType());
        val confirmationResult = (ConfirmationDialog) result;
        assertSame(DATA_UNCHANGED, confirmationResult.getCommon());

        val button = confirmationResult.getYesButton();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());

        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getNoButton());
    }

    @Test
    public void testConfirmationDialogYesToRemove() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_YES_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.CONFIRMATION, result.getType());
        val confirmationResult = (ConfirmationDialog) result;
        assertSame(DATA_UNCHANGED, confirmationResult.getCommon());

        val button = confirmationResult.getYesButton();
        assertNull(button.getAction());
        assertEquals(Component.empty(), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(150, button.getButton().getWidth());

        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getNoButton());
    }

    @Test
    public void testConfirmationDialogNoChanged() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_NO_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.CONFIRMATION, result.getType());
        val confirmationResult = (ConfirmationDialog) result;
        assertSame(DATA_UNCHANGED, confirmationResult.getCommon());

        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getYesButton());

        val button = confirmationResult.getNoButton();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testConfirmationDialogNoToRemove() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_NO_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.CONFIRMATION, result.getType());
        val confirmationResult = (ConfirmationDialog) result;
        assertSame(DATA_UNCHANGED, confirmationResult.getCommon());

        assertSame(ACTION_BUTTON_UNCHANGED, confirmationResult.getYesButton());

        val button = confirmationResult.getNoButton();
        assertNull(button.getAction());
        assertEquals(Component.empty(), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(150, button.getButton().getWidth());
    }

    @Test
    public void testConfirmationDialogUnchanged() {
        val resultOpt = handler.translateDialog(CONFIRMATION_DIALOG_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testNoticeDialogDataChanged() {
        val resultOpt = handler.translateDialog(NOTICE_DIALOG_DATA_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.NOTICE, result.getType());
        val noticeResult = (NoticeDialog) result;

        val data = noticeResult.getCommon();
        assertEquals(Component.text("replaced(data.title)"), data.getTitle().compact());
        assertNull(data.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, data.getBody());
        assertSame(INPUT_LIST_UNCHANGED, data.getInputs());

        assertSame(ACTION_BUTTON_UNCHANGED, noticeResult.getAction());
    }

    @Test
    public void testNoticeDialogActionChanged() {
        val resultOpt = handler.translateDialog(NOTICE_DIALOG_ACTION_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.NOTICE, result.getType());
        val noticeResult = (NoticeDialog) result;
        assertSame(DATA_UNCHANGED, noticeResult.getCommon());

        val button = noticeResult.getAction();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());
    }

    @Test
    public void testNoticeDialogActionToRemove() {
        val resultOpt = handler.translateDialog(NOTICE_DIALOG_ACTION_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.NOTICE, result.getType());
        val noticeResult = (NoticeDialog) result;
        assertSame(DATA_UNCHANGED, noticeResult.getCommon());
        assertSame(NoticeDialog.DEFAULT_ACTION, noticeResult.getAction());
    }

    @Test
    public void testNoticeDialogUnchanged() {
        val resultOpt = handler.translateDialog(NOTICE_DIALOG_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    public void testServerLinksDialogDataChanged() {
        val resultOpt = handler.translateDialog(SERVER_LINKS_DIALOG_DATA_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.SERVER_LINKS, result.getType());
        val serverLinksResult = (ServerLinksDialog) result;

        val data = serverLinksResult.getCommon();
        assertEquals(Component.text("replaced(data.title)"), data.getTitle().compact());
        assertNull(data.getExternalTitle());
        assertSame(BODY_LIST_UNCHANGED, data.getBody());
        assertSame(INPUT_LIST_UNCHANGED, data.getInputs());

        assertSame(ACTION_BUTTON_UNCHANGED, serverLinksResult.getExitAction());

        assertEquals(10, serverLinksResult.getColumns());
        assertEquals(20, serverLinksResult.getButtonWidth());
    }

    @Test
    public void testServerLinksDialogActionChanged() {
        val resultOpt = handler.translateDialog(SERVER_LINKS_DIALOG_ACTION_CHANGED, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.SERVER_LINKS, result.getType());
        val serverLinksResult = (ServerLinksDialog) result;
        assertSame(DATA_UNCHANGED, serverLinksResult.getCommon());

        val button = serverLinksResult.getExitAction();
        assertSame(ACTION, button.getAction());
        assertEquals(Component.text("replaced(action.button.label)"), button.getButton().getLabel().compact());
        assertNull(button.getButton().getTooltip());
        assertEquals(10, button.getButton().getWidth());

        assertEquals(10, serverLinksResult.getColumns());
        assertEquals(20, serverLinksResult.getButtonWidth());
    }

    @Test
    public void testServerLinksDialogActionToRemove() {
        val resultOpt = handler.translateDialog(SERVER_LINKS_DIALOG_ACTION_TO_REMOVE, localized);
        assertTrue(resultOpt.isPresent());

        val result = resultOpt.get();
        assertEquals(DialogTypes.SERVER_LINKS, result.getType());
        val serverLinksResult = (ServerLinksDialog) result;
        assertSame(DATA_UNCHANGED, serverLinksResult.getCommon());
        assertNull(serverLinksResult.getExitAction());

        assertEquals(10, serverLinksResult.getColumns());
        assertEquals(20, serverLinksResult.getButtonWidth());
    }

    @Test
    public void testServerLinksDialogUnchanged() {
        val resultOpt = handler.translateDialog(SERVER_LINKS_DIALOG_UNCHANGED, localized);
        assertTrue(resultOpt.isEmpty());
    }
}
