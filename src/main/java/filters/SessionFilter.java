package filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import controllers.ApplicationController;
import dao.AccountDAO;
import dao.exceptions.AccountNotFoundException;
import models.Account;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.ReverseRouter;
import ninja.session.Session;

public class SessionFilter implements Filter {

	public static final String ACCOUNT_ID = "accountID";
	public static final String CONTEXT_ACCOUNT = "account";

	final Logger logger = LoggerFactory.getLogger(SessionFilter.class);

	@Inject
	ReverseRouter reverseRouter;

	@Inject
	AccountDAO accountDAO;

	@Override
	public Result filter(FilterChain chain, Context context) {
		String accountID = null;

		Session session = context.getSession();
		if (session!=null) {
			accountID = session.get(ACCOUNT_ID);
		}

		if (!Strings.isNullOrEmpty(accountID)) {
			try {
				Account account = accountDAO.get(accountID);
				context.setAttribute(CONTEXT_ACCOUNT, account);

				return chain.next(context);
			} catch (AccountNotFoundException e) {
				session.clear();
				logger.warn("filter: Cannot locate accountID '{}' although it is specified in the session cookie", accountID);

				return reverseRouter.with(ApplicationController::indexPage).redirect();
			}
		} else {
			logger.warn("filter: No account for current session. Redirecting to login page...");
			return reverseRouter.with(ApplicationController::indexPage).redirect();
		}
	}

}
