package multitenantdb;

import java.util.List;

public interface TenantCatalog {

	public TenantDatabaseConfig getTenantDatabaseConfig(String tenantIdentifier) throws Exception;

	public List<String> listTenants() throws Exception;
}
