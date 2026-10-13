package controller.lifecyclemanager;

import model.soil.Cell;

import java.util.Objects;

public abstract class BaseController {
    protected final GameManager gameManager;

    protected BaseController(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager must not be null");
    }
}
