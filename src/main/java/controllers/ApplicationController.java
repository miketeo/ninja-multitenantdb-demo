
package controllers;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import dao.AccountDAO;
import dao.TaskDAO;
import filters.SessionFilter;
import models.Account;
import models.Task;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.ReverseRouter;
import ninja.params.Param;
import ninja.session.Session;

public class ApplicationController {

	@Inject
	ReverseRouter reverseRouter;

	@Inject
	AccountDAO accountDAO;

	@Inject
	TaskDAO taskDAO;

	final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

	public Result indexPage(Context context) {
		return Results.html().doNotCacheContent();
	}

	public Result login(Context context, Session session, @Param("accountID") String accountID) {

		Account account = accountDAO.getOrCreate(accountID);
		account.lastLoginTime = new Date();
		accountDAO.updateAccount(account);
		logger.info("login: Account '{}' has signed in", account.id);

		session.put(SessionFilter.ACCOUNT_ID, accountID);
		return reverseRouter.with(ApplicationController::tasksPage).redirect().doNotCacheContent();
	}

	public Result logout(Context context, Session session) {
		session.clear();
		return reverseRouter.with(ApplicationController::indexPage).redirect().doNotCacheContent();
	}

	@FilterWith(SessionFilter.class)
	public Result tasksPage(Context context) {
		Account account = context.getAttribute(SessionFilter.CONTEXT_ACCOUNT, Account.class);
		List<Task> tasks = taskDAO.listTasksForAccount(account);

		return Results.html().doNotCacheContent().render("tasks", tasks);
	}
}
