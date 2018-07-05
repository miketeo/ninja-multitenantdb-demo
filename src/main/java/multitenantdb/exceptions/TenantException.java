package multitenantdb.exceptions;

public class TenantException extends Exception {

	private static final long serialVersionUID = 1L;

	public TenantException(String message, Exception e) {
		super(message, e);
	}
}
