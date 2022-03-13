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

package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy implementation for parsing Spring's {@link Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	@Override
	@Nullable
	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
		/**
		 * element 就是class
		 *
		 * findMergedAnnotationAttributes:565, AnnotatedElementUtils (org.springframework.core.annotation)
		 * //下面的就是 当前的方法 parseTransactionAnnotation
		 * parseTransactionAnnotation:50, SpringTransactionAnnotationParser (org.springframework.transaction.annotation)
		 * determineTransactionAttribute:175, AnnotationTransactionAttributeSource (org.springframework.transaction.annotation)
		 * findTransactionAttribute:153, AnnotationTransactionAttributeSource (org.springframework.transaction.annotation)
		 * computeTransactionAttribute:168, AbstractFallbackTransactionAttributeSource (org.springframework.transaction.interceptor)
		 * getTransactionAttribute:112, AbstractFallbackTransactionAttributeSource (org.springframework.transaction.interceptor)
		 * matches:47, TransactionAttributeSourcePointcut (org.springframework.transaction.interceptor)
		 * canApply:252, AopUtils (org.springframework.aop.support)
		 * canApply:289, AopUtils (org.springframework.aop.support)
		 * findAdvisorsThatCanApply:321, AopUtils (org.springframework.aop.support)
		 * findAdvisorsThatCanApply:128, AbstractAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
		 * findEligibleAdvisors:97, AbstractAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
		 * getAdvicesAndAdvisorsForBean:78, AbstractAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
		 * wrapIfNecessary:347, AbstractAutoProxyCreator (org.springframework.aop.framework.autoproxy)
		 * postProcessAfterInitialization:299, AbstractAutoProxyCreator (org.springframework.aop.framework.autoproxy)
		 * applyBeanPostProcessorsAfterInitialization:430, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * initializeBean:1798, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * doCreateBean:594, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * createBean:516, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * lambda$doGetBean$0:324, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getObject:-1, 27131477 (org.springframework.beans.factory.support.AbstractBeanFactory$$Lambda$168)
		 * getSingleton:234, DefaultSingletonBeanRegistry (org.springframework.beans.factory.support)
		 * doGetBean:322, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getBean:202, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * resolveCandidate:276, DependencyDescriptor (org.springframework.beans.factory.config)
		 * doResolveDependency:1307, DefaultListableBeanFactory (org.springframework.beans.factory.support)
		 * resolveDependency:1227, DefaultListableBeanFactory (org.springframework.beans.factory.support)
		 * inject:640, AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement (org.springframework.beans.factory.annotation)
		 * inject:130, InjectionMetadata (org.springframework.beans.factory.annotation)
		 * postProcessProperties:399, AutowiredAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
		 * populateBean:1420, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * doCreateBean:593, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * createBean:516, AbstractAutowireCapableBeanFactory (org.springframework.beans.factory.support)
		 * lambda$doGetBean$0:324, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getObject:-1, 27131477 (org.springframework.beans.factory.support.AbstractBeanFactory$$Lambda$168)
		 * getSingleton:234, DefaultSingletonBeanRegistry (org.springframework.beans.factory.support)
		 * doGetBean:322, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * getBean:202, AbstractBeanFactory (org.springframework.beans.factory.support)
		 * preInstantiateSingletons:897, DefaultListableBeanFactory (org.springframework.beans.factory.support)
		 * finishBeanFactoryInitialization:879, AbstractApplicationContext (org.springframework.context.support)
		 * __refresh:551, AbstractApplicationContext (org.springframework.context.support)
		 * jrLockAndRefresh:40002, AbstractApplicationContext (org.springframework.context.support)
		 * refresh:41008, AbstractApplicationContext (org.springframework.context.support)
		 * refresh:143, ServletWebServerApplicationContext (org.springframework.boot.web.servlet.context)
		 * refresh:758, SpringApplication (org.springframework.boot)
		 * refresh:750, SpringApplication (org.springframework.boot)
		 * refreshContext:397, SpringApplication (org.springframework.boot)
		 * run:315, SpringApplication (org.springframework.boot)
		 * run:1237, SpringApplication (org.springframework.boot)
		 * run:1226, SpringApplication (org.springframework.boot)
		 * main:12, DemoApplication (com.example.demo)
		 *
		 */
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
				element, Transactional.class, false, false);
		if (attributes != null) {
			return parseTransactionAnnotation(attributes);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
		return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
	}

	protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

		Propagation propagation = attributes.getEnum("propagation");
		rbta.setPropagationBehavior(propagation.value());
		Isolation isolation = attributes.getEnum("isolation");
		rbta.setIsolationLevel(isolation.value());
		rbta.setTimeout(attributes.getNumber("timeout").intValue());
		rbta.setReadOnly(attributes.getBoolean("readOnly"));
		rbta.setQualifier(attributes.getString("value"));

		List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
		for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
			rollbackRules.add(new RollbackRuleAttribute(rbRule));
		}
		for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
			rollbackRules.add(new RollbackRuleAttribute(rbRule));
		}
		for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
		}
		for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
		}
		rbta.setRollbackRules(rollbackRules);

		return rbta;
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof SpringTransactionAnnotationParser);
	}

	@Override
	public int hashCode() {
		return SpringTransactionAnnotationParser.class.hashCode();
	}

}
