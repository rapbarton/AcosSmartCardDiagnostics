package smartcard;

public class SmartCardApplicationException extends RuntimeException {
	private static final long serialVersionUID = 2373295493937586812L;

	public SmartCardApplicationException (String message) {
		super(message);
	}
}
