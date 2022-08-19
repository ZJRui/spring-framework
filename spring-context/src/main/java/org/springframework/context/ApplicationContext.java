/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * Central interface to provide configuration for an application.
 * This is read-only while the application is running, but may be
 * reloaded if the implementation supports this.
 *
 * <p>An ApplicationContext provides:
 * <ul>
 * <li>Bean factory methods for accessing application components.
 * Inherited from {@link org.springframework.beans.factory.ListableBeanFactory}.
 * <li>The ability to load file resources in a generic fashion.
 * Inherited from the {@link org.springframework.core.io.ResourceLoader} interface.
 * <li>The ability to publish events to registered listeners.
 * Inherited from the {@link ApplicationEventPublisher} interface.
 * <li>The ability to resolve messages, supporting internationalization.
 * Inherited from the {@link MessageSource} interface.
 * <li>Inheritance from a parent context. Definitions in a descendant context
 * will always take priority. This means, for example, that a single parent
 * context can be used by an entire web application, while each servlet has
 * its own child context that is independent of that of any other servlet.
 * </ul>
 *
 * <p>In addition to standard {@link org.springframework.beans.factory.BeanFactory}
 * lifecycle capabilities, ApplicationContext implementations detect and invoke
 * {@link ApplicationContextAware} beans as well as {@link ResourceLoaderAware},
 * {@link ApplicationEventPublisherAware} and {@link MessageSourceAware} beans.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {
	/**
	 *
	 * 1. spring 为什么可以直接@Autowired ApplicationContext
	 * https://www.cnblogs.com/emanlee/p/15759135.html
	 *
	 * //ERROR No qualifying bean of type 'org.springframework.context.ApplicationContext' available
	 * applicationContext.getBean(ApplicationContext.class);
	 *
	 * //SUCCESS
	 * @Component
	 * public class SimpleBean3 {
	 *     @Autowired
	 *     private ApplicationContext applicationContext;
	 *     @Autowired
	 *     private SimpleBean2 simpleBean2;
	 * }
	 * 复制代码
	 *
	 *
	 * ApplicationContext是Spring中的重要组件,它不是bean,因此无法通过getBean获取它,但是可以通过Autowired注入获得,其中必定有特殊的处理。
	 *
	 * 普通Bean的元数据存放在DefaultListableBeanFactory的beanDefinitionNames和beanDefinitionMap,普通Bean通过遵照Spring提供的机制自动注册添加,这是Spring提供的功能。
	 *
	 *
	 * private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
	 * private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
	 *
	 * ApplicationContext和BeanFactory存储在DefaultListableBeanFactory的resolvableDependencies,它们需要手动注册添加,这是Spring的框架内部逻辑
	 *
	 *
	 * private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);
	 *
	 * 在查找依赖时,会同时搜寻beanDefinitionNames和resolvableDependencies,因此ApplicationContext也能被查找到。
	 *
	 * 而getBean时只会查找上面的BeanDefinitionMap,因此找不到ApplicationContext。
	 *
	 *
	 *
	 * 注入流程
	 * 注册 ApplicationContext 为 resolvableDependencies
	 *
	 * 在 AbstractApplicationContext.prepareBeanFactory() 中, ApplicationContext 被注册到 resolvableDependencies 中。
	 *
	 * 复制代码
	 * protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	 *         //...忽略部分代码
	 *
	 *         // BeanFactory interface not registered as resolvable type in a plain factory.
	 *         // MessageSource registered (and found for autowiring) as a bean.
	 *         beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
	 *         beanFactory.registerResolvableDependency(ResourceLoader.class, this);
	 *         beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
	 *         beanFactory.registerResolvableDependency(ApplicationContext.class, this);
	 *         //...忽略部分代码
	 *     }
	 * 复制代码
	 *
	 * 生成Bean时查找依赖
	 *
	 *
	 *
	 * 带有 @Autowired 字段的在 AutowiredAnnotationPostProcessor.postProcessProperties() 中完成注入,查找依赖的入口就在 metadata.inject(bean, beanName, pvs)
	 *
	 * 复制代码
	 *     public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
	 *         InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
	 *         try {
	 *             //### 注入 ###
	 *             metadata.inject(bean, beanName, pvs);
	 *         }
	 *         catch (BeanCreationException ex) {
	 *             throw ex;
	 *         }
	 *         catch (Throwable ex) {
	 *             throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
	 *         }
	 *         return pvs;
	 *     }
	 * 复制代码
	 *
	 *
	 *
	 *
	 *
	 */

	/**
	 * Return the unique id of this application context.
	 * @return the unique id of the context, or {@code null} if none
	 */
	@Nullable
	String getId();

	/**
	 * Return a name for the deployed application that this context belongs to.
	 * @return a name for the deployed application, or the empty String by default
	 */
	String getApplicationName();

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	String getDisplayName();

	/**
	 * Return the timestamp when this context was first loaded.
	 * @return the timestamp (ms) when this context was first loaded
	 */
	long getStartupDate();

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * and this is the root of the context hierarchy.
	 * @return the parent context, or {@code null} if there is no parent
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * Expose AutowireCapableBeanFactory functionality for this context.
	 * <p>This is not typically used by application code, except for the purpose of
	 * initializing bean instances that live outside of the application context,
	 * applying the Spring bean lifecycle (fully or partly) to them.
	 * <p>Alternatively, the internal BeanFactory exposed by the
	 * {@link ConfigurableApplicationContext} interface offers access to the
	 * {@link AutowireCapableBeanFactory} interface too. The present method mainly
	 * serves as a convenient, specific facility on the ApplicationContext interface.
	 * <p><b>NOTE: As of 4.2, this method will consistently throw IllegalStateException
	 * after the application context has been closed.</b> In current Spring Framework
	 * versions, only refreshable application contexts behave that way; as of 4.2,
	 * all application context implementations will be required to comply.
	 * @return the AutowireCapableBeanFactory for this context
	 * @throws IllegalStateException if the context does not support the
	 * {@link AutowireCapableBeanFactory} interface, or does not hold an
	 * autowire-capable bean factory yet (e.g. if {@code refresh()} has
	 * never been called), or if the context has been closed already
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
