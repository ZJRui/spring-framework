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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Complete implementation of the
 * {@link org.springframework.beans.factory.support.AutowireCandidateResolver} strategy
 * interface, providing support for qualifier annotations as well as for lazy resolution
 * driven by the {@link Lazy} annotation in the {@code context.annotation} package.
 *
 * 支持autowirecanddateresolver策略接口的完整实现，
 * 提供了对限定符注释以及上下文中由lazy注释驱动的惰性解析的支持。注释包。
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ContextAnnotationAutowireCandidateResolver extends QualifierAnnotationAutowireCandidateResolver {

	@Override
	@Nullable
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return (isLazy(descriptor) ? buildLazyResolutionProxy(descriptor, beanName) : null);
	}

	protected boolean isLazy(DependencyDescriptor descriptor) {
		for (Annotation ann : descriptor.getAnnotations()) {
			Lazy lazy = AnnotationUtils.getAnnotation(ann, Lazy.class);
			if (lazy != null && lazy.value()) {
				return true;
			}
		}
		MethodParameter methodParam = descriptor.getMethodParameter();
		if (methodParam != null) {
			Method method = methodParam.getMethod();
			if (method == null || void.class == method.getReturnType()) {
				Lazy lazy = AnnotationUtils.getAnnotation(methodParam.getAnnotatedElement(), Lazy.class);
				if (lazy != null && lazy.value()) {
					return true;
				}
			}
		}
		return false;
	}

	protected Object buildLazyResolutionProxy(final DependencyDescriptor descriptor, final @Nullable String beanName) {
		/**
		 *
		 * spring 的延迟加载
		 * （1） 如果你的Service不被任何 其他Bean依赖。 spring容器在启动阶段 会判断这个bean是否开启了懒加载 lay-init为true或者@Lazy标记类
		 * <bean id="XXX" class="XXX.XXX.XXXX" lazy-init="true"/>
		 * 那么容器启动阶段将不会 创建Bean对象。
		 *
		 * （2） 如果 你的Bean 即便 使用了@Lazy标记，但是你的bean 被@Autowired到了 另外一个 BeanB中，而这个BeanB没有懒加载，BeanB在容器启动
		 * 阶段就会被创建 ，那么这个时候 在 填充BeanB 的@Autowired属性的时候 就会 通过getBean触发 你的Bean的创建，因此懒加载失效了
		 *
		 * （3）如果 你的@Lazy 标记class 的bean 被@Autowired，但是同时使用了@Lazy。 如下，那么这个时候 ControllerA对象的创建并不会立即
		 * 创建HelloService对象， 这个处理逻辑在 AutowiredAnnotationBeanPostProcessor 实现@Autowired的时候会分析 是否是懒加载注入。
		 * @RestController
		 * public class ControllerA {
		 *     private String name;
		 *
		 *     @Autowired
		 *     @Lazy
		 *     HelloService helloService;
		 *   }
		 *
		 *
		 *   那么 如何实现懒加载呢？代码参考： org.springframework.context.annotation.
		 *   ContextAnnotationAutowireCandidateResolver#buildLazyResolutionProxy(org.springframework.beans.factory.config.DependencyDescriptor, java.lang.String)
		 *
		 *   就是如果发现@Autowired 同时使用了@Lazy，那么就创建一个代理对象
		 *   	ProxyFactory pf = new ProxyFactory();
		 * 		pf.setTargetSource(ts);
		 * 		Class<?> dependencyType = descriptor.getDependencyType();
		 * 		if (dependencyType.isInterface()) {
		 * 			pf.addInterface(dependencyType);
		 *                }
		 * 		return pf.getProxy(dlbf.getBeanClassLoader());
		 *
		 *这个ProxyFactory在创建代理对象的时候 会指定一个拦截器
		 * org.springframework.aop.framework.CglibAopProxy.DynamicAdvisedInterceptor
		 * 在这个拦截器中 使用targetSource 执行getTarget ：target = targetSource.getTarget();
		 * 而这个TargetSource的getTarget方法中  会 通过spring容器的解析 ControllerA的依赖 触发 helloService 对象的创建。
		 *
		 * Object target = dlbf.doResolveDependency(descriptor, beanName=“ControllerA”, autowiredBeanNames, null);
		 *
		 *
		 *
		 *
		 *
		 *
		 *
		 *
		 */
		BeanFactory beanFactory = getBeanFactory();
		Assert.state(beanFactory instanceof DefaultListableBeanFactory,
				"BeanFactory needs to be a DefaultListableBeanFactory");
		final DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;

		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return descriptor.getDependencyType();
			}
			@Override
			public boolean isStatic() {
				return false;
			}
			@Override
			public Object getTarget() {
				Set<String> autowiredBeanNames = (beanName != null ? new LinkedHashSet<>(1) : null);
				Object target = dlbf.doResolveDependency(descriptor, beanName, autowiredBeanNames, null);
				if (target == null) {
					Class<?> type = getTargetClass();
					if (Map.class == type) {
						return Collections.emptyMap();
					}
					else if (List.class == type) {
						return Collections.emptyList();
					}
					else if (Set.class == type || Collection.class == type) {
						return Collections.emptySet();
					}
					throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
							"Optional dependency not present for lazy injection point");
				}
				if (autowiredBeanNames != null) {
					for (String autowiredBeanName : autowiredBeanNames) {
						if (dlbf.containsBean(autowiredBeanName)) {
							dlbf.registerDependentBean(autowiredBeanName, beanName);
						}
					}
				}
				return target;
			}
			@Override
			public void releaseTarget(Object target) {
			}
		};

		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		Class<?> dependencyType = descriptor.getDependencyType();
		if (dependencyType.isInterface()) {
			pf.addInterface(dependencyType);
		}
		return pf.getProxy(dlbf.getBeanClassLoader());
	}

}
