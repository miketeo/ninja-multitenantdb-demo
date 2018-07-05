package multitenantdb;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

class TenantLocalTxnInterceptor implements MethodInterceptor {

	@Inject
	TenantDatabaseProvider emProvider;

	@Transactional
	private static class Internal {}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		Transactional transactional = readTransactionMetadata(methodInvocation);
		EntityManager em = this.emProvider.get();

		// Allow 'joining' of transactions if there is an enclosing @Transactional method.
		if (em.getTransaction().isActive()) {
			return methodInvocation.proceed();
		}

		final EntityTransaction txn = em.getTransaction();
		txn.begin();

		Object result;
		try {
			result = methodInvocation.proceed();
		} catch (Exception e) {
			//commit transaction only if rollback didnt occur
			if (rollbackIfNecessary(transactional, e, txn)) {
				txn.commit();
			}

			//propagate whatever exception is thrown anyway
			throw e;
		}

		txn.commit();

		//or return result
		return result;
	}

	private Transactional readTransactionMetadata(MethodInvocation methodInvocation) {
		Transactional transactional;
		Method method = methodInvocation.getMethod();
		Class<?> targetClass = methodInvocation.getThis().getClass();

		transactional = method.getAnnotation(Transactional.class);
		if (null == transactional) {
			// If none on method, try the class.
			transactional = targetClass.getAnnotation(Transactional.class);
		}
		if (null == transactional) {
			// If there is no transactional annotation present, use the default
			transactional = Internal.class.getAnnotation(Transactional.class);
		}

		return transactional;
	}

	private boolean rollbackIfNecessary(Transactional transactional, Exception e, EntityTransaction txn) {
		boolean commit = true;

		//check rollback clauses
		for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

			//if one matched, try to perform a rollback
			if (rollBackOn.isInstance(e)) {
				commit = false;

				//check ignore clauses (supercedes rollback clause)
				for (Class<? extends Exception> exceptOn : transactional.ignore()) {
					//An exception to the rollback clause was found, DON'T rollback
					// (i.e. commit and throw anyway)
					if (exceptOn.isInstance(e)) {
						commit = true;
						break;
					}
				}

				//rollback only if nothing matched the ignore check
				if (!commit) {
					txn.rollback();
				}
				//otherwise continue to commit

				break;
			}
		}

		return commit;
	}
}
