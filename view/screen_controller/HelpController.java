package view.screen_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import view.screen_util.SceneRouter;
import view.screen_util.Screens;

public final class HelpController {
    @FXML
    private Button btnBack;
    @FXML
    private Label body;

    private SceneRouter router;

    private static final String BODY = """
        HOW TO PLAY
          1.  Left-click a cell to use the selected tool
          2.  Click a tool icon (or press 1-5) to switch tool
          3.  Open the SHOP to buy seeds and fertilizer
          4.  Click the SEED BAG to choose which seed to plant
          5.  NEXT DAY advances weather, growth and pests
          6.  ESC opens the pause menu

        TOOLS
          1 Seed Bag   2 Hoe   3 Watering Can   4 Hand   5 Fertilizer

        CELL COLOURS
          green                     UNTILLED  (use the hoe)
          light brown           tilled, dry
          dark brown           tilled, watered
          gold flecks            high nutrient (fertilized)
          gold glow             crop is RIPE  (use the hand)
          red border            crop is DEAD  (use the hoe)

        DEMO KEYS
          [S] Sunny    [R] Rainy    [D] Drought
          [F] Fast-grow all   [P] Spawn pest   [T] Till all

        PESTS
          A beetle gives ONE day of grace. If it is still on a
          crop next morning, the crop starts to suffer stress.
        """;

    public void setRouter(SceneRouter router) {
        this.router = router;
    }

    @FXML
    private void initialize() {
        if (body != null) {
            body.setText(BODY);
        }
    }

    @FXML
    private void onBack() {
        if (router != null) {
            try {
                router.show(Screens.MAIN_MENU);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

























