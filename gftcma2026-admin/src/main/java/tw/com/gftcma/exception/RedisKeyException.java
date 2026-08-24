package tw.com.gftcma.exception;

public class RedisKeyException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public RedisKeyException(String message) {
        super(message);
    }
}
