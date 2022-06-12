/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.scope;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for creating a scoped proxy.
 *
 * <p>Used by ScopedProxyBeanDefinitionDecorator and ClassPathBeanDefinitionScanner.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
public abstract class ScopedProxyUtils {

	private static final String TARGET_NAME_PREFIX = "scopedTarget.";

	private static final int TARGET_NAME_PREFIX_LENGTH = TARGET_NAME_PREFIX.length();


	/**
	 * Generate a scoped proxy for the supplied target bean, registering the target
	 * bean with an internal name and setting 'targetBeanName' on the scoped proxy.
	 *
	 * @param definition       the original bean definition
	 * @param registry         the bean definition registry
	 * @param proxyTargetClass whether to create a target class proxy
	 * @return the scoped proxy definition
	 * @see #getTargetBeanName(String)
	 * @see #getOriginalBeanName(String)
	 */
	public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,
														 BeanDefinitionRegistry registry, boolean proxyTargetClass) {

		/**
		 *
		 * 这个方法是什么意思呢？
		 * 就是说， 你有一个原始的BeanDefinition 注册到了容器中
		 * 然后 我通过这个createScopedProxy方法 会创建一个新的beanDefiniton，这个新的beanDefinition实际上是对 原Definition的装饰
		 *proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
		 *
		 * 要注意的是 这个新Def的类是 ScopedProxyFactoryBean ，并且新Def以相同的名字注册到容器中
		 * 因此从容器中getBean得到的是一个ScopedProxyFactoryBean ，他是一个工厂bean(FactoryBean)
		 * 因此我们会通过他的getObject方法获取 工厂bean生产的对象。
		 *
		 * 这个工厂生产的对象是一个代理对象（在ScopedProxyFactoryBean 类的setBeanFactory 方法中可以看到），这个代理对象的增强逻辑是
		 * DelegatingIntroductionInterceptor ，这个增强逻辑中持有 ScopedObject 对象。具体参考ScopedProxyFactoryBean
		 *
		 */

		String originalBeanName = definition.getBeanName();
		/**
		 * 注意这里 targetDefinition 就是原来的Definition
		 */
		BeanDefinition targetDefinition = definition.getBeanDefinition();
		/**
		 * 注意这里的区别， originalBeanName 表示的是初始入参BeanDefinition中定义的beanname
		 *
		 * 然后 这里的targetBeanName 实际上是 scopedTarget.originalBeanName
		 *
		 * 这个targetBeanName  和 proxyDefinition  构成键值对注册到 容器中
		 *
		 */
		String targetBeanName = getTargetBeanName(originalBeanName);

		// Create a scoped proxy definition for the original bean name,
		// "hiding" the target bean in an internal target definition.
		RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
		proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));
		proxyDefinition.setOriginatingBeanDefinition(targetDefinition);
		proxyDefinition.setSource(definition.getSource());
		proxyDefinition.setRole(targetDefinition.getRole());

		/**
		 * 注意这里添加了一个 Properties属性， 这个属性的名称是 targetBeanName， value是 targetBeanName， 这个
		 * targetBeanName 是 "scopedTarget."+originalBeanName
		 *
		 * 下面创建了一个ProxyBeanDefinition， 一般情况下 当前方法返回值holder ，外部会将这个holder 和holder中的beanName注册到 spring容器中
		 * 而holder中的beanName是originalBeanName ，因此就实现了   originalBeanName和proxyBeanDefinition  注册到容器中。
		 *
		 * 当业务从容器中get originalBeanName的时候 会返回 proxyBeanDefinition 定义的ScopedProxyFactoryBean 对象
		 * 然后对这个ScopedProxyFactoryBean 对象的属性填充的时候 会 调用ScopedProxyFactoryBean 对象的 setTargetBeanName 方法
		 *而这个setTargetBeanName 方法的参数 就是从这里的 添加到「ProxyBeanDefinition 中提取的。因此方法参数就是"scopedTarget."+originalBeanName
		 *
		 * 在ScopedProxyFactoryBean 的setTargetBeanName方法中，这个参数 会被保存到 ScopedProxyFactoryBean对象的scopedTargetSource 成员对象属性中。
		 *
		 * scopedTargetSource的getTarget 方法会根据 名称"scopedTarget."+originalBeanName 从ioc容器中获取到 原始的BeanDefinition定义的bean对象，也就是target对象
		 *
		 */
		proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);
		if (proxyTargetClass) {
			targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ScopedProxyFactoryBean's "proxyTargetClass" default is TRUE, so we don't need to set it explicitly here.
		} else {
			proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);
		}

		// Copy autowire settings from original bean definition.
		proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
		proxyDefinition.setPrimary(targetDefinition.isPrimary());
		if (targetDefinition instanceof AbstractBeanDefinition) {
			proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
		}

		// The target bean should be ignored in favor of the scoped proxy.
		targetDefinition.setAutowireCandidate(false);
		targetDefinition.setPrimary(false);

		/**
		 * 将  scopedTarget.originalBeanName 和 原来的 definition 注册到 容器中
		 *
		 * 同时 创建一个 将 originalBeanName 和proxyDefinition 作为 一对 构建一个 BeanDefinitionHolder 作为方法返回值。
		 *
		 * 这段代码的意思是什么呢？
		 *
		 * 假设我容器中有一个 studentService的BeanDefinition(作为TargetBeanDefinition)。 通过这里的createScopedProxy方法
		 *
		 * 会首先创建一个ProxyBeanDefinition 作为对TargetBeanDefinition的装饰。
		 *
		 * 然后 将 新的BeanName scopedTarget.studentService 和TargetBeanDefinition 注册到容器中。
		 * 然后将 原来的beanName 和ProxyBeanDefinition 构建一个BeanDefinition
		 * 最终外部 拿到 这个holder后 会将 《studentService，ProxyBeanDefinition》 注册到容器中。
		 *
		 * 因此当你从容器中 get  studentServie的时候实际上 会创建一个ProxyBeanDefinition 指定的ScopedProxyFactoryBean
		 * 对象， 这个对象是一个工厂bean, 工厂的getObject方法返回的是一个代理对象。
		 * 这里代理对象的增强逻辑是DelegatingIntroductionInterceptor
		 *
		 * 而 这个 增强逻辑DelegatingIntroductionInterceptor 对象会持有原始BeanDefinition对应的Bean对象
		 * ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName());
		 * pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject));
		 *
		 *  在创建ScopedObject 的时候  this.scopedTargetSource.getTargetBeanName()  返回的值就是 scopedTarget.studentService
		 *  ScopeObject的get方法会通过 名称scopedTarget.studentService 从容器中获取到原始的BeanDefinition代表的对象。
		 *
		 * 因此DelegatingIntroductionInterceptor 增强逻辑就 持有了 原始BeanDefinition定义的的目标对象。
		 *
		 * 这样就完成了对BeanDefinition的代理。
		 *
		 *
		 *
		 */
		// Register the target bean as separate bean in the factory.
		registry.registerBeanDefinition(targetBeanName, targetDefinition);

		// Return the scoped proxy definition as primary bean definition
		// (potentially an inner bean).
		return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());
	}

	/**
	 * Generate the bean name that is used within the scoped proxy to reference the target bean.
	 *
	 * @param originalBeanName the original name of bean
	 * @return the generated bean to be used to reference the target bean
	 * @see #getOriginalBeanName(String)
	 */
	public static String getTargetBeanName(String originalBeanName) {
		return TARGET_NAME_PREFIX + originalBeanName;
	}

	/**
	 * Get the original bean name for the provided {@linkplain #getTargetBeanName
	 * target bean name}.
	 *
	 * @param targetBeanName the target bean name for the scoped proxy
	 * @return the original bean name
	 * @throws IllegalArgumentException if the supplied bean name does not refer
	 *                                  to the target of a scoped proxy
	 * @see #getTargetBeanName(String)
	 * @see #isScopedTarget(String)
	 * @since 5.1.10
	 */
	public static String getOriginalBeanName(String targetBeanName) {
		Assert.isTrue(isScopedTarget(targetBeanName), () -> "bean name '" +
				targetBeanName + "' does not refer to the target of a scoped proxy");
		return targetBeanName.substring(TARGET_NAME_PREFIX_LENGTH);
	}

	/**
	 * Specify if the {@code beanName} is the name of a bean that references the target
	 * bean within a scoped proxy.
	 *
	 * @since 4.1.4
	 */
	public static boolean isScopedTarget(@Nullable String beanName) {
		return (beanName != null && beanName.startsWith(TARGET_NAME_PREFIX));
	}

}
