package multitenantdb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import multitenantdb.exceptions.InvalidTenantException;
import multitenantdb.exceptions.TenantException;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class TenantDatabaseProvider implements Provider<EntityManager> {

	final Logger logger = LoggerFactory.getLogger(TenantDatabaseProvider.class);

	private final ThreadLocal<EntityManagerFactory> entityManagerFactory = new ThreadLocal<>();
	private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();

	private String tenantPersistenceUnitName;
	private Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<String, EntityManagerFactory>();

	@Inject
	TenantCatalog tenantCatalog;

	@Inject
	public TenantDatabaseProvider(NinjaProperties ninjaProperties) {
		this.tenantPersistenceUnitName = ninjaProperties.getOrDie("multitenantdb.tenant.persistence_unit_name");
	}

	@Override
	public EntityManager get() {
		EntityManager em = entityManager.get();
		Preconditions.checkState(em!=null, "Requesting EntityManager outside work unit. Please call beginWorkUnitForTenant() first");
		return em;
	}

	public boolean isWorking() {
		return entityManager.get() != null;
	}

	public void beginWorkUnitForTenant(String tenantIdentifier) throws TenantException, InvalidTenantException {
		EntityManager em = entityManager.get();
		Preconditions.checkState(em==null, "Previous work unit was not ended. Please call endWorkUnit() first.");

		EntityManagerFactory emFactory = getEntityManagerFactory(tenantIdentifier);
		entityManagerFactory.set(emFactory);
		entityManager.set(emFactory.createEntityManager());

		logger.debug("beginWorkUnitForTenant: Work unit for tenant '{}' started", tenantIdentifier);
	}

	public void endWorkUnit() {
		EntityManager em = entityManager.get();
		if (em!=null) {
			try {
				em.close();
			} finally {
				entityManager.remove();
			}
			entityManagerFactory.remove();
		}
		logger.debug("endWorkUnit: Work unit ended");
	}

	@Start(order = 10)
	public void startProvider() throws Exception {
		logger.info("Starting multi-tenant database provider...");

		List<String> tenantIdentifiers = null;
		try {
			tenantIdentifiers = tenantCatalog.listTenants();
		} catch (Exception ex) {
			throw new Exception("startProvider: Cannot list tenant identifiers", ex);
		}
		for (String tenantIdentifier : tenantIdentifiers) {
			TenantDatabaseConfig dbConfig = tenantCatalog.getTenantDatabaseConfig(tenantIdentifier);
			if (dbConfig!=null) { /// Should not be null
				performMigrationForTenant(dbConfig);

				// [multitenantdb]: We instantiate the factories for all tenants here.
				// If this behavior is not desirable, the code block can be commented out and
				// let the factory be instantiated only when there is a need to access the tenant data
				EntityManagerFactory factory = instantiateEntityManagerFactory(dbConfig);
				assert factory!=null;
				entityManagerFactories.put(tenantIdentifier, factory);
			}
		}
	}

	@Dispose(order = 10)
	public synchronized void endProvider() {
		for (EntityManagerFactory factory : entityManagerFactories.values()) {
			factory.close();
		}

		logger.info("endProvider: Provider disposed");
	}

	protected EntityManagerFactory getEntityManagerFactory(String tenantIdentifier) throws TenantException, InvalidTenantException  {
		EntityManagerFactory factory = null;

		synchronized(this) {
			factory = entityManagerFactories.get(tenantIdentifier);
		}

		if (factory==null) {
			TenantDatabaseConfig dbConfig = null;
			try {
				dbConfig = tenantCatalog.getTenantDatabaseConfig(tenantIdentifier);
			} catch (Exception e) {
				throw new TenantException(String.format("Cannot retrieve database config for tenant '%s'", tenantIdentifier), e);
			}
			if (dbConfig==null) {
				throw new InvalidTenantException(tenantIdentifier);
			}

			synchronized (this) {
				factory = entityManagerFactories.get(tenantIdentifier);
				if (factory==null) {
					factory = instantiateEntityManagerFactory(dbConfig);
					assert factory!=null;

					entityManagerFactories.put(tenantIdentifier, factory);
				}
			}
		}
		return factory;
	}

	protected void performMigrationForTenant(TenantDatabaseConfig dbConfig) {
		Flyway flyway = new Flyway();
		flyway.setDataSource(dbConfig.jdbcURL, dbConfig.user, dbConfig.password);
		flyway.setLocations("classpath:db/migration/");
		flyway.migrate();
	}

	protected EntityManagerFactory instantiateEntityManagerFactory(TenantDatabaseConfig dbConfig) {
		Properties jpaProperties = new Properties();
		jpaProperties.put("hibernate.ejb.entitymanager_factory_name", dbConfig.tenantIdentifier);
		jpaProperties.put("hibernate.hikari.poolName", dbConfig.tenantIdentifier);
		if (dbConfig.jdbcURL!=null) {
			jpaProperties.put("hibernate.connection.url", dbConfig.jdbcURL);
		}
		if (dbConfig.user!=null) {
			jpaProperties.put("hibernate.connection.username", dbConfig.user);
		}
		if (dbConfig.password!=null) {
			jpaProperties.put("hibernate.connection.password", dbConfig.password);
		}

		return Persistence.createEntityManagerFactory(tenantPersistenceUnitName, jpaProperties);
	}

	@Singleton
	public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory> {
		private final TenantDatabaseProvider emProvider;

		@Inject
		public EntityManagerFactoryProvider(TenantDatabaseProvider emProvider) {
			this.emProvider = emProvider;
		}

		@Override
		public EntityManagerFactory get() {
			EntityManagerFactory emFactory = emProvider.entityManagerFactory.get();
			assert emFactory!=null; // can be null if beginWorkUnitForTenant() was not called earlier
			return emFactory;
		}
	}
}
