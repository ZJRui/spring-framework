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

package org.springframework.test.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestContextManager} is the main entry point into the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Specifically, a {@code TestContextManager} is responsible for managing a
 * single {@link TestContext} and signaling events to all registered
 * {@link TestExecutionListener TestExecutionListeners} at the following test
 * execution points:
 *
 * <ul>
 * <li>{@link #beforeTestClass() before test class execution}: prior to any
 * <em>before class callbacks</em> of a particular testing framework (e.g.,
 * JUnit 4's {@link org.junit.BeforeClass @BeforeClass})</li>
 * <li>{@link #prepareTestInstance test instance preparation}:
 * immediately following instantiation of the test class</li>
 * <li>{@link #beforeTestMethod before test setup}:
 * prior to any <em>before method callbacks</em> of a particular testing framework
 * (e.g., JUnit 4's {@link org.junit.Before @Before})</li>
 * <li>{@link #beforeTestExecution before test execution}:
 * immediately before execution of the {@linkplain java.lang.reflect.Method
 * test method} but after test setup</li>
 * <li>{@link #afterTestExecution after test execution}:
 * immediately after execution of the {@linkplain java.lang.reflect.Method
 * test method} but before test tear down</li>
 * <li>{@link #afterTestMethod(Object, Method, Throwable) after test tear down}:
 * after any <em>after method callbacks</em> of a particular testing
 * framework (e.g., JUnit 4's {@link org.junit.After @After})</li>
 * <li>{@link #afterTestClass() after test class execution}: after any
 * <em>after class callbacks</em> of a particular testing framework (e.g., JUnit 4's
 * {@link org.junit.AfterClass @AfterClass})</li>
 * </ul>
 *
 * <p>Support for loading and accessing
 * {@linkplain org.springframework.context.ApplicationContext application contexts},
 * dependency injection of test instances,
 * {@linkplain org.springframework.transaction.annotation.Transactional transactional}
 * execution of test methods, etc. is provided by
 * {@link SmartContextLoader ContextLoaders} and {@link TestExecutionListener
 * TestExecutionListeners}, which are configured via
 * {@link ContextConfiguration @ContextConfiguration} and
 * {@link TestExecutionListeners @TestExecutionListeners}.
 *
 * <p>Bootstrapping of the {@code TestContext}, the default {@code ContextLoader},
 * default {@code TestExecutionListeners}, and their collaborators is performed
 * by a {@link TestContextBootstrapper}, which is configured via
 * {@link BootstrapWith @BootstrapWith}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @see TestContext
 * @see TestExecutionListener
 * @see TestExecutionListeners
 * @see ContextConfiguration
 * @see ContextHierarchy
 */
public class TestContextManager {

	private static final Log logger = LogFactory.getLog(TestContextManager.class);

	private final TestContext testContext;

	private final ThreadLocal<TestContext> testContextHolder = ThreadLocal.withInitial(
			// Implemented as an anonymous inner class instead of a lambda expression due to a bug
			// in Eclipse IDE: "The blank final field testContext may not have been initialized"
			new Supplier<TestContext>() {
				@Override
				public TestContext get() {
					return copyTestContext(TestContextManager.this.testContext);
				}
			});

	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<>();


	/**
	 * Construct a new {@code TestContextManager} for the supplied {@linkplain Class test class}.
	 * <p>Delegates to {@link #TestContextManager(TestContextBootstrapper)} with
	 * the {@link TestContextBootstrapper} configured for the test class. If the
	 * {@link BootstrapWith @BootstrapWith} annotation is present on the test
	 * class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type;
	 * otherwise, the {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} will be used.
	 * @param testClass the test class to be managed
	 * @see #TestContextManager(TestContextBootstrapper)
	 */
	public TestContextManager(Class<?> testClass) {
		/**
		 * @SpringBootTest注解 定义了@BootstrapWith 指定一个TestContextBootstrapper
		 * 获取TestContextBootstrapper 从而创建 BootstrapContext
		 *
		 * BootstrapUtils  resolve解析到 TestContextBootstrapper 后 调用  this构造器
		 *
		 * 在TestContextManager的 构造器中执行
		 * this.testContext = testContextBootstrapper.buildTestContext();
		 *
		 *
		 * 在buildTestContext的过程中 会执行 buildMergedContextConfiguration
		 *
		 * 然后 buildTestContext 返回 的TestContext 中有getApplicationContex方法
		 * 这个get方法  会执行 loadContext
		 * 		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		 *
		 *在loadContext中执行  通过SpringBootContextLoader的loadContext 方法创建 SpringApplication 并执行
		 * SpringBoot的启动
		 *
		 *
		 * ----------
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

		 *
		 *
		 *
		 */
		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
	}

	/**
	 * Construct a new {@code TestContextManager} using the supplied {@link TestContextBootstrapper}
	 * and {@linkplain #registerTestExecutionListeners register} the necessary
	 * {@link TestExecutionListener TestExecutionListeners}.
	 * <p>Delegates to the supplied {@code TestContextBootstrapper} for building
	 * the {@code TestContext} and retrieving the {@code TestExecutionListeners}.
	 * @param testContextBootstrapper the bootstrapper to use
	 * @see TestContextBootstrapper#buildTestContext
	 * @see TestContextBootstrapper#getTestExecutionListeners
	 * @see #registerTestExecutionListeners
	 */
	public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
		/**
		 *
		 * 这里创建创建 Test Context对象
		 *
		 *
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

		 *
		 */
		this.testContext = testContextBootstrapper.buildTestContext();
		registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
	}

	/**
	 * Get the {@link TestContext} managed by this {@code TestContextManager}.
	 */
	public final TestContext getTestContext() {
		return this.testContextHolder.get();
	}

	/**
	 * Register the supplied list of {@link TestExecutionListener TestExecutionListeners}
	 * by appending them to the list of listeners used by this {@code TestContextManager}.
	 * @see #registerTestExecutionListeners(TestExecutionListener...)
	 */
	public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
		registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[0]));
	}

	/**
	 * Register the supplied array of {@link TestExecutionListener TestExecutionListeners}
	 * by appending them to the list of listeners used by this {@code TestContextManager}.
	 */
	public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
		for (TestExecutionListener listener : testExecutionListeners) {
			if (logger.isTraceEnabled()) {
				logger.trace("Registering TestExecutionListener: " + listener);
			}
			this.testExecutionListeners.add(listener);
		}
	}

	/**
	 * Get the current {@link TestExecutionListener TestExecutionListeners}
	 * registered for this {@code TestContextManager}.
	 * <p>Allows for modifications, e.g. adding a listener to the beginning of the list.
	 * However, make sure to keep the list stable while actually executing tests.
	 */
	public final List<TestExecutionListener> getTestExecutionListeners() {
		return this.testExecutionListeners;
	}

	/**
	 * Get a copy of the {@link TestExecutionListener TestExecutionListeners}
	 * registered for this {@code TestContextManager} in reverse order.
	 */
	private List<TestExecutionListener> getReversedTestExecutionListeners() {
		List<TestExecutionListener> listenersReversed = new ArrayList<>(getTestExecutionListeners());
		Collections.reverse(listenersReversed);
		return listenersReversed;
	}

	/**
	 * Hook for pre-processing a test class <em>before</em> execution of any
	 * tests within the class. Should be called prior to any framework-specific
	 * <em>before class methods</em> (e.g., methods annotated with JUnit 4's
	 * {@link org.junit.BeforeClass @BeforeClass}).
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to pre-process the test class
	 * execution. If a listener throws an exception, however, the remaining
	 * registered listeners will <strong>not</strong> be called.
	 * @throws Exception if a registered TestExecutionListener throws an
	 * exception
	 * @since 3.0
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestClass() throws Exception {
		/**
		 * 在ParentRunner的classBlock方法中会 首先 通过childrenInvoker获得一个Statmenet。
		 * 然后对这个Statement进行包装，withBeforeClasses 方法包装后将会在 Statement之前之前执行@BeforeClass 注解标注对方法
		 * 同样对withAfterClasses 将获取@AfterClass注解。
		 *
		 *
		 *   protected Statement classBlock(final RunNotifier notifier) {
		 *         Statement statement = childrenInvoker(notifier);
		 *         if (!areAllChildrenIgnored()) {
		 *             statement = withBeforeClasses(statement);
		 *             statement = withAfterClasses(statement);
		 *             statement = withClassRules(statement);
		 *             statement = withInterruptIsolation(statement);
		 *         }
		 *         return statement;
		 *     }
		 *
		 *     当执行
		 *     beforeTestClass:205, TestContextManager (org.springframework.test.context)
		 * evaluate:60, RunBeforeTestClassCallbacks (org.springframework.test.context.junit4.statements)
		 * evaluate:70, RunAfterTestClassCallbacks (org.springframework.test.context.junit4.statements)
		 * evaluate:306, ParentRunner$3 (org.junit.runners)
		 * run:413, ParentRunner (org.junit.runners)
		 * run:190, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * run:137, JUnitCore (org.junit.runner)
		 * startRunnerWithArgs:69, JUnit4IdeaTestRunner (com.intellij.junit4)
		 * execute:38, IdeaTestRunner$Repeater$1 (com.intellij.rt.junit)
		 * repeat:11, TestsRepeater (com.intellij.rt.execution.junit)
		 * startRunnerWithArgs:35, IdeaTestRunner$Repeater (com.intellij.rt.junit)
		 * prepareStreamsAndStart:235, JUnitStarter (com.intellij.rt.junit)
		 * main:54, JUnitStarter (com.intellij.rt.junit)
		 *
		 * runner对run方法对时候 ，执行statement对evaluate 将会执行 这里对TestContextManager的beforeTestClass 方法
		 * 在这个beforeTestClass 方法中首先执行了 getTestContext方法。
		 *
		 * 这个getTestContext 方法内部 会从 ThreadLocal testContextHolder 中取值，从而触发testContextHolder的初始化。
		 * testContextHolder的初始化是浅拷贝了 当前TestContextManager的testContext对象
		 * copyTestContext(TestContextManager.this.testContext);
		 * copyTestContext方法内部会通过反射创建 新的DefaultTestContext对象，这个新的DefaultTestContext对象 内部持有的数据
		 * 和TestContextManager中的 testContext对象持有的数据相同。
		 *
		 *
		 *
		 *
		 *
		 * 问题 ： 这里为什么要考拷贝而不是直接使用 TestContextManager的 testContext
		 *  实际上 TestContext对象内部包含了三部分信息
		 *  （1）TestClass （包括 TestInstance 示例 和TestMethod）， TestContext 方法提供了updateState 方法来更新自身内部的 TestInstance和 TestMethod方法
		 *  （2）configuration： 通过解析TestClass上的@ContextConfiguration得到
		 *  （3）CacheAwareContextLoaderDelegate   TestContext对外提供getApplicationContext方法。 内部实现时委托给
		 *  CacheAwareContextLoaderDelegate去实现的。 cacheAwareContextLoaderDelegate 对象提供了loadContext方法加载TestContext的配置信息 创建 ApplicationContext。 CacheAwareContextLoaderDelegate的loadContext方法内部做了缓存，因此对相同的configuration 并不户重复创建ApplicationiContext对象。
		 *
		 *  DefaultCacheAwareContextLoaderDelegate对象 在loadContextInternal的时候 会根据configration 来获取一个ContextLoader。
		 *  然后使用这个ContextLoader来 loadContext
		 *
		 *  普通的ContextLoader 就是创建 GenericApplicationContext  ，然后 loadBeanDefinitions（configs）
		 *  在SpringBoot中提供了一个SpringBootContextLoader ，这个contextLoader的loadContext方法内部不是直接创建spring容器，而是创建 SpringApplication 执行application.run(args); 来走了SpringBoot的那套流程。
		 *
		 *  在执行beforeTestClass 时 TestContext 还没有 进行applicationContext的初始化。
		 *
		 *  我们使用 testContextHolder 这个ThreadLocal  为每一个线程 创建一个TestContext，然后让每个线程负责创建自己的Spring容器
		 *
		 *
		 *
		 *
		 *
		 */
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("beforeTestClass(): class [" + testClass.getName() + "]");
		}
		/**
		 * 注意这里 获取testContext的方式 ，是通过 testContextHolder这个threadLocal获取
		 *
		 * 通过 updateState 方法 将当前线程的 TestContex对象 内部持有的TestInstance 设置为null， testMethod也设置为null
		 *
		 *
		 * 问题： TestClass 测试实例的创建是在什么时候？ 答案是在methodBlock方法中会 首先通过反射创建一个TestInstance对象
		 * 然后将这个TestInstance对象和当前要测试的 method 一起封装成一个InvokeMethod Statement对象。
		 * 然后在createTest  创建测试实例对象之后会执行        getTestContextManager().prepareTestInstance(testInstance);
		 * 对testInstances进行依赖注入。依赖注入的前提条件是ApplicationContext准备好了，所以又会触发Spring容器的创建。
		 *
		 * <init>:25, TestConfigB (com.example.demo.configtest)
		 * newInstance0:-1, NativeConstructorAccessorImpl (sun.reflect)
		 * newInstance:62, NativeConstructorAccessorImpl (sun.reflect)
		 * newInstance:45, DelegatingConstructorAccessorImpl (sun.reflect)
		 * newInstance:423, Constructor (java.lang.reflect)
		 * createTest:250, BlockJUnit4ClassRunner (org.junit.runners)
		 * createTest:226, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * runReflectiveCall:289, SpringJUnit4ClassRunner$1 (org.springframework.test.context.junit4)
		 * run:12, ReflectiveCallable (org.junit.internal.runners.model)
		 * methodBlock:291, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * runChild:246, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * runChild:97, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * run:331, ParentRunner$4 (org.junit.runners)
		 * schedule:79, ParentRunner$1 (org.junit.runners)
		 * runChildren:329, ParentRunner (org.junit.runners)
		 * access$100:66, ParentRunner (org.junit.runners)
		 * evaluate:293, ParentRunner$2 (org.junit.runners)
		 * evaluate:61, RunBeforeTestClassCallbacks (org.springframework.test.context.junit4.statements)
		 * evaluate:70, RunAfterTestClassCallbacks (org.springframework.test.context.junit4.statements)
		 * evaluate:306, ParentRunner$3 (org.junit.runners)
		 * run:413, ParentRunner (org.junit.runners)
		 * run:190, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * run:137, JUnitCore (org.junit.runner)
		 *
		 *
		 *  下面这段代码是createTest创建 测试对象成功之后对其进行依赖注入，触发Spring容器的创建
		 * loadContext:120, DefaultCacheAwareContextLoaderDelegate (org.springframework.test.context.cache)
		 * getApplicationContext:124, DefaultTestContext (org.springframework.test.context.support)
		 * setUpRequestContextIfNecessary:190, ServletTestExecutionListener (org.springframework.test.context.web)
		 * prepareTestInstance:132, ServletTestExecutionListener (org.springframework.test.context.web)
		 * prepareTestInstance:248, TestContextManager (org.springframework.test.context)
		 * createTest:227, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 *
		 *
		 */
		getTestContext().updateState(null, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				/**
				 *
				 */
				testExecutionListener.beforeTestClass(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, "beforeTestClass", testExecutionListener, testClass);
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * Hook for preparing a test instance prior to execution of any individual
	 * test methods, for example for injecting dependencies, etc. Should be
	 * called immediately after instantiation of the test instance.
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance}.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to prepare the test instance. If a
	 * listener throws an exception, however, the remaining registered listeners
	 * will <strong>not</strong> be called.
	 * @param testInstance the test instance to prepare (never {@code null})
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #getTestExecutionListeners()
	 */
	public void prepareTestInstance(Object testInstance) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
		}
		getTestContext().updateState(testInstance, null, null);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.prepareTestInstance(getTestContext());
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
							"] to prepare test instance [" + testInstance + "]", ex);
				}
				ReflectionUtils.rethrowException(ex);
			}
		}
	}

	/**
	 * Hook for pre-processing a test <em>before</em> execution of <em>before</em>
	 * lifecycle callbacks of the underlying test framework &mdash; for example,
	 * setting up test fixtures, starting a transaction, etc.
	 * <p>This method <strong>must</strong> be called immediately prior to
	 * framework-specific <em>before</em> lifecycle callbacks (e.g., methods
	 * annotated with JUnit 4's {@link org.junit.Before @Before}). For historical
	 * reasons, this method is named {@code beforeTestMethod}. Since the
	 * introduction of {@link #beforeTestExecution}, a more suitable name for
	 * this method might be something like {@code beforeTestSetUp} or
	 * {@code beforeEach}; however, it is unfortunately impossible to rename
	 * this method due to backward compatibility concerns.
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance} and {@code testMethod}.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to perform its pre-processing.
	 * If a listener throws an exception, however, the remaining registered
	 * listeners will <strong>not</strong> be called.
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which is about to be executed on the
	 * test instance
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
		String callbackName = "beforeTestMethod";
		prepareForBeforeCallback(callbackName, testInstance, testMethod);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
			}
		}
	}

	/**
	 * Hook for pre-processing a test <em>immediately before</em> execution of
	 * the {@linkplain java.lang.reflect.Method test method} in the supplied
	 * {@linkplain TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called after framework-specific
	 * <em>before</em> lifecycle callbacks (e.g., methods annotated with JUnit 4's
	 * {@link org.junit.Before @Before}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance} and {@code testMethod}.
	 * <p>An attempt will be made to give each registered
	 * {@link TestExecutionListener} a chance to perform its pre-processing.
	 * If a listener throws an exception, however, the remaining registered
	 * listeners will <strong>not</strong> be called.
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which is about to be executed on the
	 * test instance
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 */
	public void beforeTestExecution(Object testInstance, Method testMethod) throws Exception {
		String callbackName = "beforeTestExecution";
		prepareForBeforeCallback(callbackName, testInstance, testMethod);

		for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
			try {
				testExecutionListener.beforeTestExecution(getTestContext());
			}
			catch (Throwable ex) {
				handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
			}
		}
	}

	/**
	 * Hook for post-processing a test <em>immediately after</em> execution of
	 * the {@linkplain java.lang.reflect.Method test method} in the supplied
	 * {@linkplain TestContext test context} &mdash; for example, for timing
	 * or logging purposes.
	 * <p>This method <strong>must</strong> be called before framework-specific
	 * <em>after</em> lifecycle callbacks (e.g., methods annotated with JUnit 4's
	 * {@link org.junit.After @After}).
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance}, {@code testMethod}, and {@code exception}.
	 * <p>Each registered {@link TestExecutionListener} will be given a chance
	 * to perform its post-processing. If a listener throws an exception, the
	 * remaining registered listeners will still be called. After all listeners
	 * have executed, the first caught exception will be rethrown with any
	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
	 * the first exception.
	 * <p>Note that registered listeners will be executed in the opposite
	 * order in which they were registered.
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which has just been executed on the
	 * test instance
	 * @param exception the exception that was thrown during execution of the
	 * test method or by a TestExecutionListener, or {@code null} if none
	 * was thrown
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @since 5.0
	 * @see #beforeTestMethod
	 * @see #afterTestMethod
	 * @see #beforeTestExecution
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestExecution(Object testInstance, Method testMethod, @Nullable Throwable exception)
			throws Exception {

		String callbackName = "afterTestExecution";
		prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
		Throwable afterTestExecutionException = null;

		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestExecution(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
				if (afterTestExecutionException == null) {
					afterTestExecutionException = ex;
				}
				else {
					afterTestExecutionException.addSuppressed(ex);
				}
			}
		}

		if (afterTestExecutionException != null) {
			ReflectionUtils.rethrowException(afterTestExecutionException);
		}
	}

	/**
	 * Hook for post-processing a test <em>after</em> execution of <em>after</em>
	 * lifecycle callbacks of the underlying test framework &mdash; for example,
	 * tearing down test fixtures, ending a transaction, etc.
	 * <p>This method <strong>must</strong> be called immediately after
	 * framework-specific <em>after</em> lifecycle callbacks (e.g., methods
	 * annotated with JUnit 4's {@link org.junit.After @After}). For historical
	 * reasons, this method is named {@code afterTestMethod}. Since the
	 * introduction of {@link #afterTestExecution}, a more suitable name for
	 * this method might be something like {@code afterTestTearDown} or
	 * {@code afterEach}; however, it is unfortunately impossible to rename
	 * this method due to backward compatibility concerns.
	 * <p>The managed {@link TestContext} will be updated with the supplied
	 * {@code testInstance}, {@code testMethod}, and {@code exception}.
	 * <p>Each registered {@link TestExecutionListener} will be given a chance
	 * to perform its post-processing. If a listener throws an exception, the
	 * remaining registered listeners will still be called. After all listeners
	 * have executed, the first caught exception will be rethrown with any
	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
	 * the first exception.
	 * <p>Note that registered listeners will be executed in the opposite
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which has just been executed on the
	 * test instance
	 * @param exception the exception that was thrown during execution of the test
	 * method or by a TestExecutionListener, or {@code null} if none was thrown
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @see #beforeTestMethod
	 * @see #beforeTestExecution
	 * @see #afterTestExecution
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestMethod(Object testInstance, Method testMethod, @Nullable Throwable exception)
			throws Exception {

		String callbackName = "afterTestMethod";
		prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
		Throwable afterTestMethodException = null;

		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestMethod(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
				if (afterTestMethodException == null) {
					afterTestMethodException = ex;
				}
				else {
					afterTestMethodException.addSuppressed(ex);
				}
			}
		}

		if (afterTestMethodException != null) {
			ReflectionUtils.rethrowException(afterTestMethodException);
		}
	}

	/**
	 * Hook for post-processing a test class <em>after</em> execution of all
	 * tests within the class. Should be called after any framework-specific
	 * <em>after class methods</em> (e.g., methods annotated with JUnit 4's
	 * {@link org.junit.AfterClass @AfterClass}).
	 * <p>Each registered {@link TestExecutionListener} will be given a chance
	 * to perform its post-processing. If a listener throws an exception, the
	 * remaining registered listeners will still be called. After all listeners
	 * have executed, the first caught exception will be rethrown with any
	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
	 * the first exception.
	 * <p>Note that registered listeners will be executed in the opposite
	 * @throws Exception if a registered TestExecutionListener throws an exception
	 * @since 3.0
	 * @see #getTestExecutionListeners()
	 * @see Throwable#addSuppressed(Throwable)
	 */
	public void afterTestClass() throws Exception {
		Class<?> testClass = getTestContext().getTestClass();
		if (logger.isTraceEnabled()) {
			logger.trace("afterTestClass(): class [" + testClass.getName() + "]");
		}
		getTestContext().updateState(null, null, null);

		Throwable afterTestClassException = null;
		// Traverse the TestExecutionListeners in reverse order to ensure proper
		// "wrapper"-style execution of listeners.
		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
			try {
				testExecutionListener.afterTestClass(getTestContext());
			}
			catch (Throwable ex) {
				logException(ex, "afterTestClass", testExecutionListener, testClass);
				if (afterTestClassException == null) {
					afterTestClassException = ex;
				}
				else {
					afterTestClassException.addSuppressed(ex);
				}
			}
		}

		this.testContextHolder.remove();

		if (afterTestClassException != null) {
			ReflectionUtils.rethrowException(afterTestClassException);
		}
	}

	private void prepareForBeforeCallback(String callbackName, Object testInstance, Method testMethod) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s(): instance [%s], method [%s]", callbackName, testInstance, testMethod));
		}
		getTestContext().updateState(testInstance, testMethod, null);
	}

	private void prepareForAfterCallback(String callbackName, Object testInstance, Method testMethod,
			@Nullable Throwable exception) {

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s(): instance [%s], method [%s], exception [%s]",
					callbackName, testInstance, testMethod, exception));
		}
		getTestContext().updateState(testInstance, testMethod, exception);
	}

	private void handleBeforeException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
			Object testInstance, Method testMethod) throws Exception {

		logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
		ReflectionUtils.rethrowException(ex);
	}

	private void logException(
			Throwable ex, String callbackName, TestExecutionListener testExecutionListener, Class<?> testClass) {

		if (logger.isWarnEnabled()) {
			logger.warn(String.format("Caught exception while invoking '%s' callback on " +
					"TestExecutionListener [%s] for test class [%s]", callbackName, testExecutionListener,
					testClass), ex);
		}
	}

	private void logException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
			Object testInstance, Method testMethod) {

		if (logger.isWarnEnabled()) {
			logger.warn(String.format("Caught exception while invoking '%s' callback on " +
					"TestExecutionListener [%s] for test method [%s] and test instance [%s]",
					callbackName, testExecutionListener, testMethod, testInstance), ex);
		}
	}


	/**
	 * Attempt to create a copy of the supplied {@code TestContext} using its
	 * <em>copy constructor</em>.
	 */
	private static TestContext copyTestContext(TestContext testContext) {
		Constructor<? extends TestContext> constructor =
				ClassUtils.getConstructorIfAvailable(testContext.getClass(), testContext.getClass());

		if (constructor != null) {
			try {
				ReflectionUtils.makeAccessible(constructor);
				return constructor.newInstance(testContext);
			}
			catch (Exception ex) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Failed to invoke copy constructor for [%s]; " +
							"concurrent test execution is therefore likely not supported.",
							testContext), ex);
				}
			}
		}

		// Fallback to original instance
		return testContext;
	}

}
