package exception;

public class SmartFarmException extends RuntimeException {

    public SmartFarmException(String message) {
        super(message);
    }

    public SmartFarmException(String message, Throwable cause) {
        super(message, cause);
    }
}
