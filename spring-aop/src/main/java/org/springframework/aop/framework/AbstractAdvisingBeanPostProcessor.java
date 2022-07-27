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

package org.springframework.aop.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;

/**
 * Base class for {@link BeanPostProcessor} implementations that apply a
 * Spring AOP {@link Advisor} to specific beans.
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
@SuppressWarnings("all")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {
	/**
	 * Base class for BeanPostProcessor implementations that apply a
	 * Spring AOP Advisor to specific beans.
	 *
	 * 对特定bean应用Spring AOP Advisor的BeanPostProcessor实现的基类。
	 *
	 *
	 * 我们知道  判断一个Bean是否需要被增强是在Bean 实例化之后， 在执行org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 * 的时候进行 wrapIfNecessary的判断。 这就要求我们首先需要开启AOP， 开启AOP之后会注入 aop的BeanPostProcessor，
	 * 然后再bean 创建之后进行拦截 判断是否需要代理。
	 *
	 * 对于 @Async这个 标记方法异步执行的的实现是这样的：
	 * （1）@EnableAsync 注解会通过 importSelector 导入一个配置ProxyAsyncConfiguration，
	 * 这个配置类里面会注入一个AsyncAnnotationBeanPostProcessor， 这个beanPostprocessor的
	 * org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 * 方法内部会创建Advisor 和pointcut,pointcut的判断条件就是判断 方法上是否存在@Async注解。
	 * 然后在业务Bean 被创建后执行  AsyncAnnotationBeanPostProcessor的 postProcessAfterInitialization 的时候判断是否增强，生成代理
	 * 我们要关注的是 AsyncAnnotationAdvisor中的advice ，这个advice就是 AnnotationAsyncExecutionInterceptor，他拦截到方法执行交给
	 * 线程池执行
	 *
	 *
	 *
	 *
	 */

	@Nullable
	protected Advisor advisor;

	protected boolean beforeExistingAdvisors = false;

	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);


	/**
	 * Set whether this post-processor's advisor is supposed to apply before
	 * existing advisors when encountering a pre-advised object.
	 * <p>Default is "false", applying the advisor after existing advisors, i.e.
	 * as close as possible to the target method. Switch this to "true" in order
	 * for this post-processor's advisor to wrap existing advisors as well.
	 * <p>Note: Check the concrete post-processor's javadoc whether it possibly
	 * changes this flag by default, depending on the nature of its advisor.
	 */
	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
		/**
		 * 设置当遇到预先通知的对象时，是否应该在现有的建议程序之前应用此后处理器的建议程序。
		 * 默认为“false”，将advisor应用在现有advisor之后，即尽可能接近目标方法。
		 * 将它切换为“true”，以便这个后处理器的顾问也可以包装现有的顾问。
		 * 注意:检查具体的后处理器的javadoc是否可能在默认情况下改变这个标志，这取决于它的顾问的性质。
		 */
		this.beforeExistingAdvisors = beforeExistingAdvisors;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (this.advisor == null || bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}

		if (bean instanceof Advised) {
			Advised advised = (Advised) bean;
			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
				// Add our local Advisor to the existing proxy's Advisor chain...
				if (this.beforeExistingAdvisors) {
					advised.addAdvisor(0, this.advisor);
				}
				else {
					advised.addAdvisor(this.advisor);
				}
				return bean;
			}
		}

		/**
		 * 判断bean是否是被被 aop，也就是判断 poincut是否可以应用
		 * 	eligible = AopUtils.canApply(this.advisor, targetClass);
		 *
		 *
		 * 	参考： org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
		 * 	#postProcessAfterInitialization(java.lang.Object, java.lang.String)--->wrapIfNecessary
		 */
		if (isEligible(bean, beanName)) {
			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
			if (!proxyFactory.isProxyTargetClass()) {
				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
			}
			proxyFactory.addAdvisor(this.advisor);
			customizeProxyFactory(proxyFactory);
			return proxyFactory.getProxy(getProxyClassLoader());
		}

		// No proxy needed.
		return bean;
	}

	/**
	 * Check whether the given bean is eligible for advising with this
	 * post-processor's {@link Advisor}.
	 * <p>Delegates to {@link #isEligible(Class)} for target class checking.
	 * Can be overridden e.g. to specifically exclude certain beans by name.
	 * <p>Note: Only called for regular bean instances but not for existing
	 * proxy instances which implement {@link Advised} and allow for adding
	 * the local {@link Advisor} to the existing proxy's {@link Advisor} chain.
	 * For the latter, {@link #isEligible(Class)} is being called directly,
	 * with the actual target class behind the existing proxy (as determined
	 * by {@link AopUtils#getTargetClass(Object)}).
	 * @param bean the bean instance
	 * @param beanName the name of the bean
	 * @see #isEligible(Class)
	 */
	protected boolean isEligible(Object bean, String beanName) {

		/**
		 *  isEligible: 符合条件
		 * 检查给定bean是否有资格使用此后处理器的Advisor提供建议。
		 * 委托isEligible(Class)用于目标类检查。可以被重写，例如通过名字来排除特定的bean。
		 * 注意:只调用常规bean实例，不调用实现advise并允许将本地Advisor添加到现有代理的
		 * Advisor链的现有代理实例。对于后者，直接调用isEligible(Class)，使用现有代理后
		 * 面的实际目标类(由AopUtils.getTargetClass(Object)确定)。
		 *
		 *
		 *
		 */
		return isEligible(bean.getClass());
	}

	/**
	 * Check whether the given class is eligible for advising with this
	 * post-processor's {@link Advisor}.
	 * <p>Implements caching of {@code canApply} results per bean target class.
	 * @param targetClass the class to check against
	 * @see AopUtils#canApply(Advisor, Class)
	 */
	protected boolean isEligible(Class<?> targetClass) {
		Boolean eligible = this.eligibleBeans.get(targetClass);
		if (eligible != null) {
			return eligible;
		}
		if (this.advisor == null) {
			return false;
		}
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(targetClass, eligible);
		return eligible;
	}

	/**
	 * Prepare a {@link ProxyFactory} for the given bean.
	 * <p>Subclasses may customize the handling of the target instance and in
	 * particular the exposure of the target class. The default introspection
	 * of interfaces for non-target-class proxies and the configured advisor
	 * will be applied afterwards; {@link #customizeProxyFactory} allows for
	 * late customizations of those parts right before proxy creation.
	 * @param bean the bean instance to create a proxy for
	 * @param beanName the corresponding bean name
	 * @return the ProxyFactory, initialized with this processor's
	 * {@link ProxyConfig} settings and the specified bean
	 * @since 4.2.3
	 * @see #customizeProxyFactory
	 */
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);
		proxyFactory.setTarget(bean);
		return proxyFactory;
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 * @param proxyFactory the ProxyFactory that is already configured with
	 * target, advisor and interfaces and will be used to create the proxy
	 * immediately after this method returns
	 * @since 4.2.3
	 * @see #prepareProxyFactory
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}

}
