package conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;

import multitenantdb.TenantDatabaseProvider;
import multitenantdb.exceptions.InvalidTenantException;
import multitenantdb.exceptions.TenantException;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.Results;

public class MyTenantFilter implements Filter {

	@Inject
	TenantDatabaseProvider tenantDatabaseProvider;

	final Logger logger = LoggerFactory.getLogger(MyTenantFilter.class);

	@Override
	public Result filter(FilterChain chain, Context context) {
		// [MultiTenantDB]: We identify each tenant via the first component of the hostname in the request.
		// Eg. http://ninja-tenant1.localdomain:8080/index.html will produce this tenant identifer "ninja-tenant1"
		//
		// !!!WARNING!!!
		// This way of inferring the tenant may not be secure depending on your environment.
		// You are advised to evaluate its suitability or to devise your own method of detecting the tenant.
		String[] hostnameParts = HostAndPort.fromString(context.getHostname()).getHostText().split("\\.", 2);
		String tenantIdentifier = hostnameParts[0].toLowerCase();

		try {
			// [MultiTenantDB]: Generally, you call beginWorkUnitForTenant() and provide it with the tenantIdentifier
			// This parameter will be passed to your TenantCatalog implementation to retrieve the tenant's database
			// information to setup the JDBC connection.
			tenantDatabaseProvider.beginWorkUnitForTenant(tenantIdentifier);

			return chain.next(context);
		} catch (TenantException e) {
			// [MultiTenantDB]: Thrown when there is an error attempting to retrieve the tenant database information
			// or when connecting to the tenant database
			return Results.internalServerError();
		} catch (InvalidTenantException e) {
			// [MultiTenantDB]: Thrown when there is no database information for this tenant
			return Results.notFound();
		} finally {
			// [MultiTenantDB]: You must always end the work unit properly by calling endWorkUnit()
			tenantDatabaseProvider.endWorkUnit();
		}
	}

}
