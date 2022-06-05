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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link TestContext} interface.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 4.0
 */
public class DefaultTestContext implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>(4);

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	@Nullable
	private volatile Object testInstance;

	@Nullable
	private volatile Method testMethod;

	@Nullable
	private volatile Throwable testException;


	/**
	 * <em>Copy constructor</em> for creating a new {@code DefaultTestContext}
	 * based on the <em>attributes</em> and immutable state of the supplied context.
	 * <p><em>Immutable state</em> includes all arguments supplied to the
	 * {@linkplain #DefaultTestContext(Class, MergedContextConfiguration,
	 * CacheAwareContextLoaderDelegate) standard constructor}.
	 * @throws NullPointerException if the supplied {@code DefaultTestContext}
	 * is {@code null}
	 */
	public DefaultTestContext(DefaultTestContext testContext) {
		this(testContext.testClass, testContext.mergedContextConfiguration,
			testContext.cacheAwareContextLoaderDelegate);
		this.attributes.putAll(testContext.attributes);
	}

	/**
	 * Construct a new {@code DefaultTestContext} from the supplied arguments.
	 * @param testClass the test class for this test context
	 * @param mergedContextConfiguration the merged application context
	 * configuration for this test context
	 * @param cacheAwareContextLoaderDelegate the delegate to use for loading
	 * and closing the application context for this test context
	 */
	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedContextConfiguration,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		Assert.notNull(testClass, "Test Class must not be null");
		Assert.notNull(mergedContextConfiguration, "MergedContextConfiguration must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * Get the {@linkplain ApplicationContext application context} for this
	 * test context.
	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
	 * that was supplied when this {@code TestContext} was constructed.
	 * @throws IllegalStateException if the context returned by the context
	 * loader delegate is not <em>active</em> (i.e., has been closed).
	 * @see CacheAwareContextLoaderDelegate#loadContext
	 */
	public ApplicationContext getApplicationContext() {

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
		 在SpringBoot中提供了一个SpringBootContextLoader ，这个contextLoader的loadContext方法内部不是直接创建spring容器，而是创建 SpringApplication 执行application.run(args); 来走了SpringBoot的那套流程

		 */

		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		if (context instanceof ConfigurableApplicationContext) {
			@SuppressWarnings("resource")
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			Assert.state(cac.isActive(), () ->
					"The ApplicationContext loaded for [" + this.mergedContextConfiguration +
					"] is not active. This may be due to one of the following reasons: " +
					"1) the context was closed programmatically by user code; " +
					"2) the context was closed during parallel test execution either " +
					"according to @DirtiesContext semantics or due to automatic eviction " +
					"from the ContextCache due to a maximum cache size policy.");
		}
		return context;
	}

	/**
	 * Mark the {@linkplain ApplicationContext application context} associated
	 * with this test context as <em>dirty</em> (i.e., by removing it from the
	 * context cache and closing it).
	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
	 * that was supplied when this {@code TestContext} was constructed.
	 * @see CacheAwareContextLoaderDelegate#closeContext
	 */
	public void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode) {
		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedContextConfiguration, hierarchyMode);
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	public final Object getTestInstance() {
		Object testInstance = this.testInstance;
		Assert.state(testInstance != null, "No test instance");
		return testInstance;
	}

	public final Method getTestMethod() {
		Method testMethod = this.testMethod;
		Assert.state(testMethod != null, "No test method");
		return testMethod;
	}

	@Override
	@Nullable
	public final Throwable getTestException() {
		return this.testException;
	}

	public void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.attributes) {
			if (value != null) {
				this.attributes.put(name, value);
			}
			else {
				this.attributes.remove(name);
			}
		}
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.remove(name);
	}

	@Override
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.containsKey(name);
	}

	@Override
	public String[] attributeNames() {
		synchronized (this.attributes) {
			return StringUtils.toStringArray(this.attributes.keySet());
		}
	}


	/**
	 * Provide a String representation of this test context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("testInstance", this.testInstance)
				.append("testMethod", this.testMethod)
				.append("testException", this.testException)
				.append("mergedContextConfiguration", this.mergedContextConfiguration)
				.append("attributes", this.attributes)
				.toString();
	}

}
