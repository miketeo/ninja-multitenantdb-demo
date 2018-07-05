package multitenantdb;

public class TenantDatabaseConfig {

	public String tenantIdentifier;
	public String jdbcURL;
	public String user;
	public String password;

	public TenantDatabaseConfig(String tenantIdentifier, String jdbcURL, String user, String password) {
		this.tenantIdentifier = tenantIdentifier;
		this.jdbcURL = jdbcURL;
		this.user = user;
		this.password = password;
	}

}
