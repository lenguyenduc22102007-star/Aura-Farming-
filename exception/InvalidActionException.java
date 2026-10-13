package exception;

public class InvalidActionException extends SmartFarmException {

    public InvalidActionException(String message) {
        super(message);
    }

    public InvalidActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
