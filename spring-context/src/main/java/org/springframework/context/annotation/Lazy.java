/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates whether a bean is to be lazily initialized.
 *
 * <p>May be used on any class directly or indirectly annotated with {@link
 * org.springframework.stereotype.Component @Component} or on methods annotated with
 * {@link Bean @Bean}.
 *
 * <p>If this annotation is not present on a {@code @Component} or {@code @Bean} definition,
 * eager initialization will occur. If present and set to {@code true}, the {@code @Bean} or
 * {@code @Component} will not be initialized until referenced by another bean or explicitly
 * retrieved from the enclosing {@link org.springframework.beans.factory.BeanFactory
 * BeanFactory}. If present and set to {@code false}, the bean will be instantiated on
 * startup by bean factories that perform eager initialization of singletons.
 *
 * <p>If Lazy is present on a {@link Configuration @Configuration} class, this
 * indicates that all {@code @Bean} methods within that {@code @Configuration}
 * should be lazily initialized. If {@code @Lazy} is present and false on a {@code @Bean}
 * method within a {@code @Lazy}-annotated {@code @Configuration} class, this indicates
 * overriding the 'default lazy' behavior and that the bean should be eagerly initialized.
 *
 * <p>In addition to its role for component initialization, this annotation may also be placed
 * on injection points marked with {@link org.springframework.beans.factory.annotation.Autowired}
 * or {@link javax.inject.Inject}: In that context, it leads to the creation of a
 * lazy-resolution proxy for all affected dependencies, as an alternative to using
 * {@link org.springframework.beans.factory.ObjectFactory} or {@link javax.inject.Provider}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Primary
 * @see Bean
 * @see Configuration
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SuppressWarnings("all")
public @interface Lazy {
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

	/**
	 * Whether lazy initialization should occur.
	 */
	boolean value() default true;

}
