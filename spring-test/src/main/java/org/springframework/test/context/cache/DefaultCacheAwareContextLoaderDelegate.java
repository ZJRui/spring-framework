/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link CacheAwareContextLoaderDelegate} interface.
 *
 * <p>To use a static {@code DefaultContextCache}, invoke the
 * {@link #DefaultCacheAwareContextLoaderDelegate()} constructor; otherwise,
 * invoke the {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)}
 * and provide a custom {@link ContextCache} implementation.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);

	/**
	 * Default static cache of Spring application contexts.
	 */
	static final ContextCache defaultContextCache = new DefaultContextCache();

	private final ContextCache contextCache;


	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
	 * a static {@link DefaultContextCache}.
	 * <p>This default cache is static so that each context can be cached
	 * and reused for all subsequent tests that declare the same unique
	 * context configuration within the same JVM process.
	 * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
	 */
	public DefaultCacheAwareContextLoaderDelegate() {
		this(defaultContextCache);
	}

	/**
	 * Construct a new {@code DefaultCacheAwareContextLoaderDelegate} using
	 * the supplied {@link ContextCache}.
	 * @see #DefaultCacheAwareContextLoaderDelegate()
	 */
	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}

	/**
	 * Get the {@link ContextCache} used by this context loader delegate.
	 */
	protected ContextCache getContextCache() {
		return this.contextCache;
	}

	/**
	 * Load the {@code ApplicationContext} for the supplied merged context configuration.
	 * <p>Supports both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
	 * @throws Exception if an error occurs while loading the application context
	 */
	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {

		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
				"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

		ApplicationContext applicationContext;

		/**
		 *
		 SpringJunit4ClassRunner 创建 TestContextManager。
		 TestContextManager 创建一个TestContextBootstrapper。
		 TestContextBootstrapper 对象内有一个buildTestContext方法来创建一个TestContext对象


		 TestContext 对象中有一个getApplicationContext方法来获取 ApplicationContext
		 testContext对象中有getTestClass 和getTestInstance方法来获取测试类的类和测试类示例对象。
		 TestContext对象中有一个getTestmethod方法 返回Method 对象表示要测试的方法。


		 TestContextManager的构造器 中 会调用TestContextBootstrapper的buildTestContext 方法来 创建一个 DefaultTestContext 对象。
		 我们说TestContext 中有一个getApplicationContext方法能够返回一个ApplicationContext。

		 TestContextManager 在 通过 TestContextBootstrapper的buildTestContext方法创建DefaultTestContext时会 先准备Configuration。 这个configuration时通过解析 TestClass 测试类上的@ContextConfiguration 注解得到的


		 实际上 TestContext对象内部包含了三部分信息
		 （1）TestClass （包括 TestInstance 示例 和TestMethod）， TestContext 方法提供了updateState 方法来更新自身内部的 TestInstance和 TestMethod方法
		 （2）configuration： 通过解析TestClass上的@ContextConfiguration得到
		 （3）CacheAwareContextLoaderDelegate   TestContext对外提供getApplicationContext方法。 内部实现时委托给
		 CacheAwareContextLoaderDelegate去实现的。 cacheAwareContextLoaderDelegate 对象提供了loadContext方法加载TestContext的配置信息 创建 ApplicationContext。 CacheAwareContextLoaderDelegate的loadContext方法内部做了缓存，因此对相同的configuration 并不户重复创建ApplicationiContext对象。

		 DefaultCacheAwareContextLoaderDelegate对象 在loadContextInternal的时候 会根据configration 来获取一个ContextLoader。
		 然后使用这个ContextLoader来 loadContext

		 普通的ContextLoader 就是创建 GenericApplicationContext  ，然后 loadBeanDefinitions（configs）
		 在SpringBoot中提供了一个SpringBootContextLoader ，这个contextLoader的loadContext方法内部不是直接创建spring容器，而是创建 SpringApplication 执行application.run(args); 来走了SpringBoot的那套流程。
		 */

		if (contextLoader instanceof SmartContextLoader) {
			SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
			applicationContext = smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
					"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
			applicationContext = contextLoader.loadContext(locations);
		}

		return applicationContext;
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadContextInternal(mergedContextConfiguration);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Storing ApplicationContext in cache under key [%s]",
								mergedContextConfiguration));
					}
					this.contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Retrieved ApplicationContext from cache with key [%s]",
							mergedContextConfiguration));
				}
			}

			this.contextCache.logStatistics();

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode) {
		synchronized (this.contextCache) {
			this.contextCache.remove(mergedContextConfiguration, hierarchyMode);
		}
	}

}
