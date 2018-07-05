package dao;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import dao.exceptions.TaskNotFoundException;
import models.Account;
import models.Task;

public class TaskDAO {

	final Logger logger = LoggerFactory.getLogger(TaskDAO.class);

	// [multitenantdb]: The EntityManager instance must be retrieved from the injected provider.
	@Inject
	Provider<EntityManager> entitiyManagerProvider;

	@Transactional
	public List<Task> listUncompletedTask() {
		EntityManager entityManager = entitiyManagerProvider.get();

		TypedQuery<Task> q = entityManager.createQuery("SELECT x FROM Task x JOIN FETCH x.account WHERE x.hasCompleted=false", Task.class);
		return q.getResultList();
	}

	@Transactional
	public void completeTask(Task task) {
		if (task.hasCompleted) {
			throw new IllegalArgumentException(String.format("completeTask: Task (id:%d) has already been completed", task.id));
		}

		EntityManager entityManager = entitiyManagerProvider.get();
		task.hasCompleted = true;
		task.completionTime = new Date();
		entityManager.merge(task);
		logger.info("completeTask: Task (id:{} accountID:{}) is marked as completed", task.id, task.account.id);
	}

	@Transactional
	public Task getTask(int taskID) throws TaskNotFoundException {
		EntityManager entityManager = entitiyManagerProvider.get();
		TypedQuery<Task> q = entityManager.createQuery("SELECT x FROM Task x JOIN FETCH x.account WHERE x.id=:id", Task.class);
		q.setParameter("id", taskID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			throw new TaskNotFoundException(taskID);
		}
	}

	@Transactional
	public List<Task> listTasksForAccount(Account account) {
		if (account==null) {
			throw new IllegalArgumentException("listTasksForAccount: account parameter cannot be null");
		}

		EntityManager entityManager = entitiyManagerProvider.get();
		TypedQuery<Task> q = entityManager.createQuery("SELECT x FROM Task x WHERE x.account=:account ORDER BY x.createTime DESC", Task.class);
		q.setParameter("account", account);
		return q.getResultList();
	}

	@Transactional
	public Task createTaskForAccount(Account account, String description) {
		if (account==null) {
			throw new IllegalArgumentException("createTaskForAccount: account parameter cannot be null");
		}
		if (Strings.isNullOrEmpty(description)) {
			throw new IllegalArgumentException("createTaskForAccount: description parameter cannot be null/empty");
		}

		EntityManager entityManager = entitiyManagerProvider.get();
		Task task = new Task();
		task.account = account;
		task.description = description;
		task.createTime = new Date();
		entityManager.persist(task);

		logger.info("createTaskForAccount: Task (id:{}) created for account '{}'", task.id, task.account.id);
		return task;
	}

	@Transactional
	public void deleteTask(Task task) {
		EntityManager entityManager = entitiyManagerProvider.get();
		entityManager.remove(task);
		logger.info("deleteTask: Task (id:{} accountID:{}) deleted", task.id, task.account.id);
	}
}
