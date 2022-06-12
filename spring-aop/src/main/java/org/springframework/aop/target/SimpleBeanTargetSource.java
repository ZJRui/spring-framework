/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Simple {@link org.springframework.aop.TargetSource} implementation,
 * freshly obtaining the specified target bean from its containing
 * Spring {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Can obtain any kind of target bean: singleton, scoped, or prototype.
 * Typically used for scoped beans.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class SimpleBeanTargetSource extends AbstractBeanFactoryBasedTargetSource {

	@Override
	public Object getTarget() throws Exception {
		/**
		 * 参考org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy(
		 * org.springframework.beans.factory.config.BeanDefinitionHolder, org.springframework.beans.factory.support.BeanDefinitionRegistry, boolean)
		 *
		 * 问题： 为什么  对于一个SimpleBeanTargetSource对象， 每次他的 getTarget方法都要从容器中获取一次呢？
		 * 毕竟容器中getBean是一个比较费时的操作。 实际上是因为  不同Scope的bean  getBean可能会返回不同的Bean对象，
		 * 所以必须要每次都从 容器中getBean
		 *
		 */
		return getBeanFactory().getBean(getTargetBeanName());
	}

}
