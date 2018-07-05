package dao;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import dao.exceptions.AccountNotFoundException;
import models.Account;

public class AccountDAO {

	final Logger logger = LoggerFactory.getLogger(AccountDAO.class);

	// [multitenantdb]: The EntityManager instance must be retrieved from the injected provider.
	@Inject
	Provider<EntityManager> entitiyManagerProvider;

	@Transactional
	public Account get(String accountID) throws AccountNotFoundException {
		EntityManager entityManager = entitiyManagerProvider.get();

		TypedQuery<Account> q = entityManager.createQuery("SELECT x FROM Account x WHERE x.id=:accountID", Account.class);
		q.setParameter("accountID", accountID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			throw new AccountNotFoundException(accountID);
		}
	}

	@Transactional
	public Account getOrCreate(String accountID) {
		EntityManager entityManager = entitiyManagerProvider.get();

		TypedQuery<Account> q = entityManager.createQuery("SELECT x FROM Account x WHERE x.id=:accountID", Account.class);
		q.setParameter("accountID", accountID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			Account account = new Account();
			account.id = accountID;
			account.createTime = new Date();
			entityManager.persist(account);
			logger.info("getOrCreate: Created new account '{}'", accountID);
			return account;
		}
	}

	@Transactional
	public void updateAccount(Account account) {
		EntityManager entityManager = entitiyManagerProvider.get();
		entityManager.merge(account);
		logger.info("updateAccount: Account '{}' has been updated", account.id);
	}
}
