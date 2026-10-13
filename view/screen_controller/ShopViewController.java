package view.screen_controller;

import controller.shopmanager.ItemManager;
import controller.shopmanager.ShopController;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import model.game_item.Item;
import model.game_item.Seed;
import model.player_inventory.Player;
import view.assets.Assets;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

public final class ShopViewController {

    @FXML private FlowPane seedPane;
    @FXML private FlowPane fertilizerPane;
    @FXML private FlowPane animalPane;
    @FXML private Label moneyLabel;
    @FXML private Label statusLabel;

    private SceneRouter router;
    private Player player;
    private ShopController shop;

    public void setRouter(SceneRouter router) {
        this.router = router;
    }

    public void setShop(Player player, ShopController shop) {
        this.player = player;
        this.shop   = shop;

        moneyLabel.textProperty().bind(player.moneyProperty().asString("$%d"));

        // Reset the status banner every time the shop is (re)opened
        if (statusLabel != null) {
            statusLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    statusLabel.setText("Welcome to the shop!");
                }
            });
        }

        build();
    }

    private void build() {
        if (seedPane == null || fertilizerPane == null || shop == null) return;
        seedPane.getChildren().clear();
        fertilizerPane.getChildren().clear();
        if (animalPane != null) animalPane.getChildren().clear();

        for (ItemManager entry : shop.getCatalog()) {
            Item prototype = entry.create();
            VBox card = makeCard(entry, prototype);
            if (prototype instanceof Seed) {
                seedPane.getChildren().add(card);
            } else if (entry instanceof controller.shopmanager.AnimalShopItem 
                    || prototype instanceof model.game_item.BabyChickenItem 
                    || prototype instanceof model.game_item.BabyPigItem 
                    || prototype instanceof model.game_item.BabyCowItem 
                    || prototype instanceof model.game_item.AnimalFeedItem
                    || prototype.getName().toLowerCase().contains("baby")
                    || prototype.getName().toLowerCase().contains("animal")) {
                if (animalPane != null) {
                    animalPane.getChildren().add(card);
                }
            } else {
                fertilizerPane.getChildren().add(card);
            }
        }
        if (statusLabel != null) statusLabel.setText("Welcome to the shop!");
    }

    private VBox makeCard(final ItemManager entry, Item prototype) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(prototype), 20);

        Label name = new Label(prototype.getName().toUpperCase());
        name.getStyleClass().add("shop-name");

        Label stats = new Label(prototype.getStats());
        stats.getStyleClass().add("shop-stats");

        Label price = new Label("$" + prototype.getPrice());
        price.getStyleClass().add("shop-name");

        Button buy = new Button("BUY");
        buy.getStyleClass().add("btn-gold");
        buy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                shop.buyItem(entry, 1);
                if (statusLabel != null) statusLabel.setText(shop.getLastMessage());
            }
        });


        VBox card = new VBox(8, icon, name, stats, price, buy);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shop-card");
        card.setPrefWidth(377);
        return card;
    }

    @FXML
    private void backToGame() {
        if (router != null) {
            try {
                router.show(Screens.INGAME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
