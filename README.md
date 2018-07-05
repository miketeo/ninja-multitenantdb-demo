# About this demo

This demo aims to illustrate how to support multi-tenant databases using Ninja web framework. I hope this could be useful to developers who are looking to support multi-tenant databases, especially for existing Ninja applications that are now being upgraded to support multiple tenants in a software-as-a-service model.

# Features of this demo

- Support multi-tenant databases using Ninja web framework. Majority of the existing Ninja web framework documentation for database access is still applicable; only small amount of changes is required to provide tenant database information, and to bind the EntityManager provider to each tenant at runtime.
- Existing database migration scripts can be used to migrate database changes to all tenant databases.
- Use HikariCP connection pool in Ninja web framework.

# Setting up the demo

The demo was developed on Ubuntu 16.04 (x64) for Ninja web framework 6.1.0 and will connect to MySQL databases on localhost. All software (maven, openjdk-8-jdk, mariadb-client and mariadb-server) are installed from Ubuntu official repositories. Please install these software before setting up this demo.

1.  Create the databases and users needed for the demo. You can run the ```sql/databases.sql``` script using your favourite MySQL client as *root* user.
```
$> mysql -u root -p mysql < sql/databases.sql
```

2. Set up the tenant_catalog database schema and its default tenants for the demo. The database password is *catalogpw*.
```
$> mysql -u cataloguser -p tenant_catalog < sql/db_catalog.sql
```

3. As a root user, edit the file ```/etc/hosts``` using your favourite text editor, add in the following line, and save the changes.
```
127.0.0.1	ninja-tenant1.localdomain ninja-tenant2.localdomain
```

4. Generate the demo package first
```
$> mvn clean package
```

5. Now run the demo in development mode.
```
$> mvn ninja:run
```

Check the tenant1 and tenant2 databases using your favourite MySQL clients. If the databases are setup correctly, you will see Account and Task tables being created automatically in the two tenant databases, similar to how Ninja framework does the database migration for you.

# Testing the demo

1. On the Ubuntu machine, start the local web browser like Firefox, and access http://ninja-tenant1.localdomain:8080/. You will see a simple login page.

2. Fill in any random ID, and click on the Login button. You will be redirected to the Task listing page.

3. Start another tab on the web browser and access http://ninja-tenant2.localdomain:8080/. Fill in a different ID to sign in.

Compare the User tables on tenant1 and tenant2 databases. You will see different rows being created for each User table.

You can continue to test with different account IDs, and create tasks for each account via the web UI. The system will know which database to use for the queries and data updates based on the web site's hostname.

# General working of the demo

In the demo's code, all multi-tenant specific comments are prefixed with "*[multitenantdb]*". You can search/find all these comments and learn about the workings.

- The **conf.MyTenantCatalog** class provides the tenant database information. The tenant identifier is implementation-specific and how this identifier is derived will depend on each application. In the demo, the tenant identifier is the first component of the hostname in the incoming HTTP requests, and will be matched against the *tenant_id* column in the *TenantInfo* table in *tenant_catalog* datbase.
- The **conf.MyFilter** class implements the Ninja filter interface, and extracts the tenant identifier from the hostname of the incoming requests. The class also begins the work unit for each tenant by calling **TenantDatabaseProvider.beginWorkUnitForTenant()** method. When the request processing completed, the work unit is closed by calling **TenantDatabaseProvider.endWorkUnit()**. Finally, the **MyFilter** class is registered as a global filter in the **conf.Filters**, so that it will be called for all incoming HTTP requests.
- The **MyTenantCatalog** and **MultiTenantModule** classes are bounded in the **conf.Module** class. These two bindings should be called before other bindings.
- The **schedules.CompletionWorker** class shows how to implement scheduled jobs across all tenants. The **TenantDatabaseProvider.beginWorkUnitForTenant()** method must be called for each tenant before any database queries/updates, and to end the work unit by calling  **TenantDatabaseProvider.endWorkUnit()**.

# Customizing your Ninja applications

You can
- upgrade your existing Ninja application to support multi-tenant databases using the following steps, or
- start from scratch by following the instructions on Ninja framework's [Create your first application](http://www.ninjaframework.org/documentation/getting_started/create_your_first_application.html) page.

The following changes need to be made to upgrade your application to support multi-tenant databases.

1. Comment out ```ninja.migration.run``` and ```ninja.jpa.persistence_unit_name``` (including those that are prefixed with ```%prod```, ```%dev``` or ```%test```). **This is a very important as Ninja web framework's JPA module will interfere with the multi-tenant workings**.

2. Copy the ```multitenantdb``` package in ```src/main/java``` in this demo to your application's code base.

3. Implement your **TenantCatalog** class implement the methods which return the database information for the requested tenant, and to return a list of valid tenant identifiers. Depending on your application, the tenant information can reside in a configuration file, or on a database. You can refer to **conf.MyTenantCatalog** class in the demo to see how to connect to another database to query for the tenant database information.

4. Bind your **TenantCatalog** implementation and install the **MultiTenantModule** module in **conf.Module** class. Refer to the Module class in the demo for more information.

5. Implement a Ninja filter to call **TenantDatabaseProvider.beginWorkUnitForTenant()** and **TenantDatabaseProvider.endWorkUnit()** before and after each request processing. You can refer to **conf.MyFilter** class in the demo. Depending on your application, your filter class can be added as a global filter in **conf.Filters** or for each controller class at class-level.

6. Check all the methods in your DAO and controller classes. Any method that uses **EntityManager** instance must retrieve the **EntityManager** instance via the injected **Provider< EntityManager >** attribute. You can see the **AccountDAO** class in the demo for more details.

7. Check all scheduled tasks. Any task that accesses the database must call **TenantDatabaseProvider.beginWorkUnitForTenant()** method for each tenant before making any database queries/updates, and to end the work unit by calling  **TenantDatabaseProvider.endWorkUnit()**

# Other matters

- Each tenant must be identified by an unique case-sensitive identifier. The identifier should preferably be limited to ASCII printable characters.
- All tenant databases are upgraded via flyway migration when the Ninja web framework starts up. Depending on the number of tenant databases, this may not be desirable as it can prolong the startup time before the application is ready to process its first request.
- Tenant databases that are added after the framework startup will not be migrated. You can improve **TenantDatabaseProvider** class to overcome this limitation.
