/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionManager;
import org.springframework.util.ObjectUtils;

/**
 * Abstract class that implements a Pointcut that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
@SuppressWarnings("serial")
abstract class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	protected TransactionAttributeSourcePointcut() {
		setClassFilter(new TransactionAttributeSourceClassFilter());
	}


	/**
	 *
	 * @param method
	 * @param targetClass
	 * @return
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		/**
		 *
		 * 这个地方调用了 getTransactionAttributeSource
		 *
		 *
		 *
		 * 	 *
		 * 	 *2.2% - 2,928 bytes - 122 alloc. org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean
		 * 	 *   56.2% - 2,280 bytes - 95 alloc. org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessAfterInitialization
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.wrapIfNecessary
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator.getAdvicesAndAdvisorsForBean
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator.findEligibleAdvisors
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator.findAdvisorsThatCanApply
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.support.AopUtils.findAdvisorsThatCanApply
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.support.AopUtils.canApply
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.aop.support.AopUtils.canApply
		 * 	 ========================  这个地方调用  TransactionAttributeSourcePointcut.matches，也就是当前方法
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.transaction.interceptor.TransactionAttributeSourcePointcut.matches
		 * 	 *   =============================== 这个地方调用了 getTransactionAttribute
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource.getTransactionAttribute
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource.getCacheKey
		 * 	 *   53.8% - 2,184 bytes - 91 alloc. org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource$DefaultCacheKey.<init>
		 * 	 *
		 * 	 *
		 *
		 */
		org.springframework.transaction.interceptor.TransactionAttributeSource tas = getTransactionAttributeSource();
		return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TransactionAttributeSourcePointcut)) {
			return false;
		}
		TransactionAttributeSourcePointcut otherPc = (TransactionAttributeSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), otherPc.getTransactionAttributeSource());
	}

	@Override
	public int hashCode() {
		return TransactionAttributeSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getTransactionAttributeSource();
	}


	/**
	 * Obtain the underlying TransactionAttributeSource (may be {@code null}).
	 * To be implemented by subclasses.
	 */
	@Nullable
	protected abstract org.springframework.transaction.interceptor.TransactionAttributeSource getTransactionAttributeSource();


	/**
	 * {@link ClassFilter} that delegates to {@link TransactionAttributeSource#isCandidateClass}
	 * for filtering classes whose methods are not worth searching to begin with.
	 */
	private class TransactionAttributeSourceClassFilter implements ClassFilter {

		@Override
		public boolean matches(Class<?> clazz) {
			if (org.springframework.transaction.interceptor.TransactionalProxy.class.isAssignableFrom(clazz) ||
					TransactionManager.class.isAssignableFrom(clazz) ||
					PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
				return false;
			}
			org.springframework.transaction.interceptor.TransactionAttributeSource tas = getTransactionAttributeSource();
			return (tas == null || tas.isCandidateClass(clazz));
		}
	}

}
