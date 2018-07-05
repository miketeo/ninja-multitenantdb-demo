package conf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import multitenantdb.TenantCatalog;
import multitenantdb.TenantDatabaseConfig;
import ninja.utils.NinjaProperties;

@Singleton // [multitenantdb] Remember to specify @Singleton annotation
// Otherwise, multiple instances will be created, resulting in many connection pools for the catalog database
public class MyTenantCatalog implements TenantCatalog {

	final Logger logger = LoggerFactory.getLogger(MyTenantCatalog.class);

	private HikariDataSource catalogDataSource;

	@Inject
	public MyTenantCatalog(NinjaProperties ninjaProperties) {
		// [multitentantdb]: We setup a HikariCP connection pool for the catalog database.
		HikariConfig catalogConfig = new HikariConfig();
		catalogConfig.setJdbcUrl(ninjaProperties.getOrDie("multitenantdb.catalog.jdbcUrl"));
		catalogConfig.setUsername(ninjaProperties.getOrDie("multitenantdb.catalog.user"));
		catalogConfig.setPassword(ninjaProperties.getOrDie("multitenantdb.catalog.password"));
		catalogConfig.setPoolName("TenantCatalog");
		catalogConfig.setMinimumIdle(1);
		catalogDataSource = new HikariDataSource(catalogConfig);
		logger.info("Connection pool setup for tenant catalog database at {}", catalogConfig.getJdbcUrl());
	}

	// [multitentantdb]: The getTenantDatabaseConfig() method could be invoked very frequently.
	// One possible improvement is to cache the returned result from the database.
	@Override
	public TenantDatabaseConfig getTenantDatabaseConfig(String tenantIdentifier) throws Exception {
		if (Strings.isNullOrEmpty(tenantIdentifier)) {
			throw new IllegalArgumentException("getTenantDatabaseConfig: tenantIdentifier cannot be null/empty");
		}

		Connection conn = catalogDataSource.getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT db_host,db_name,db_user,db_password FROM TenantInfo WHERE tenant_id=?");
			stmt.setString(1, tenantIdentifier.toLowerCase());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String dbURL = "jdbc:mysql://"+rs.getString(1)+"/"+rs.getString(2);
				String dbUser = rs.getString(3);
				String dbPassword = rs.getString(4);
				rs.close();

				logger.debug("getTenantDatabaseConfig: Tenant info (dbURL:{} dbUser:{}) found for tenant '{}'", dbURL, dbUser, tenantIdentifier);
				return new TenantDatabaseConfig(tenantIdentifier, dbURL, dbUser, dbPassword);
			} else {
				logger.warn("getTenantDatabaseConfig: No tenant information found for '{}'", tenantIdentifier);
				return null;
			}
		} finally {
			conn.close();
		}
	}

	@Override
	public List<String> listTenants() throws Exception {
		List<String> identifiers = new ArrayList<>();
		Connection conn = catalogDataSource.getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT tenant_id FROM TenantInfo");
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				identifiers.add(rs.getString(1));
			}
			rs.close();

			return identifiers;
		} finally {
			conn.close();
		}
	}

}
