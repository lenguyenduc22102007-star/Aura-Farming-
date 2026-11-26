package view.screen_controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import view.assets.Assets;

import java.util.Map;
import java.util.function.Consumer;

public final class SeedPickerController {

    @FXML
    private VBox list;

    private Consumer<String> onPick = new Consumer<String>() {
        @Override
        public void accept(String s) {}
    };

    private Runnable onClose = new Runnable() {
        @Override
        public void run() {}
    };

    // Wire the callbacks and fill the list from the player's inventory.
    public void init(Player player, Consumer<String> onPick, Runnable onClose) {
        if (onPick != null) this.onPick  = onPick;
        if (onClose != null) this.onClose = onClose;
        populate(player);
    }

    private void populate(Player player) {
        if (list == null) return;
        list.getChildren().clear();

        boolean any = false;
        try {
            if (player != null) {
                for (Map.Entry<Item, Integer> e : player.getInventory().getItems().entrySet()) {
                    if (e.getKey() instanceof Seed) {
                        Seed seed = (Seed) e.getKey();
                        any = true;
                        list.getChildren().add(makeRow(seed, e.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!any) {
            Label empty = new Label("No seeds yet — visit the Shop!");
            empty.getStyleClass().add("seed-name");
            list.getChildren().add(empty);
        }
    }

    private HBox makeRow(final Seed seed, int qty) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(seed), 36);

        Label name = new Label(seed.getName() + "  x" + qty);
        name.getStyleClass().add("seed-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button use = new Button("USE");
        use.getStyleClass().add("btn-gold");
        use.setDisable(qty <= 0);
        use.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                onPick.accept(seed.getName());
                onClose.run();
            }
        });

        HBox row = new HBox(12, icon, name, spacer, use);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("seed-row");
        return row;
    }

    @FXML
    private void onCancel() {
        onClose.run();
    }
}
