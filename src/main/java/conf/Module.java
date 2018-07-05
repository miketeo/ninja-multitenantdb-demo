package conf;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import multitenantdb.MultiTenantModule;
import multitenantdb.TenantCatalog;
import schedules.CompletionWorker;

@Singleton
public class Module extends AbstractModule {

	@Override
	protected void configure() {
		// [MultiTenantDB]: Bind your TenantCatalog implementation here
		bind(TenantCatalog.class).to(MyTenantCatalog.class);
		// [MultiTenantDB]: Install the MultiTenantModule after your TenantCatalog binding
		install(new MultiTenantModule());

		// [MultiTenantDB]: Add any schdeuled actions
		bind(CompletionWorker.class);
	}
}
