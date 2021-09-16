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

package org.springframework.transaction.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans
 * necessary to enable proxy-based annotation-driven transaction management.
 *
 * @author Chris Beams
 * @author Sebastien Deleuze
 * @since 3.1
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyTransactionManagementConfiguration extends org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration {


	/**
	 * 我们分析下这个@Bean是如何实现注入Bean的？ 首先@Bean注解判断是否存在 的方法是org.springframework.context.annotation.BeanAnnotationHelper#isBeanAnnotated(java.lang.reflect.Method)
	 *
	 * @Bean注解的方法被拦截是在 ：ConfigurationClassEnhancer.BeanMethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], MethodProxy)
	 *
	 * 在下面的调用栈中，首先是执行getBean， 这个里的transactionAdvisor 最终会被包装成工厂方法来创建bean，因此getBean最终会调用AbstractBeanFactory$1.getObject()来获取bean
	 * 在工厂方法的getObject内会执行SimpleInstantiationStrategy  instantiate 来实例化bean，在instantiate方法中 执行了 factoryMethod.invoke(factoryBean, args);
	 * 也就是具体执行这里的工厂方法transactionAdvisor， 从堆栈信息来看，在执行 transactionAdvisor之前 先执行了了拦截器ConfigurationClassEnhancer$BeanMethodInterceptor的intercept方法
	 *
	 * 在创建@Configuration标注的 类对象的时候会执行 org.springframework.context.annotation.ConfigurationClassPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)
	 * 在这个postProcessBeanFactory方法中会执行 org.springframework.context.annotation.ConfigurationClassPostProcessor#enhanceConfigurationClasses(ConfigurableListableBeanFactory)
	 *
	 * 在这个enhanceConfigurationClasses方法中会创建，对这个Configuration  Class 进行增强。 增强的属性中指定了callback，
	 * ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
	 * Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
	 *
	 * 在这个Callback数组中有两个重要的拦截器： BeanMethodInterceptor和BeanFactoryAwareMethodInterceptor， 这就导致增强后的Configuration class执行 原来Configuration
	 * class中的方法的时候会被增强。
	 *
	 *
	 * class ConfigurationClassEnhancer {
	 *
	 * 	// The callbacks to use. Note that these callbacks must be stateless.
	 * 	private static final Callback[] CALLBACKS = new Callback[] {
	 * 			new BeanMethodInterceptor(),
	 * 			new BeanFactoryAwareMethodInterceptor(),
	 * 			NoOp.INSTANCE
	 *        };
	 *
	 *  }
	 *
	 * org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[ ], org.springframework.cglib.proxy.MethodProxy)
	 * org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration$$EnhancerBySpringCGLIB$$5cecf520.transactionAdvisor()
	 * org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(org.springframework.beans.factory.support.RootBeanDefinition, java.lang.String, org.springframework.beans.factory.BeanFactory, java.lang.Object, java.lang.reflect.Method, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(java.lang.String, org.springframework.beans.factory.support.RootBeanDefinition, java.lang.Object[ ])
	 * org.springframework.beans.factory.support.AbstractBeanFactory$1.getObject()
	 * org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(java.lang.String, org.springframework.beans.factory.ObjectFactory)
	 * org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(java.lang.String, java.lang.Class, java.lang.Object[ ], boolean)
	 * org.springframework.beans.factory.support.AbstractBeanFactory.getBean(java.lang.String, java.lang.Class)
	 *
	 *
	 * @param transactionAttributeSource
	 * @param transactionInterceptor
	 * @return
	 */
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
			TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {

		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
		advisor.setTransactionAttributeSource(transactionAttributeSource);
		advisor.setAdvice(transactionInterceptor);
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource);
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
