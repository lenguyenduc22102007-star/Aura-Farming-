package controller.lifecyclemanager;

import model.player_inventory.Player;
import model.soil.FarmMap;

import java.util.List;
import java.util.Objects;

public class GameOverController extends BaseController implements DayObserver {
    private final Player player;
    private final FarmMap farmMap;
    private final List<GameOverCondition> conditions;
    private final GameOverListener listener;
    private final int graceDays;
    private int consecutiveLostDays;

    public GameOverController(GameManager gameManager, FarmMap farmMap, Player player, List<GameOverCondition> conditions, GameOverListener listener, int graceDays) {
        super(gameManager);
        this.farmMap = farmMap;
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.conditions = List.copyOf(conditions);
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
        this.graceDays = Math.max(0, graceDays);
        gameManager.addObserver(this);
    }

    @Override
    public void dayEnded() {
        if (gameManager.getStatus() != GameStatus.RUNNING) {
            return;
        }

        GameStatus result = evaluateConditions();

        if (result == GameStatus.LOST) {
            consecutiveLostDays++;
            if (consecutiveLostDays > graceDays) {
                announce(GameStatus.LOST);
            }
            return;
        }

        consecutiveLostDays = 0;
    }

    private GameStatus evaluateConditions() {
        for (GameOverCondition condition : conditions) {
            GameStatus result = condition.evaluate(player, farmMap, gameManager.getCurrentDay());
            if (result != GameStatus.RUNNING) {
                return result;
            }
        }
        return GameStatus.RUNNING;
    }

    private void announce(GameStatus status) {
        gameManager.setStatus(status);
        listener.onGameOver(status);
    }
}
