package com.usainsrht.purpurpvp.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fluent service for opening client-side native Minecraft Dialogs in Paper 26.2+.
 * Used for text input, checkboxes, dropdown option selections, number sliders, and confirmations.
 */
public class DialogService {

    private static final ClickCallback.Options DEFAULT_CLICK_OPTIONS = ClickCallback.Options.builder().build();

    private static DialogAction action(DialogActionCallback callback) {
        return DialogAction.customClick(callback, DEFAULT_CLICK_OPTIONS);
    }

    /**
     * Opens a text input dialog prompting the player for a string value.
     */
    public void promptText(Player player, Component title, Component prompt, String initialValue, Consumer<String> callback) {
        String key = "text_input";
        TextDialogInput.Builder inputBuilder = DialogInput.text(key, prompt != null ? prompt : Component.text("Input"))
                .labelVisible(true);
        if (initialValue != null && !initialValue.isEmpty()) {
            inputBuilder.initial(initialValue);
        }
        TextDialogInput input = inputBuilder.build();

        ActionButton submitBtn = ActionButton.builder(Component.text("Submit", NamedTextColor.GREEN))
                .action(action((view, audience) -> {
                    String result = view.getText(key);
                    if (result != null && !result.trim().isEmpty()) {
                        callback.accept(result.trim());
                    }
                }))
                .build();

        ActionButton cancelBtn = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(action((view, audience) -> {
                    // Closed without action
                }))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            DialogBase base = DialogBase.builder(title)
                    .canCloseWithEscape(true)
                    .body(prompt != null ? List.of(DialogBody.plainMessage(prompt)) : List.of())
                    .inputs(List.of(input))
                    .build();
            builder.base(base);
            builder.type(DialogType.confirmation(submitBtn, cancelBtn));
        });

        player.showDialog(dialog);
    }

    /**
     * Opens a toggle/boolean checkbox dialog.
     */
    public void promptBoolean(Player player, Component title, Component prompt, boolean defaultValue, Consumer<Boolean> callback) {
        String key = "bool_input";
        BooleanDialogInput input = DialogInput.bool(key, prompt != null ? prompt : Component.text("Toggle"))
                .initial(defaultValue)
                .build();

        ActionButton submitBtn = ActionButton.builder(Component.text("Confirm", NamedTextColor.GREEN))
                .action(action((view, audience) -> {
                    Boolean val = view.getBoolean(key);
                    callback.accept(val != null ? val : defaultValue);
                }))
                .build();

        ActionButton cancelBtn = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(action((view, audience) -> {}))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            DialogBase base = DialogBase.builder(title)
                    .canCloseWithEscape(true)
                    .body(prompt != null ? List.of(DialogBody.plainMessage(prompt)) : List.of())
                    .inputs(List.of(input))
                    .build();
            builder.base(base);
            builder.type(DialogType.confirmation(submitBtn, cancelBtn));
        });

        player.showDialog(dialog);
    }

    /**
     * Opens a single-option dropdown / selection dialog.
     */
    public void promptOption(Player player, Component title, Component prompt, List<DialogOption> options, Consumer<String> callback) {
        String key = "option_input";
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (DialogOption opt : options) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(opt.id(), opt.label(), opt.initial()));
        }

        SingleOptionDialogInput input = DialogInput.singleOption(key, prompt != null ? prompt : Component.text("Select Option"), entries)
                .labelVisible(true)
                .build();

        ActionButton submitBtn = ActionButton.builder(Component.text("Select", NamedTextColor.GREEN))
                .action(action((view, audience) -> {
                    String selected = view.getText(key);
                    if (selected != null) {
                        callback.accept(selected);
                    }
                }))
                .build();

        ActionButton cancelBtn = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(action((view, audience) -> {}))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            DialogBase base = DialogBase.builder(title)
                    .canCloseWithEscape(true)
                    .body(prompt != null ? List.of(DialogBody.plainMessage(prompt)) : List.of())
                    .inputs(List.of(input))
                    .build();
            builder.base(base);
            builder.type(DialogType.confirmation(submitBtn, cancelBtn));
        });

        player.showDialog(dialog);
    }

    /**
     * Opens a number range slider dialog.
     */
    public void promptNumber(Player player, Component title, Component prompt, float min, float max, float defaultValue, Consumer<Float> callback) {
        String key = "number_input";
        NumberRangeDialogInput input = DialogInput.numberRange(key, prompt != null ? prompt : Component.text("Value"), min, max)
                .initial(defaultValue)
                .build();

        ActionButton submitBtn = ActionButton.builder(Component.text("Confirm", NamedTextColor.GREEN))
                .action(action((view, audience) -> {
                    Float val = view.getFloat(key);
                    callback.accept(val != null ? val : defaultValue);
                }))
                .build();

        ActionButton cancelBtn = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(action((view, audience) -> {}))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            DialogBase base = DialogBase.builder(title)
                    .canCloseWithEscape(true)
                    .body(prompt != null ? List.of(DialogBody.plainMessage(prompt)) : List.of())
                    .inputs(List.of(input))
                    .build();
            builder.base(base);
            builder.type(DialogType.confirmation(submitBtn, cancelBtn));
        });

        player.showDialog(dialog);
    }

    /**
     * Opens a simple Confirmation dialog with Yes / No actions.
     */
    public void confirm(Player player, Component title, Component message, Runnable onConfirm, Runnable onCancel) {
        ActionButton yesBtn = ActionButton.builder(Component.text("Confirm", NamedTextColor.GREEN))
                .action(action((view, audience) -> {
                    if (onConfirm != null) onConfirm.run();
                }))
                .build();

        ActionButton noBtn = ActionButton.builder(Component.text("Cancel", NamedTextColor.RED))
                .action(action((view, audience) -> {
                    if (onCancel != null) onCancel.run();
                }))
                .build();

        Dialog dialog = Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            DialogBase base = DialogBase.builder(title)
                    .canCloseWithEscape(true)
                    .body(List.of(DialogBody.plainMessage(message)))
                    .build();
            builder.base(base);
            builder.type(DialogType.confirmation(yesBtn, noBtn));
        });

        player.showDialog(dialog);
    }

    /**
     * Creates a builder for custom composite forms with multiple inputs.
     */
    public CustomFormBuilder formBuilder(Component title) {
        return new CustomFormBuilder(title);
    }

    public static class CustomFormBuilder {
        private final Component title;
        private Component message;
        private final List<DialogInput> inputs = new ArrayList<>();
        private Component submitLabel = Component.text("Submit", NamedTextColor.GREEN);
        private Component cancelLabel = Component.text("Cancel", NamedTextColor.RED);
        private BiConsumer<Player, DialogResponse> onSubmit;
        private Consumer<Player> onCancel;

        public CustomFormBuilder(Component title) {
            this.title = title;
        }

        public CustomFormBuilder message(Component message) {
            this.message = message;
            return this;
        }

        public CustomFormBuilder addText(String key, Component label, String initialValue) {
            TextDialogInput.Builder b = DialogInput.text(key, label).labelVisible(true);
            if (initialValue != null && !initialValue.isEmpty()) b.initial(initialValue);
            inputs.add(b.build());
            return this;
        }

        public CustomFormBuilder addBoolean(String key, Component label, boolean initialValue) {
            inputs.add(DialogInput.bool(key, label).initial(initialValue).build());
            return this;
        }

        public CustomFormBuilder addOptions(String key, Component label, List<DialogOption> options) {
            List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
            for (DialogOption opt : options) {
                entries.add(SingleOptionDialogInput.OptionEntry.create(opt.id(), opt.label(), opt.initial()));
            }
            inputs.add(DialogInput.singleOption(key, label, entries).labelVisible(true).build());
            return this;
        }

        public CustomFormBuilder addNumber(String key, Component label, float min, float max, float initialValue) {
            inputs.add(DialogInput.numberRange(key, label, min, max).initial(initialValue).build());
            return this;
        }

        public CustomFormBuilder submitLabel(Component label) {
            this.submitLabel = label;
            return this;
        }

        public CustomFormBuilder cancelLabel(Component label) {
            this.cancelLabel = label;
            return this;
        }

        public CustomFormBuilder onSubmit(BiConsumer<Player, DialogResponse> onSubmit) {
            this.onSubmit = onSubmit;
            return this;
        }

        public CustomFormBuilder onCancel(Consumer<Player> onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        public void open(Player player) {
            ActionButton submitBtn = ActionButton.builder(submitLabel)
                    .action(action((view, audience) -> {
                        if (onSubmit != null && audience instanceof Player p) {
                            onSubmit.accept(p, new DialogResponse(view));
                        }
                    }))
                    .build();

            ActionButton cancelBtn = ActionButton.builder(cancelLabel)
                    .action(action((view, audience) -> {
                        if (onCancel != null && audience instanceof Player p) {
                            onCancel.accept(p);
                        }
                    }))
                    .build();

            Dialog dialog = Dialog.create(factory -> {
                DialogRegistryEntry.Builder builder = factory.empty();
                DialogBase.Builder base = DialogBase.builder(title).canCloseWithEscape(true);
                if (message != null) {
                    base.body(List.of(DialogBody.plainMessage(message)));
                }
                base.inputs(inputs);
                builder.base(base.build());
                builder.type(DialogType.confirmation(submitBtn, cancelBtn));
            });

            player.showDialog(dialog);
        }
    }
}
