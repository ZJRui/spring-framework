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

package org.springframework.test.context.junit4;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.springframework.lang.Nullable;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestExecutionCallbacks;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestExecutionCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringJUnit4ClassRunner} is a custom extension of JUnit's
 * {@link BlockJUnit4ClassRunner} which provides functionality of the
 * <em>Spring TestContext Framework</em> to standard JUnit tests by means of the
 * {@link TestContextManager} and associated support classes and annotations.
 *
 * <p>To use this class, simply annotate a JUnit 4 based test class with
 * {@code @RunWith(SpringJUnit4ClassRunner.class)} or {@code @RunWith(SpringRunner.class)}.
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringJUnit4ClassRunner}. <em>(Note that additional
 * annotations may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener}
 * or {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link Test#expected() @Test(expected=...)}</li>
 * <li>{@link Test#timeout() @Test(timeout=...)}</li>
 * <li>{@link org.springframework.test.annotation.Timed @Timed}</li>
 * <li>{@link org.springframework.test.annotation.Repeat @Repeat}</li>
 * <li>{@link Ignore @Ignore}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p>If you would like to use the Spring TestContext Framework with a runner
 * other than this one, use {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * <p><strong>NOTE:</strong> As of Spring Framework 4.3, this class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see SpringRunner
 * @see TestContextManager
 * @see AbstractJUnit4SpringContextTests
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.junit4.rules.SpringClassRule
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 * @since 2.5
 */
public class SpringJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(SpringJUnit4ClassRunner.class);

	private static final Method withRulesMethod;

	static {

		//确定由提供的名称标识的类是否存在并可以加载。如果类或类的依赖项不存在或无法加载，则返回false。
		Assert.state(ClassUtils.isPresent("org.junit.internal.Throwables", SpringJUnit4ClassRunner.class.getClassLoader()),
				"SpringJUnit4ClassRunner requires JUnit 4.12 or higher.");

		Method method = ReflectionUtils.findMethod(SpringJUnit4ClassRunner.class, "withRules",
				FrameworkMethod.class, Object.class, Statement.class);
		Assert.state(method != null, "SpringJUnit4ClassRunner requires JUnit 4.12 or higher");
		ReflectionUtils.makeAccessible(method);
		withRulesMethod = method;
	}


	private final TestContextManager testContextManager;


	private static void ensureSpringRulesAreNotPresent(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			//判断 SpringClassRule是否是 field类型的父类或者同类，也就是Field的类型是否是SpringClassRule类型的

			Assert.state(!SpringClassRule.class.isAssignableFrom(field.getType()), () -> String.format(
					"Detected SpringClassRule field in test class [%s], " +
							"but SpringClassRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
			Assert.state(!SpringMethodRule.class.isAssignableFrom(field.getType()), () -> String.format(
					"Detected SpringMethodRule field in test class [%s], " +
							"but SpringMethodRule cannot be used with the SpringJUnit4ClassRunner.", testClass.getName()));
		}
	}

	/**
	 * Construct a new {@code SpringJUnit4ClassRunner} and initialize a
	 * {@link TestContextManager} to provide Spring testing functionality to
	 * standard JUnit tests.
	 *
	 * @param clazz the test class to be run
	 * @see #createTestContextManager(Class)
	 */
	public SpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);

		/***
		 *
		 * 同一个@RunWith标记的测试类中的每一个测试方法 都会创建一个 测试类对象，但是他们都是使用相同的Spring容器。 这其中的原因是：
		 * （1）@RunWith标记的测试类，会根据@RunWith注解中指定的Runner 和测试类 构建一个TestContext, TestContext 是所有的@Test方法共享的，所以每一个测试方法都会使用同一个spring容器。
		 *
		 * （2）每一个Test方法 都会被Junit重新封装成一个Statement，这个Statement的evaluate方法会首先创建测试类对象，因此每个测试方法执行的时候
		 * 都会创建一个测试类对象。
		 *
		 * （3）测试类对象创建完成之后 会执行一些listener之类的回调， 并对测试类对象进行依赖注入，依赖注入之前会先通过TestContext 获取Spring容器，如果发先容器没有创建，则会触发容器的创建。
		 *
		 * （4）容器创建是 加载的配置类和配置文件 是在TestContext中管理的 被merge成了一个configuration。
		 *
		 *
		 *
		 */



		/**
		 *
		 * SpringJunit4ClassRunner 创建 TestContextManager。
		 * TestContextManager 创建一个TestContextBootstrapper。
		 * TestContextBootstrapper 对象内有一个buildTestContext方法来创建一个TestContext对象
		 *
		 *
		 * TestContext 对象中有一个getApplicationContext方法来获取 ApplicationContext
		 * testContext对象中有getTestClass 和getTestInstance方法来获取测试类的类和测试类示例对象。
		 *  TestContext对象中有一个getTestmethod方法 返回Method 对象表示要测试的方法。
		 *
		 *
		 *  TestContextManager的构造器 中 会调用TestContextBootstrapper的buildTestContext 方法来 创建一个 DefaultTestContext 对象。
		 *  我们说TestContext 中有一个getApplicationContext方法能够返回一个ApplicationContext。
		 *
		 *  TestContextManager 在 通过 TestContextBootstrapper的buildTestContext方法创建DefaultTestContext时会 先准备Configuration。 这个configuration时通过解析 TestClass 测试类上的@ContextConfiguration 注解得到的
		 *
		 *
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
		 *
		 * 值得注意的 创建SpringJuni4ClassRunner对象 会 引起创建TestContext对象，但是这个时候我们并没有执行 testContext的loadApplicationContext
		 * 方法触发Spring容器的创建。
		 *
		 * 问题： 那么Spring容器是什么时候创建的呢？ 实际上是 在我们创建 测试实例对象 后 会对测试实例对象进行依赖注入，这个时候 会获取ApplicationContext
		 * TestContext内部 将ApplicationContext的管理委托给了CacheAwareContextLoaderDelegate ，因此第一个依赖注入的时候将会发现尚未存在
		 * ApplicationContext，从而触发创建Spring容器。但是当第二次对第二个测试实例依赖注入的时候 CacheAwareContextLoaderDelegate本身具有
		 * 缓存ApplicationContext的功能，因此不会再次创建Spring容器。
		 *
		 *
		 * 那么问题是 ： 测试实例对象又是什么时候执行的呢？
		 * 在SpringJunit4ClassRunner的methodBlock 方法中会将  测试实例的创建 和当前被测试的方法 封装成一个 MethodInvoker
		 * 类型的Statement。  当执行这个Statement 的evaluate的时候就会触发 测试实例对象的创建。
		 * 从这里我们可以看到， 实际上每一个@Test方法都会为其创建一个 测试实例对象，并对这个测试实例对象进行依赖注入。但是这些测试实例对象都会
		 * 使用同一个Spring容器。
		 *
		 *
		 * 那么问题： MethodInvoker 这个Statement是什么时候执行的呢？ JunitCore的run方法会执行 BlockJunit4ClassRunner的run方法
		 * 在run方法中会执行 测试树 每个节点statement的evalute 。
		 *
		 *
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
		 *
		 */


		/**
		 * 问题： SpringRunner继承自 SpringJunit4ClassRunner ， SpringJunit4ClassRunner什么时候
		 * 被调用？
		 *
		 *
		 * buildTestContext:103, SpringBootTestContextBootstrapper (org.springframework.boot.test.context)
		 * <init>:137, TestContextManager (org.springframework.test.context)
		 * <init>:122, TestContextManager (org.springframework.test.context)
		 * createTestContextManager:151, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 *
		 * 这个地方执行 SpringJunit4ClassRunner对象的创建， Junit的运行原理是先从TestClass
		 * 测试类上获取@RunWith注解， 这个注解会指定一个Junit的RUnner对象，如果没有的话默认就是
		 * BlockJunit4ClassRuner对象。然后 通过反射技术创建这个Runner对象
		 * 在SpringJunit4ClassRunner的 构造器中调用 了createTestContextManager
		 * <init>:142, SpringJUnit4ClassRunner (org.springframework.test.context.junit4)
		 * <init>:49, SpringRunner (org.springframework.test.context.junit4)
		 * newInstance0:-1, NativeConstructorAccessorImpl (sun.reflect)
		 * newInstance:62, NativeConstructorAccessorImpl (sun.reflect)
		 * newInstance:45, DelegatingConstructorAccessorImpl (sun.reflect)
		 * newInstance:423, Constructor (java.lang.reflect)
		 * buildRunner:104, AnnotatedBuilder (org.junit.internal.builders)
		 * runnerForClass:86, AnnotatedBuilder (org.junit.internal.builders)
		 * safeRunnerForClass:70, RunnerBuilder (org.junit.runners.model)
		 * runnerForClass:37, AllDefaultPossibilitiesBuilder (org.junit.internal.builders)
		 * safeRunnerForClass:70, RunnerBuilder (org.junit.runners.model)
		 * createRunner:28, ClassRequest (org.junit.internal.requests)
		 * getRunner:19, MemoizingRequest (org.junit.internal.requests)
		 * getRunner:36, FilterRequest (org.junit.internal.requests)
		 * startRunnerWithArgs:50, JUnit4IdeaTestRunner (com.intellij.junit4)
		 * execute:38, IdeaTestRunner$Repeater$1 (com.intellij.rt.junit)
		 * repeat:11, TestsRepeater (com.intellij.rt.execution.junit)
		 * startRunnerWithArgs:35, IdeaTestRunner$Repeater (com.intellij.rt.junit)
		 * prepareStreamsAndStart:235, JUnitStarter (com.intellij.rt.junit)
		 * main:54, JUnitStarter (com.intellij.rt.junit)
		 *
		 *
		 */


		if (logger.isDebugEnabled()) {
			logger.debug("SpringJUnit4ClassRunner constructor called with [" + clazz + "]");
		}
		//检测 testClass中是否存在 Field类型为SpringCassRule或者SpringMethodRule
		ensureSpringRulesAreNotPresent(clazz);
		/**
		 * createTestContextManager 很重要， 在创建TestContextManager的过程中执行了重要的逻辑
		 *
		 *
		 *
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
		 * 问题： 一个测试类中有两个@Test方法，那么他们会创建几个Spring容器？ 也就是
		 * Spring容器的创建和 SpringJUnit4ClassRunner之间的关系是什么？
		 *
		 *
		 */
		this.testContextManager = createTestContextManager(clazz);
	}




	/**
	 * Create a new {@link TestContextManager} for the supplied test class.
	 * <p>Can be overridden by subclasses.
	 *
	 * @param clazz the test class to be managed
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz);
	}

	/**
	 * Get the {@link TestContextManager} associated with this runner.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}

	/**
	 * Return a description suitable for an ignored test class if the test is
	 * disabled via {@code @IfProfileValue} at the class-level, and
	 * otherwise delegate to the parent implementation.
	 *
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 */
	@Override
	public Description getDescription() {
		/**
		 * 判断 testClsass类上是否存在@IfProfileValue注解， 如果存在则判断 对应的 属性是否为期望值。
		 * 如果为期望值则启用这个testClass，否则不启用这个testClass。
		 * isTestEnable返回false，则if为true，   createSuiteDescription 方法仅仅是创建了一个Description
		 * 和super.getDescription 相比， super的getDescription的方法会 执行 getFilteredChildren 从而构建测试树
		 */
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			return Description.createSuiteDescription(getTestClass().getJavaClass());
		}
		/**
		 * 构建测试树， 在getDescription方法中会执行getFilterChildren-->getChildren--
		 * BlockJunitClassRunner的getChildren方法会分析TestClass 获取其中的@Test方法
		 * 从而构建测试树
		 *
		 *
		 * 这个getDescription方法是在SpringJunit4ClassRunner的run方法中被调用的
		 * 也就是。 SpringJUnit4ClassRunner本身作为一个Runner对象。
		 * JUnitCore的run方法中 会执行 runner.run
		 *
		 */
		return super.getDescription();
	}

	/**
	 * Check whether the test is enabled in the current execution environment.
	 * <p>This prevents classes with a non-matching {@code @IfProfileValue}
	 * annotation from running altogether, even skipping the execution of
	 * {@code prepareTestInstance()} methods in {@code TestExecutionListeners}.
	 *
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see org.springframework.test.annotation.IfProfileValue
	 * @see org.springframework.test.context.TestExecutionListener
	 */
	@Override
	public void run(RunNotifier notifier) {

		/**
		 * 有三个注解：
		 * @Profile  @ActiveProfile  @IfProfileValue
		 *
		 *（1）@Profile 是spring中的正式注解，非test包下的注解，而且这个注解本质上是一个Condition。
		 * 经常的用法是 在一个配置类中 对@Bean标记的方法 加上@Profile注解	 ，标记如果当前启用的配置文件是 指定的
		 * Profile则注入这个Bean。 典型场景是  两个@Bean标记的创建DataSource的方法， 一个是正式数据库，一个是测试数据库
		 * 这种情况下根据 当前启用的Profile不同注入不同的 DataSource
		 *  @Bean
		 *     @Profile("upper")
		 *     public UpperAction upperAction1(){
		 *         return  new UpperAction("Tom");
		 *     }
		 *
		 *     @Bean
		 *     @Profile("upper1")
		 *     public UpperAction upperAction2(){
		 *         return  new UpperAction("Jack");
		 *     }
		 *
		 *
		 *  （2）@ActiveProfiles是test模块下的注解，   典型用法是 在配置类上 标记改注解，表示启用某 profile
		 *
		 *  （3）@IfProfileValue 也是test模块下的类。 用来标注在方法或者类上。
		 *   @IfProfileValue(name = "test-groups", values = { "unit-tests", "integration-tests" })
		 *   public void testWhichRunsForUnitOrIntegrationTestGroups() {
		 *     // ...
		 *     }
		 *     表示 ，这个方法只有在  unit-tests的值 为 unit-tests或者 integration-tests时候才会执行
		 *     The above test method would be executed if you set the test-groups system property (e.g., -Dtest-groups=unit-tests or -Dtest-groups=integration-tests).
		 *
		 *
		 */
		if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
			notifier.fireTestIgnored(getDescription());
			return;
		}
		/**
		 * SpringJunitClassRunner 本身作为Runner， JunitCore的run方法会调用 runner的run方法。
		 *
		 *
		 * SpringJunit4ClassRunner的父类是BlockJunit4CLassRunner
		 * 父类的run方法 内部首先是  通过classBlock方法 创建一个Statement
		 *
		 * 这个Statement内部的实现逻辑就是  将测试类中的 所有 testMethod 交给RunnerScheduler
		 * 执行。
		 * 那么 对于每一个 testmethod  ，RunnerScheduler都会执行 通过runChild 方法来执行他。
		 *
		 * BlockJunit4ClassRunner 的runChild方法 内部实现的逻辑 就是 首选通过 反射的方式创建一个TestClass对象（getTestClass().getOnlyConstructor().newInstance()
		 * ），然后  创建一个 InvokeMethod 对象， new InvokeMethod(method, test)。 通过InvokeMethod对象 完成反射方法的调用。
		 *
		 *
		 * SpringJUnit4ClassRuner 重写了runChild方法
		 */
		super.run(notifier);
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunBeforeTestClassCallbacks} statement, thus preserving the
	 * default JUnit functionality while adding support for the Spring TestContext
	 * Framework.
	 *
	 * @see RunBeforeTestClassCallbacks
	 */
	@Override
	protected Statement withBeforeClasses(Statement statement) {
		/**
		 * withBeforeClasses-->ParentRunner.classBlok-->ParentRunner.run-->
		 */
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunAfterTestClassCallbacks} statement, thus preserving the default
	 * JUnit functionality while adding support for the Spring TestContext Framework.
	 *
	 * @see RunAfterTestClassCallbacks
	 */
	@Override
	protected Statement withAfterClasses(Statement statement) {
		Statement junitAfterClasses = super.withAfterClasses(statement);
		return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
	}

	/**
	 * Delegate to the parent implementation for creating the test instance and
	 * then allow the {@link #getTestContextManager() TestContextManager} to
	 * prepare the test instance before returning it.
	 *
	 * @see TestContextManager#prepareTestInstance
	 */
	@Override
	protected Object createTest() throws Exception {
		Object testInstance = super.createTest();
		getTestContextManager().prepareTestInstance(testInstance);
		return testInstance;
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#runChild(FrameworkMethod, RunNotifier)},
	 * except that tests are determined to be <em>ignored</em> by
	 * {@link #isTestMethodIgnored(FrameworkMethod)}.
	 */
	@Override
	protected void runChild(FrameworkMethod frameworkMethod, RunNotifier notifier) {

		/**
		 * SpringJunitClassRunner 本身作为Runner， JunitCore的run方法会调用 runner的run方法。
		 *
		 *
		 * SpringJunit4ClassRunner的父类是BlockJunit4CLassRunner
		 * 父类的run方法 内部首先是  通过classBlock方法 创建一个Statement
		 *
		 * 这个Statement内部的实现逻辑就是  将测试类中的 所有 testMethod 交给RunnerScheduler
		 * 执行。
		 * 那么 对于每一个 testmethod  ，RunnerScheduler都会执行 通过runChild 方法来执行他。
		 *
		 * BlockJunit4ClassRunner 的runChild方法 内部实现的逻辑 就是 首选通过 反射的方式创建一个TestClass对象（getTestClass().getOnlyConstructor().newInstance()
		 * ），然后  创建一个 InvokeMethod 对象， new InvokeMethod(method, test)。 通过InvokeMethod对象 完成反射方法的调用。
		 *
		 *
		 * SpringJUnit4ClassRuner 重写了runChild方法。
		 *
		 *  首先 得到当前frameworkMethod的 Description对象
		 *  然后根据frameworkMethod构建一个 statement.
		 *  在这个Statement的内部实现中 也会通过 反射的方式创建 测试类的示例对象
		 *   创建完示例对象之后会 执行 getTestContextManager().prepareTestInstance(testInstance);
		 *   在prepareTestInstance的过程中会执行 TestExecutionListener ，其中有一个 DependencyInjectionTestExecutionListener
		 *   这个Listener完成对测试对象的依赖注入。
		 *   依赖注入的逻辑是：
		 *   	protected void injectDependencies(TestContext testContext) throws Exception {
		 * 		Object bean = testContext.getTestInstance();
		 * 		Class<?> clazz = testContext.getTestClass();
		 * 		AutowireCapableBeanFactory beanFactory = testContext.getApplicationContext().getAutowireCapableBeanFactory();
		 * 		beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
		 * 		beanFactory.initializeBean(bean, clazz.getName() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX);
		 * 		testContext.removeAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE);
		 *        }
		 *
		 *
		 *  然后将测试类示例对象和frameworkMethod封装成 InvokeMethod
		 *
		 *
		 */


		Description description = describeChild(frameworkMethod);
		if (isTestMethodIgnored(frameworkMethod)) {
			notifier.fireTestIgnored(description);
		} else {
			Statement statement;
			try {

				/**
				 *对测试类 方法对封装  ，封装成Statement
				 *
				 *
				 *同一个@RunWith标记的测试类中的每一个测试方法 都会创建一个 测试类对象，但是他们都是使用相同的Spring容器。 这其中的原因是：
				 * （1）@RunWith标记的测试类，会根据@RunWith注解中指定的Runner 和测试类 构建一个TestContext, TestContext 是所有的@Test方法共享的，所以每一个测试方法都会使用同一个spring容器。
				 *
				 * （2）每一个Test方法 都会被Junit重新封装成一个Statement，这个Statement的evaluate方法会首先创建测试类对象，因此每个测试方法执行的时候
				 * 都会创建一个测试类对象。
				 *
				 * （3）测试类对象创建完成之后 会执行一些listener之类的回调， 并对测试类对象进行依赖注入，依赖注入之前会先通过TestContext 获取Spring容器，如果发先容器没有创建，则会触发容器的创建。
				 *
				 * （4）容器创建是 加载的配置类和配置文件 是在TestContext中管理的 被merge成了一个configuration。
				 *
				 */
				statement = methodBlock(frameworkMethod);
			} catch (Throwable ex) {
				statement = new Fail(ex);
			}
			runLeaf(statement, description, notifier);
		}
	}

	/**
	 * Augment the default JUnit behavior
	 * {@linkplain #withPotentialRepeat with potential repeats} of the entire
	 * execution chain.
	 * <p>Furthermore, support for timeouts has been moved down the execution
	 * chain in order to include execution of {@link org.junit.Before @Before}
	 * and {@link org.junit.After @After} methods within the timed execution.
	 * Note that this differs from the default JUnit behavior of executing
	 * {@code @Before} and {@code @After} methods in the main thread while
	 * executing the actual test method in a separate thread. Thus, the net
	 * effect is that {@code @Before} and {@code @After} methods will be
	 * executed in the same thread as the test method. As a consequence,
	 * JUnit-specified timeouts will work fine in combination with Spring
	 * transactions. However, JUnit-specific timeouts still differ from
	 * Spring-specific timeouts in that the former execute in a separate
	 * thread while the latter simply execute in the main thread (like regular
	 * tests).
	 *
	 * @see #methodInvoker(FrameworkMethod, Object)
	 * @see #withBeforeTestExecutionCallbacks(FrameworkMethod, Object, Statement)
	 * @see #withAfterTestExecutionCallbacks(FrameworkMethod, Object, Statement)
	 * @see #possiblyExpectingExceptions(FrameworkMethod, Object, Statement)
	 * @see #withBefores(FrameworkMethod, Object, Statement)
	 * @see #withAfters(FrameworkMethod, Object, Statement)
	 * @see #withRulesReflectively(FrameworkMethod, Object, Statement)
	 * @see #withPotentialRepeat(FrameworkMethod, Object, Statement)
	 * @see #withPotentialTimeout(FrameworkMethod, Object, Statement)
	 */
	@Override
	protected Statement methodBlock(FrameworkMethod frameworkMethod) {
		/**
		 *
		 * SpringJunit4ClassRunner 创建 TestContextManager。
		 * TestContextManager 创建一个TestContextBootstrapper。
		 * TestContextBootstrapper 对象内有一个buildTestContext方法来创建一个TestContext对象
		 *
		 *
		 * TestContext 对象中有一个getApplicationContext方法来获取 ApplicationContext
		 * testContext对象中有getTestClass 和getTestInstance方法来获取测试类的类和测试类示例对象。
		 *  TestContext对象中有一个getTestmethod方法 返回Method 对象表示要测试的方法。
		 *
		 *
		 *  TestContextManager的构造器 中 会调用TestContextBootstrapper的buildTestContext 方法来 创建一个 DefaultTestContext 对象。
		 *  我们说TestContext 中有一个getApplicationContext方法能够返回一个ApplicationContext。
		 *
		 *  TestContextManager 在 通过 TestContextBootstrapper的buildTestContext方法创建DefaultTestContext时会 先准备Configuration。 这个configuration时通过解析 TestClass 测试类上的@ContextConfiguration 注解得到的
		 *
		 *
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
		 *
		 * 值得注意的 创建SpringJuni4ClassRunner对象 会 引起创建TestContext对象，但是这个时候我们并没有执行 testContext的loadApplicationContext
		 * 方法触发Spring容器的创建。
		 *
		 * 问题： 那么Spring容器是什么时候创建的呢？ 实际上是 在我们创建 测试实例对象 后 会对测试实例对象进行依赖注入，这个时候 会获取ApplicationContext
		 * TestContext内部 将ApplicationContext的管理委托给了CacheAwareContextLoaderDelegate ，因此第一个依赖注入的时候将会发现尚未存在
		 * ApplicationContext，从而触发创建Spring容器。但是当第二次对第二个测试实例依赖注入的时候 CacheAwareContextLoaderDelegate本身具有
		 * 缓存ApplicationContext的功能，因此不会再次创建Spring容器。
		 *
		 *
		 * 那么问题是 ： 测试实例对象又是什么时候执行的呢？
		 * 在SpringJunit4ClassRunner的methodBlock 方法中会将  测试实例的创建 和当前被测试的方法 封装成一个 MethodInvoker
		 * 类型的Statement。  当执行这个Statement 的evaluate的时候就会触发 测试实例对象的创建。
		 * 从这里我们可以看到， 实际上每一个@Test方法都会为其创建一个 测试实例对象，并对这个测试实例对象进行依赖注入。但是这些测试实例对象都会
		 * 使用同一个Spring容器。
		 *
		 *
		 * 那么问题： MethodInvoker 这个Statement是什么时候执行的呢？ JunitCore的run方法会执行 BlockJunit4ClassRunner的run方法
		 * 在run方法中会执行 测试树 每个节点statement的evalute 。
		 *
		 *
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
		 *
		 */
		Object testInstance;
		try {
			//创建测试类对象
			testInstance = new ReflectiveCallable() {
				@Override
				protected Object runReflectiveCall() throws Throwable {
					return createTest();
				}
			}.run();
		} catch (Throwable ex) {
			return new Fail(ex);
		}

		Statement statement = methodInvoker(frameworkMethod, testInstance);
		statement = withBeforeTestExecutionCallbacks(frameworkMethod, testInstance, statement);
		statement = withAfterTestExecutionCallbacks(frameworkMethod, testInstance, statement);
		statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
		statement = withBefores(frameworkMethod, testInstance, statement);
		statement = withAfters(frameworkMethod, testInstance, statement);
		statement = withRulesReflectively(frameworkMethod, testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		return statement;
	}

	/**
	 * Invoke JUnit's private {@code withRules()} method using reflection.
	 */
	private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Object result = ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance, statement);
		Assert.state(result instanceof Statement, "withRules mismatch");
		return (Statement) result;
	}

	/**
	 * Return {@code true} if {@link Ignore @Ignore} is present for the supplied
	 * {@linkplain FrameworkMethod test method} or if the test method is disabled
	 * via {@code @IfProfileValue}.
	 *
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
	 */
	protected boolean isTestMethodIgnored(FrameworkMethod frameworkMethod) {
		Method method = frameworkMethod.getMethod();
		return (method.isAnnotationPresent(Ignore.class) ||
				!ProfileValueUtils.isTestEnabledInThisEnvironment(method, getTestClass().getJavaClass()));
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)}
	 * except that the <em>expected exception</em> is retrieved using
	 * {@link #getExpectedException(FrameworkMethod)}.
	 */
	@Override
	protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
		return (expectedException != null ? new ExpectException(next, expectedException) : next);
	}

	/**
	 * Get the {@code exception} that the supplied {@linkplain FrameworkMethod
	 * test method} is expected to throw.
	 * <p>Supports JUnit's {@link Test#expected() @Test(expected=...)} annotation.
	 * <p>Can be overridden by subclasses.
	 *
	 * @return the expected exception, or {@code null} if none was specified
	 */
	@Nullable
	protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.expected() != Test.None.class ? test.expected() : null);
	}

	/**
	 * Perform the same logic as
	 * {@link BlockJUnit4ClassRunner#withPotentialTimeout(FrameworkMethod, Object, Statement)}
	 * but with additional support for Spring's {@code @Timed} annotation.
	 * <p>Supports both Spring's {@link org.springframework.test.annotation.Timed @Timed}
	 * and JUnit's {@link Test#timeout() @Test(timeout=...)} annotations, but not both
	 * simultaneously.
	 *
	 * @return either a {@link SpringFailOnTimeout}, a {@link FailOnTimeout},
	 * or the supplied {@link Statement} as appropriate
	 * @see #getSpringTimeout(FrameworkMethod)
	 * @see #getJUnitTimeout(FrameworkMethod)
	 */
	@Override
	// Retain the following warning suppression for deprecation (even if Eclipse
	// states it is unnecessary) since withPotentialTimeout(FrameworkMethod,Object,Statement)
	// in BlockJUnit4ClassRunner has been deprecated.
	@SuppressWarnings("deprecation")
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Statement statement = null;
		long springTimeout = getSpringTimeout(frameworkMethod);
		long junitTimeout = getJUnitTimeout(frameworkMethod);
		if (springTimeout > 0 && junitTimeout > 0) {
			String msg = String.format("Test method [%s] has been configured with Spring's @Timed(millis=%s) and " +
					"JUnit's @Test(timeout=%s) annotations, but only one declaration of a 'timeout' is " +
					"permitted per test method.", frameworkMethod.getMethod(), springTimeout, junitTimeout);
			logger.error(msg);
			throw new IllegalStateException(msg);
		} else if (springTimeout > 0) {
			statement = new SpringFailOnTimeout(next, springTimeout);
		} else if (junitTimeout > 0) {
			statement = FailOnTimeout.builder().withTimeout(junitTimeout, TimeUnit.MILLISECONDS).build(next);
		} else {
			statement = next;
		}

		return statement;
	}

	/**
	 * Retrieve the configured JUnit {@code timeout} from the {@link Test @Test}
	 * annotation on the supplied {@linkplain FrameworkMethod test method}.
	 *
	 * @return the timeout, or {@code 0} if none was specified
	 */
	protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
		Test test = frameworkMethod.getAnnotation(Test.class);
		return (test != null && test.timeout() > 0 ? test.timeout() : 0);
	}

	/**
	 * Retrieve the configured Spring-specific {@code timeout} from the
	 * {@link org.springframework.test.annotation.Timed @Timed} annotation
	 * on the supplied {@linkplain FrameworkMethod test method}.
	 *
	 * @return the timeout, or {@code 0} if none was specified
	 * @see TestAnnotationUtils#getTimeout(Method)
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		return TestAnnotationUtils.getTimeout(frameworkMethod.getMethod());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunBeforeTestExecutionCallbacks}
	 * statement, thus preserving the default functionality while adding support for the
	 * Spring TestContext Framework.
	 *
	 * @see RunBeforeTestExecutionCallbacks
	 */
	protected Statement withBeforeTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new RunBeforeTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunAfterTestExecutionCallbacks}
	 * statement, thus preserving the default functionality while adding support for the
	 * Spring TestContext Framework.
	 *
	 * @see RunAfterTestExecutionCallbacks
	 */
	protected Statement withAfterTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new RunAfterTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunBeforeTestMethodCallbacks} statement, thus preserving the
	 * default functionality while adding support for the Spring TestContext
	 * Framework.
	 *
	 * @see RunBeforeTestMethodCallbacks
	 */
	@Override
	protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
		return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * Wrap the {@link Statement} returned by the parent implementation with a
	 * {@code RunAfterTestMethodCallbacks} statement, thus preserving the
	 * default functionality while adding support for the Spring TestContext
	 * Framework.
	 *
	 * @see RunAfterTestMethodCallbacks
	 */
	@Override
	protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
		return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(), getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code SpringRepeat} statement.
	 * <p>Supports Spring's {@link org.springframework.test.annotation.Repeat @Repeat}
	 * annotation.
	 *
	 * @see TestAnnotationUtils#getRepeatCount(Method)
	 * @see SpringRepeat
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		return new SpringRepeat(next, frameworkMethod.getMethod());
	}

}
