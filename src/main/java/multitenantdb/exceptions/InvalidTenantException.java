package multitenantdb.exceptions;

public class InvalidTenantException extends Exception {

	private static final long serialVersionUID = 1L;

	public String tenantIdentifier;

	public InvalidTenantException(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public String toString() {
		return "Invalid tenant identifier: " + tenantIdentifier;
	}
}
