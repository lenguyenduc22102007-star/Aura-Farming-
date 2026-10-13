package view.screen_controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.game_item.AnimalFeedItem;
import model.game_item.HarvestedCropItem;
import model.game_item.Item;
import model.player_inventory.Player;
import view.assets.Assets;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StorageViewController {

    @FXML private BorderPane rootPane;
    @FXML private FlowPane cropsPane;
    @FXML private FlowPane feedPane;
    @FXML private FlowPane suppliesPane;
    @FXML private Label moneyLabel;
    @FXML private Label statusLabel;
    @FXML private Button convertAllBtn;
    @FXML private Button sellAllBtn;

    private SceneRouter router;
    private Player player;

    public void setRouter(SceneRouter router) {
        this.router = router;
    }

    public void setPlayer(Player player) {
        this.player = player;

        if (moneyLabel != null) {
            moneyLabel.textProperty().bind(player.moneyProperty().asString("$%d"));
        }

        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if (statusLabel != null) {
                        statusLabel.setText("Welcome to Storage!");
                    }
                    build();
                }
            });
        }

        build();
    }

    public void build() {
        if (player == null) return;
        if (cropsPane != null) cropsPane.getChildren().clear();
        if (feedPane != null) feedPane.getChildren().clear();
        if (suppliesPane != null) suppliesPane.getChildren().clear();

        boolean hasCrops = false;
        boolean hasFeed = false;
        boolean hasSupplies = false;

        Map<Item, Integer> inventoryItems = player.getInventory().getItems();

        for (Map.Entry<Item, Integer> entry : inventoryItems.entrySet()) {
            Item item = entry.getKey();
            int qty = entry.getValue();
            if (qty <= 0) continue;

            if (item instanceof HarvestedCropItem) {
                hasCrops = true;
                if (cropsPane != null) {
                    cropsPane.getChildren().add(makeCropCard((HarvestedCropItem) item, qty));
                }
            } else if (item instanceof AnimalFeedItem || item.getName().equalsIgnoreCase("Animal Feed")) {
                hasFeed = true;
                if (feedPane != null) {
                    feedPane.getChildren().add(makeFeedCard(item, qty));
                }
            } else {
                hasSupplies = true;
                if (suppliesPane != null) {
                    suppliesPane.getChildren().add(makeSupplyCard(item, qty));
                }
            }
        }

        if (convertAllBtn != null) convertAllBtn.setDisable(!hasCrops);
        if (sellAllBtn != null) sellAllBtn.setDisable(!hasCrops);

        // Empty state placeholders
        if (!hasCrops && cropsPane != null) {
            cropsPane.getChildren().add(makeEmptyPlaceholder("No harvested crops in storage. Harvest crops on your farm to store or sell here!"));
        }
        if (!hasFeed && feedPane != null) {
            feedPane.getChildren().add(makeEmptyPlaceholder("No animal feed in storage. Buy feed from the shop or convert harvested crops!"));
        }
        if (!hasSupplies && suppliesPane != null) {
            suppliesPane.getChildren().add(makeEmptyPlaceholder("No seeds or supplies in your inventory."));
        }
    }

    private VBox makeCropCard(HarvestedCropItem item, int quantity) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(item), 22);

        Label name = new Label(item.getName().toUpperCase());
        name.getStyleClass().add("shop-name");

        Label stats = new Label("OWNED: " + quantity + "  |  SELL: $" + item.getPrice() + "/ea");
        stats.getStyleClass().add("shop-stats");

        Label price = new Label("$" + (item.getPrice() * quantity) + "  (Total)");
        price.getStyleClass().add("shop-name");

        Button sell1 = new Button("SELL 1");
        sell1.getStyleClass().add("btn-gold");
        sell1.setPrefWidth(90);
        sell1.setOnAction(e -> {
            if (player.getInventory().getItemCount(item) >= 1) {
                player.getInventory().removeItem(item, 1);
                player.addMoney(item.getPrice());
                if (statusLabel != null) {
                    statusLabel.setText("Sold 1 " + item.getName() + " for $" + item.getPrice() + "!");
                }
                build();
            }
        });

        Button sellAll = new Button("SELL ALL");
        sellAll.getStyleClass().add("btn-gold");
        sellAll.setPrefWidth(100);
        sellAll.setOnAction(e -> {
            int currentQty = player.getInventory().getItemCount(item);
            if (currentQty > 0) {
                int earned = currentQty * item.getPrice();
                player.getInventory().removeItem(item, currentQty);
                player.addMoney(earned);
                if (statusLabel != null) {
                    statusLabel.setText("Sold " + currentQty + " " + item.getName() + " for $" + earned + "!");
                }
                build();
            }
        });

        Button feedBtn = new Button("FEED (1)");
        feedBtn.getStyleClass().add("btn-gold");
        feedBtn.setPrefWidth(100);
        feedBtn.setOnAction(e -> {
            if (player.getInventory().getItemCount(item) >= 1) {
                player.getInventory().removeItem(item, 1);
                Item canonicalFeed = player.getInventory().findByName("Animal Feed");
                if (canonicalFeed == null) canonicalFeed = new AnimalFeedItem();
                player.getInventory().addItem(canonicalFeed, 1);
                if (statusLabel != null) {
                    statusLabel.setText("Converted 1 " + item.getName() + " into Animal Feed!");
                }
                build();
            }
        });

        HBox actions = new HBox(8, sell1, sellAll, feedBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, icon, name, stats, price, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shop-card");
        card.setPrefWidth(377);
        return card;
    }

    private VBox makeFeedCard(Item item, int quantity) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(item), 22);

        Label name = new Label("ANIMAL FEED");
        name.getStyleClass().add("shop-name");

        Label stats = new Label("OWNED: " + quantity + "  |  Used to feed farm animals daily");
        stats.getStyleClass().add("shop-stats");

        Label price = new Label("$" + (item.getPrice() * quantity) + "  ($" + item.getPrice() + "/ea)");
        price.getStyleClass().add("shop-name");

        Button sell1 = new Button("SELL 1");
        sell1.getStyleClass().add("btn-gold");
        sell1.setPrefWidth(90);
        sell1.setOnAction(e -> {
            if (player.getInventory().getItemCount(item) >= 1) {
                player.getInventory().removeItem(item, 1);
                player.addMoney(item.getPrice());
                if (statusLabel != null) {
                    statusLabel.setText("Sold 1 Animal Feed for $" + item.getPrice() + "!");
                }
                build();
            }
        });

        Button sellAll = new Button("SELL ALL");
        sellAll.getStyleClass().add("btn-gold");
        sellAll.setPrefWidth(100);
        sellAll.setOnAction(e -> {
            int currentQty = player.getInventory().getItemCount(item);
            if (currentQty > 0) {
                int earned = currentQty * item.getPrice();
                player.getInventory().removeItem(item, currentQty);
                player.addMoney(earned);
                if (statusLabel != null) {
                    statusLabel.setText("Sold " + currentQty + " Animal Feed for $" + earned + "!");
                }
                build();
            }
        });

        HBox actions = new HBox(8, sell1, sellAll);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, icon, name, stats, price, actions);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shop-card");
        card.setPrefWidth(377);
        return card;
    }

    private VBox makeSupplyCard(Item item, int quantity) {
        ImageView icon = Assets.imageView(Assets.getShopIcon(item), 22);

        Label name = new Label(item.getName().toUpperCase());
        name.getStyleClass().add("shop-name");

        Label stats = new Label("OWNED: " + quantity + "  |  " + item.getStats());
        stats.getStyleClass().add("shop-stats");

        Label price = new Label("Value: $" + item.getPrice());
        price.getStyleClass().add("shop-name");

        VBox card = new VBox(8, icon, name, stats, price);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("shop-card");
        card.setPrefWidth(377);
        return card;
    }

    private VBox makeEmptyPlaceholder(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("shop-stats");
        label.setStyle("-fx-font-size: 16; -fx-text-fill: #5C3A1E; -fx-font-weight: bold; -fx-padding: 20;");

        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(850);
        box.setMinHeight(200);
        return box;
    }

    @FXML
    private void handleProcessAllCrops(ActionEvent event) {
        int totalProcessed = 0;
        List<Item> toRemove = new ArrayList<>();

        for (Map.Entry<Item, Integer> entry : player.getInventory().getItems().entrySet()) {
            if (entry.getKey() instanceof HarvestedCropItem) {
                totalProcessed += entry.getValue();
                toRemove.add(entry.getKey());
            }
        }

        for (Item item : toRemove) {
            player.getInventory().removeItem(item, player.getInventory().getItemCount(item));
        }

        if (totalProcessed > 0) {
            Item canonicalFeed = player.getInventory().findByName("Animal Feed");
            if (canonicalFeed == null) canonicalFeed = new AnimalFeedItem();
            player.getInventory().addItem(canonicalFeed, totalProcessed);
            if (statusLabel != null) {
                statusLabel.setText("Processed all crops into " + totalProcessed + " Animal Feed!");
            }
        } else {
            if (statusLabel != null) {
                statusLabel.setText("No crops available to process!");
            }
        }
        build();
    }

    @FXML
    private void handleSellAllCrops(ActionEvent event) {
        int totalEarned = 0;
        int totalCount = 0;
        List<Item> toRemove = new ArrayList<>();

        for (Map.Entry<Item, Integer> entry : player.getInventory().getItems().entrySet()) {
            if (entry.getKey() instanceof HarvestedCropItem) {
                int count = entry.getValue();
                totalCount += count;
                totalEarned += count * entry.getKey().getPrice();
                toRemove.add(entry.getKey());
            }
        }

        for (Item item : toRemove) {
            player.getInventory().removeItem(item, player.getInventory().getItemCount(item));
        }

        if (totalEarned > 0) {
            player.addMoney(totalEarned);
            if (statusLabel != null) {
                statusLabel.setText("Sold " + totalCount + " crops for $" + totalEarned + "!");
            }
        } else {
            if (statusLabel != null) {
                statusLabel.setText("No crops to sell!");
            }
        }
        build();
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
