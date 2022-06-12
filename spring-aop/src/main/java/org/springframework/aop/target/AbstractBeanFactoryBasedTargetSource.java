/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.target;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.ObjectUtils;

/**
 * Base class for {@link org.springframework.aop.TargetSource} implementations
 * that are based on a Spring {@link org.springframework.beans.factory.BeanFactory},
 * delegating to Spring-managed bean instances.
 *
 * <p>Subclasses can create prototype instances or lazily access a
 * singleton target, for example. See {@link LazyInitTargetSource} and
 * {@link AbstractPrototypeBasedTargetSource}'s subclasses for concrete strategies.
 *
 * <p>BeanFactory-based TargetSources are serializable. This involves
 * disconnecting the current target and turning into a {@link SingletonTargetSource}.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.1.4
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @see LazyInitTargetSource
 * @see PrototypeTargetSource
 * @see ThreadLocalTargetSource
 * @see CommonsPool2TargetSource
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
public abstract class AbstractBeanFactoryBasedTargetSource implements TargetSource, BeanFactoryAware, Serializable {

	/** use serialVersionUID from Spring 1.2.7 for interoperability. */
	private static final long serialVersionUID = -4721607536018568393L;


	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Name of the target bean we will create on each invocation. */
	private String targetBeanName;

	/** Class of the target. */
	private volatile Class<?> targetClass;

	/**
	 * BeanFactory that owns this TargetSource. We need to hold onto this
	 * reference so that we can create new prototype instances as necessary.
	 */
	private BeanFactory beanFactory;


	/**
	 * Set the name of the target bean in the factory.
	 * <p>The target bean should not be a singleton, else the same instance will
	 * always be obtained from the factory, resulting in the same behavior as
	 * provided by {@link SingletonTargetSource}.
	 * @param targetBeanName name of the target bean in the BeanFactory
	 * that owns this interceptor
	 * @see SingletonTargetSource
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Return the name of the target bean in the factory.
	 */
	public String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * Specify the target class explicitly, to avoid any kind of access to the
	 * target bean (for example, to avoid initialization of a FactoryBean instance).
	 * <p>Default is to detect the type automatically, through a {@code getType}
	 * call on the BeanFactory (or even a full {@code getBean} call as fallback).
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * Set the owning BeanFactory. We need to save a reference so that we can
	 * use the {@code getBean} method on every invocation.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.targetBeanName == null) {
			throw new IllegalStateException("Property 'targetBeanName' is required");
		}
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the owning BeanFactory.
	 */
	public BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public Class<?> getTargetClass() {


		/**
		 *
		 * 理解这个 getTargetClass参考 org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy(org.springframework.beans.factory.config.BeanDefinitionHolder, org.springframework.beans.factory.support.BeanDefinitionRegistry, boolean)
		 *
		 * 在createScopedProxy中我们看到了 会在 ProxyBeanDefinition 中保存一个 targetBeanName
		 * proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
		 *
		 * 这个targetBeanName 就是 scopedTarget.originalBeanName
		 * 在这里 我们想获取 目标对象的class，
		 *
		 *
		 *
		 * getTargetClass:133, AbstractBeanFactoryBasedTargetSource (org.springframework.aop.target)
		 * getTargetClass:156, AdvisedSupport (org.springframework.aop.framework)
		 * createAopProxy:58, DefaultAopProxyFactory (org.springframework.aop.framework)
		 * createAopProxy:105, ProxyCreatorSupport (org.springframework.aop.framework)
		 * getProxy:110, ProxyFactory (org.springframework.aop.framework)
		 * setBeanFactory:117, ScopedProxyFactoryBean (org.springframework.aop.scope) ----------》 填充Bean对象的Aware属性， 在setBeanFactory的过程中创建  代理对象
		 * invokeAwareMethods:1826, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * initializeBean:1791, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * doCreateBean:620, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * createBean:542, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * lambda$doGetBean$0:335, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getObject:-1, 657069980 (org.springframework.beans.factory.support.AbstractBeanFactory$$Lambda$550)
		 * getSingleton:234, DefaultSingletonBeanRegistry (org.springframework.beans.factory.support)
		 * doGetBean:333, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getBean:208, AbstractBeanFactory (org.springframework.beans.factory.support)-------------》  创建ScopedProxyFactoryBean 对象 之后 对这个对象执行BeanFactoryAware接口
		 *
		 *
		 */


		Class<?> targetClass = this.targetClass;
		if (targetClass != null) {
			return targetClass;
		}
		synchronized (this) {
			// Full check within synchronization, entering the BeanFactory interaction algorithm only once...
			targetClass = this.targetClass;
			if (targetClass == null && this.beanFactory != null) {

				//从容器中 根据beanname获取  类型， 如果获取不到， 则根据beanName获取bean
				// Determine type of the target bean.
				targetClass = this.beanFactory.getType(this.targetBeanName);
				if (targetClass == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Getting bean with name '" + this.targetBeanName + "' for type determination");
					}
					/**
					 * 根据 scopedTarget.originalName 从容器中获取  target对象， 然后 获取器class作为targetClass
					 */
					Object beanInstance = this.beanFactory.getBean(this.targetBeanName);
					targetClass = beanInstance.getClass();
				}
				this.targetClass = targetClass;
			}
			return targetClass;
		}
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public void releaseTarget(Object target) throws Exception {
		// Nothing to do here.
	}


	/**
	 * Copy configuration from the other AbstractBeanFactoryBasedTargetSource object.
	 * Subclasses should override this if they wish to expose it.
	 * @param other object to copy configuration from
	 */
	protected void copyFrom(AbstractBeanFactoryBasedTargetSource other) {
		this.targetBeanName = other.targetBeanName;
		this.targetClass = other.targetClass;
		this.beanFactory = other.beanFactory;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractBeanFactoryBasedTargetSource otherTargetSource = (AbstractBeanFactoryBasedTargetSource) other;
		return (ObjectUtils.nullSafeEquals(this.beanFactory, otherTargetSource.beanFactory) &&
				ObjectUtils.nullSafeEquals(this.targetBeanName, otherTargetSource.targetBeanName));
	}

	@Override
	public int hashCode() {
		int hashCode = getClass().hashCode();
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.beanFactory);
		hashCode = 13 * hashCode + ObjectUtils.nullSafeHashCode(this.targetBeanName);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" for target bean '").append(this.targetBeanName).append("'");
		if (this.targetClass != null) {
			sb.append(" of type [").append(this.targetClass.getName()).append("]");
		}
		return sb.toString();
	}

}
