package schedules;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dao.TaskDAO;
import models.Task;
import multitenantdb.TenantCatalog;
import multitenantdb.TenantDatabaseProvider;
import multitenantdb.exceptions.InvalidTenantException;
import multitenantdb.exceptions.TenantException;
import ninja.scheduler.Schedule;

@Singleton
public class CompletionWorker {

	final Logger logger = LoggerFactory.getLogger(CompletionWorker.class);

	@Inject
	TenantCatalog tenantCatalog;

	@Inject
	TenantDatabaseProvider tenantDatabaseProvider;

	@Inject
	TaskDAO taskDAO;

	@Schedule(delay = 60, initialDelay = 30, timeUnit = TimeUnit.SECONDS)
	public void completeTasks() {
		List<String> tenantIdentifiers = null;
		try {
			tenantIdentifiers = tenantCatalog.listTenants();
		} catch (Exception ex) {
			logger.error("completeTasks: Cannot retrieve list of tenant identifiers", ex);
		}

		for (String tenantIdentifier : tenantIdentifiers) {
			try {
				// [MultiTenantDB]: You must always start work unit for each tenant before making any database queries
				tenantDatabaseProvider.beginWorkUnitForTenant(tenantIdentifier);

				Date cutoffTime = new Date(System.currentTimeMillis() - 30*1000); // Tasks that are created 30 secs ago will be marked as completed.
				try {
					List<Task> uncompletedTasks = taskDAO.listUncompletedTask();
					for (Task task : uncompletedTasks) {
						if (cutoffTime.after(task.createTime)) {
							taskDAO.completeTask(task);
						}
					}
				} finally {
					// [MultiTenantDB]: You must always end the work unit properly by calling endWorkUnit()
					tenantDatabaseProvider.endWorkUnit();
				}
			} catch (TenantException e) {
				// [MultiTenantDB]: Thrown when there is an error attempting to retrieve the tenant database information
				// or when connecting to the tenant database
				logger.error("completeTasks: Error retrieving tenant database information", e);
			} catch (InvalidTenantException e) {
				// [MultiTenantDB]: Thrown when there is no database information for this tenant
				// Should not happen as the list of identifiers was just retrieved from the catalog
			}
		}
	}
}
