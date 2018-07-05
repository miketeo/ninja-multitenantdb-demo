package multitenantdb;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.persist.Transactional;

public class MultiTenantModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TenantDatabaseProvider.class);
		bind(EntityManager.class).toProvider(TenantDatabaseProvider.class);
		bind(EntityManagerFactory.class).toProvider(TenantDatabaseProvider.EntityManagerFactoryProvider.class);

		TenantLocalTxnInterceptor txnInterceptor = new TenantLocalTxnInterceptor();
		requestInjection(txnInterceptor);

		// class-level @Transactional
		bindInterceptor(
				annotatedWith(Transactional.class),
				any(),
				txnInterceptor);

		// method-level @Transactional
		bindInterceptor(
				any(),
				annotatedWith(Transactional.class),
				txnInterceptor);
	}
}
