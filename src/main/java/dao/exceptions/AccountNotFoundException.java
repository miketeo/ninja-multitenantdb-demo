package dao.exceptions;

public class AccountNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public String accountID;

	public AccountNotFoundException(String accountID) {
		this.accountID = accountID;
	}
}
