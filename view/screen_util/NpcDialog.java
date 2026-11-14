package view.screen_util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game NPC conversation panel. Reuses the same wood panel / parchment card /
 * gold + wood button styles as the rest of the UI so it matches the game's look.
 * The dialog is a single overlay whose page (text + buttons) can be swapped in place.
 */
public final class NpcDialog {

    /** A button on a dialog page. */
    public static final class Option {
        final String text;
        final boolean primary;
        final Runnable action;

        public Option(String text, boolean primary, Runnable action) {
            this.text = text;
            this.primary = primary;
            this.action = action;
        }
    }

    private final SceneRouter router;
    private final StackPane root = new StackPane();
    private final Label nameChip = new Label();
    private final Label message = new Label();
    private final HBox buttonRow = new HBox(12);
    private Runnable onClosed;

    public NpcDialog(SceneRouter router, String npcName) {
        this.router = router;

        root.getStyleClass().add("overlay-scrim");
        root.setAlignment(Pos.CENTER);

        nameChip.setText(npcName.toUpperCase());
        nameChip.getStyleClass().add("npc-name");

        message.getStyleClass().add("npc-text");
        message.setWrapText(true);
        message.setMaxWidth(440);
        message.setMinHeight(70);

        VBox card = new VBox(message);
        card.getStyleClass().add("parchment-card");
        card.setAlignment(Pos.CENTER_LEFT);

        buttonRow.setAlignment(Pos.CENTER);

        VBox panel = new VBox(16, nameChip, card, buttonRow);
        panel.getStyleClass().addAll("wood-panel", "npc-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(520);
        panel.setMaxHeight(StackPane.USE_PREF_SIZE);

        root.getChildren().add(panel);
    }

    /** Called after the dialog has been closed (use it to restore game focus). */
    public NpcDialog onClosed(Runnable onClosed) {
        this.onClosed = onClosed;
        return this;
    }

    public void open(String text, Option... options) {
        setPage(text, options);
        router.showOverlay(root);
    }

    public void setPage(String text, Option... options) {
        message.setText(text);
        List<Node> buttons = new ArrayList<>();
        for (Option o : options) {
            Button b = new Button(o.text);
            b.getStyleClass().add(o.primary ? "btn-gold" : "btn-wood");
            b.setMinWidth(110);
            b.setFocusTraversable(false);
            b.setOnAction(e -> o.action.run());
            buttons.add(b);
        }
        buttonRow.getChildren().setAll(buttons);
    }

    public void close() {
        router.closeOverlay();
        if (onClosed != null) onClosed.run();
    }
}
