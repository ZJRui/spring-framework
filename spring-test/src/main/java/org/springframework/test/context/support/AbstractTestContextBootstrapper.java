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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract implementation of the {@link TestContextBootstrapper} interface which
 * provides most of the behavior required by a bootstrapper.
 *
 * <p>Concrete subclasses typically will only need to provide implementations for
 * the following methods:
 * <ul>
 * <li>{@link #getDefaultContextLoaderClass}
 * <li>{@link #processMergedContextConfiguration}
 * </ul>
 *
 * <p>To plug in custom
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * support, override {@link #getCacheAwareContextLoaderDelegate()}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.1
 */
@SuppressWarnings("all")
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {

	private final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private BootstrapContext bootstrapContext;


	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		Assert.state(this.bootstrapContext != null, "No BootstrapContext set");
		return this.bootstrapContext;
	}

	/**
	 * Build a new {@link DefaultTestContext} using the {@linkplain Class test class}
	 * in the {@link BootstrapContext} associated with this bootstrapper and
	 * by delegating to {@link #buildMergedContextConfiguration()} and
	 * {@link #getCacheAwareContextLoaderDelegate()}.
	 * <p>Concrete subclasses may choose to override this method to return a
	 * custom {@link TestContext} implementation.
	 * @since 4.2
	 */
	@Override
	public TestContext buildTestContext() {
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
		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
				getCacheAwareContextLoaderDelegate());
	}

	@Override
	public final List<TestExecutionListener> getTestExecutionListeners() {
		Class<?> clazz = getBootstrapContext().getTestClass();
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<>();
		boolean usingDefaults = false;

		AnnotationDescriptor<TestExecutionListeners> descriptor =
				MetaAnnotationUtils.findAnnotationDescriptor(clazz, annotationType);

		// Use defaults?
		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("@TestExecutionListeners is not present for class [%s]: using defaults.",
						clazz.getName()));
			}
			usingDefaults = true;
			classesList.addAll(getDefaultTestExecutionListenerClasses());
		}
		else {
			// Traverse the class hierarchy...
			while (descriptor != null) {
				Class<?> declaringClass = descriptor.getDeclaringClass();
				TestExecutionListeners testExecutionListeners = descriptor.synthesizeAnnotation();
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Retrieved @TestExecutionListeners [%s] for declaring class [%s].",
							testExecutionListeners, declaringClass.getName()));
				}

				boolean inheritListeners = testExecutionListeners.inheritListeners();
				AnnotationDescriptor<TestExecutionListeners> superDescriptor =
						MetaAnnotationUtils.findAnnotationDescriptor(
								descriptor.getRootDeclaringClass().getSuperclass(), annotationType);

				// If there are no listeners to inherit, we might need to merge the
				// locally declared listeners with the defaults.
				if ((!inheritListeners || superDescriptor == null) &&
						testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Merging default listeners with listeners configured via " +
								"@TestExecutionListeners for class [%s].", descriptor.getRootDeclaringClass().getName()));
					}
					usingDefaults = true;
					classesList.addAll(getDefaultTestExecutionListenerClasses());
				}

				classesList.addAll(0, Arrays.asList(testExecutionListeners.listeners()));

				descriptor = (inheritListeners ? superDescriptor : null);
			}
		}

		Collection<Class<? extends TestExecutionListener>> classesToUse = classesList;
		// Remove possible duplicates if we loaded default listeners.
		if (usingDefaults) {
			classesToUse = new LinkedHashSet<>(classesList);
		}

		List<TestExecutionListener> listeners = instantiateListeners(classesToUse);
		// Sort by Ordered/@Order if we loaded default listeners.
		if (usingDefaults) {
			AnnotationAwareOrderComparator.sort(listeners);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Using TestExecutionListeners: " + listeners);
		}
		return listeners;
	}

	private List<TestExecutionListener> instantiateListeners(Collection<Class<? extends TestExecutionListener>> classes) {
		List<TestExecutionListener> listeners = new ArrayList<>(classes.size());
		for (Class<? extends TestExecutionListener> listenerClass : classes) {
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			}
			catch (BeanInstantiationException ex) {
				if (ex.getCause() instanceof NoClassDefFoundError) {
					// TestExecutionListener not applicable due to a missing dependency
					if (logger.isDebugEnabled()) {
						logger.debug(String.format(
								"Skipping candidate TestExecutionListener [%s] due to a missing dependency. " +
								"Specify custom listener classes or make the default listener classes " +
								"and their required dependencies available. Offending class: [%s]",
								listenerClass.getName(), ex.getCause().getMessage()));
					}
				}
				else {
					throw ex;
				}
			}
		}
		return listeners;
	}

	/**
	 * Get the default {@link TestExecutionListener} classes for this bootstrapper.
	 * <p>This method is invoked by {@link #getTestExecutionListeners()} and
	 * delegates to {@link #getDefaultTestExecutionListenerClassNames()} to
	 * retrieve the class names.
	 * <p>If a particular class cannot be loaded, a {@code DEBUG} message will
	 * be logged, but the associated exception will not be rethrown.
	 */
	@SuppressWarnings("unchecked")
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<>();
		ClassLoader cl = getClass().getClassLoader();
		for (String className : getDefaultTestExecutionListenerClassNames()) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) ClassUtils.forName(className, cl));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className +
							"]. Specify custom listener classes or make the default listener classes available.", ex);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * Get the names of the default {@link TestExecutionListener} classes for
	 * this bootstrapper.
	 * <p>The default implementation looks up all
	 * {@code org.springframework.test.context.TestExecutionListener} entries
	 * configured in all {@code META-INF/spring.factories} files on the classpath.
	 * <p>This method is invoked by {@link #getDefaultTestExecutionListenerClasses()}.
	 * @return an <em>unmodifiable</em> list of names of default {@code TestExecutionListener}
	 * classes
	 * @see SpringFactoriesLoader#loadFactoryNames
	 */
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		List<String> classNames =
				SpringFactoriesLoader.loadFactoryNames(TestExecutionListener.class, getClass().getClassLoader());
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Loaded default TestExecutionListener class names from location [%s]: %s",
					SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION, classNames));
		}
		return Collections.unmodifiableList(classNames);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final MergedContextConfiguration buildMergedContextConfiguration() {
		/**
		 * 在与这个引导程序关联的BootstrapContext中为测试类构建合并的上下文配置。
		 * 在构建合并配置时，实现必须考虑以下因素:
		 * 1.上下文层次结构通过@ContextHierarchy和@ContextConfiguration声明
		 * 2.通过@ActiveProfiles声明的活动bean定义配置文件
		 * 3.上下文初始化器通过contextconfiguration .initializer声明:Context initializers declared via ContextConfiguration.initializers
		 * 4.测试属性源声明通过@TestPropertySource
		 *
		 * 有关所需语义的详细信息，请参考Javadoc中的上述注释。
		 * 注意，在构造TestContext时，buildTestContext()的实现通常应该委托给这个方法。
		 * 当决定为给定的测试类使用哪个ContextLoader时，应该使用以下算法:
		 * 如果一个ContextLoader类已经通过ContextConfiguration显式声明。装载机,使用它。
		 * 否则，具体实现可以自由决定使用哪个ContextLoader类作为默认值。
		 *
		 */
		Class<?> testClass = getBootstrapContext().getTestClass();
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

		/**
		 *
		 * 确定测试类上的ContextConfiguration 和ContextHierarchy
		 */
		if (MetaAnnotationUtils.findAnnotationDescriptorForTypes(
				testClass, ContextConfiguration.class, ContextHierarchy.class) == null) {
			return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
		}

		if (AnnotationUtils.findAnnotation(testClass, ContextHierarchy.class) != null) {
			Map<String, List<ContextConfigurationAttributes>> hierarchyMap =
					ContextLoaderUtils.buildContextHierarchyMap(testClass);
			MergedContextConfiguration parentConfig = null;
			MergedContextConfiguration mergedConfig = null;

			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
				List<ContextConfigurationAttributes> reversedList = new ArrayList<>(list);
				Collections.reverse(reversedList);

				// Don't use the supplied testClass; instead ensure that we are
				// building the MCC for the actual test class that declared the
				// configuration for the current level in the context hierarchy.
				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

				mergedConfig = buildMergedContextConfiguration(
						declaringClass, reversedList, parentConfig, cacheAwareContextLoaderDelegate, true);
				parentConfig = mergedConfig;
			}

			// Return the last level in the context hierarchy
			Assert.state(mergedConfig != null, "No merged context configuration");
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass,
					ContextLoaderUtils.resolveContextConfigurationAttributes(testClass),
					null, cacheAwareContextLoaderDelegate, true);
		}
	}

	private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		List<ContextConfigurationAttributes> defaultConfigAttributesList =
				Collections.singletonList(new ContextConfigurationAttributes(testClass));

		ContextLoader contextLoader = resolveContextLoader(testClass, defaultConfigAttributesList);
		if (logger.isInfoEnabled()) {
			logger.info(String.format(
					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s], using %s",
					testClass.getName(), contextLoader.getClass().getSimpleName()));
		}
		return buildMergedContextConfiguration(testClass, defaultConfigAttributesList, null,
				cacheAwareContextLoaderDelegate, false);
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration}
	 * for the supplied {@link Class testClass}, context configuration attributes,
	 * and parent context configuration.
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param configAttributesList the list of context configuration attributes for the
	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
	 * traversing up the class hierarchy); never {@code null} or empty
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @param requireLocationsClassesOrInitializers whether locations, classes, or
	 * initializers are required; typically {@code true} but may be set to {@code false}
	 * if the configured loader supports empty configuration
	 * @return the merged context configuration
	 * @see #resolveContextLoader
	 * @see ContextLoaderUtils#resolveContextConfigurationAttributes
	 * @see SmartContextLoader#processContextConfiguration
	 * @see ContextLoader#processLocations
	 * @see ActiveProfilesUtils#resolveActiveProfiles
	 * @see ApplicationContextInitializerUtils#resolveInitializerClasses
	 * @see MergedContextConfiguration
	 */
	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, @Nullable MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			boolean requireLocationsClassesOrInitializers) {
		/**
		 *
		 * 为提供的testClass、上下文配置属性和父上下文配置构建合并的上下文配置。
		 * 参数:
		 * configAttributesList -指定测试类的上下文配置属性列表，顺序自底向上(即，好像我们在遍历类层次结构);
		 * 永远不要为null或空的parentConfig——上下文层次结构中父应用程序上下文的合并上下文配置，
		 * 如果没有父cacheAwareContextLoaderDelegate——缓存感知上下文加载器委托将被传递给MergedContextConfiguration
		 * 构造函数requirelocationsclassesorinitializer——无论位置、类或初始化器是否需要;通常为true，
		 * 但如果配置的加载器支持空配置，也可以设置为false
		 * 返回:
		 * 合并的上下文配置
		 */

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
		List<String> locations = new ArrayList<>();
		List<Class<?>> classes = new ArrayList<>();
		List<Class<?>> initializers = new ArrayList<>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
						configAttributes));
			}
			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(
						configAttributes.getDeclaringClass(), configAttributes.getLocations());
				locations.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}
			initializers.addAll(0, Arrays.asList(configAttributes.getInitializers()));
			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
				Collections.unmodifiableList(configAttributesList));

		Assert.state(!(requireLocationsClassesOrInitializers &&
				areAllEmpty(locations, classes, initializers, contextCustomizers)), () -> String.format(
				"%s was unable to detect defaults, and no ApplicationContextInitializers " +
				"or ContextCustomizers were declared for context configuration attributes %s",
				contextLoader.getClass().getSimpleName(), configAttributesList));

		MergedTestPropertySources mergedTestPropertySources =
				TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
				StringUtils.toStringArray(locations), ClassUtils.toClassArray(classes),
				ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList),
				ActiveProfilesUtils.resolveActiveProfiles(testClass),
				mergedTestPropertySources.getLocations(),
				mergedTestPropertySources.getProperties(),
				contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

		return processMergedContextConfiguration(mergedConfig);
	}

	private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		/**
		 * 这个方法有什么作用？
		 *
		 * createContextCustomizer:38, TestRestTemplateContextCustomizerFactory (org.springframework.boot.test.web.client)
		 * getContextCustomizers:401, AbstractTestContextBootstrapper (org.springframework.test.context.support)------> 获取contextCustomizers
		 * buildMergedContextConfiguration:373, AbstractTestContextBootstrapper (org.springframework.test.context.support)
		 * buildDefaultMergedContextConfiguration:309, AbstractTestContextBootstrapper (org.springframework.test.context.support)
		 * buildMergedContextConfiguration:262, AbstractTestContextBootstrapper (org.springframework.test.context.support)
		 * buildTestContext:107, AbstractTestContextBootstrapper (org.springframework.test.context.support)
		 * buildTestContext:153, SpringBootTestContextBootstrapper (org.springframework.boot.test.context)--------——>构建context
		 * <init>:137, TestContextManager (org.springframework.test.context)
		 * <init>:122, TestContextManager (org.springframework.test.context)
		 * apply:-1, 116734858 (org.springframework.test.context.junit.jupiter.SpringExtension$$Lambda$261)
		 * lambda$getOrComputeIfAbsent$4:86, ExtensionValuesStore (org.junit.jupiter.engine.execution)
		 * get:-1, 93740343 (org.junit.jupiter.engine.execution.ExtensionValuesStore$$Lambda$262)
		 * computeValue:223, ExtensionValuesStore$MemoizingSupplier (org.junit.jupiter.engine.execution)
		 * get:211, ExtensionValuesStore$MemoizingSupplier (org.junit.jupiter.engine.execution)
		 * evaluate:191, ExtensionValuesStore$StoredValue (org.junit.jupiter.engine.execution)
		 * access$100:171, ExtensionValuesStore$StoredValue (org.junit.jupiter.engine.execution)
		 * getOrComputeIfAbsent:89, ExtensionValuesStore (org.junit.jupiter.engine.execution)
		 * getOrComputeIfAbsent:93, ExtensionValuesStore (org.junit.jupiter.engine.execution)
		 * getOrComputeIfAbsent:61, NamespaceAwareStore (org.junit.jupiter.engine.execution)
		 * getTestContextManager:294, SpringExtension (org.springframework.test.context.junit.jupiter)
		 * beforeAll:113, SpringExtension (org.springframework.test.context.junit.jupiter)
		 * lambda$invokeBeforeAllCallbacks$8:368, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * execute:-1, 1171802656 (org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor$$Lambda$256)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * invokeBeforeAllCallbacks:368, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * before:192, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * before:78, ClassBasedTestDescriptor (org.junit.jupiter.engine.descriptor)
		 * lambda$executeRecursively$5:136, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 1961501712 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$207)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$7:129, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * invoke:-1, 634297796 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$206)
		 * around:137, Node (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$8:127, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 754177595 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$205)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * executeRecursively:126, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:84, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * accept:-1, 65586123 (org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService$$Lambda$211)
		 * forEach:1259, ArrayList (java.util)
		 * invokeAll:38, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$5:143, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 1961501712 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$207)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$7:129, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * invoke:-1, 634297796 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$206)
		 * around:137, Node (org.junit.platform.engine.support.hierarchical)
		 * lambda$executeRecursively$8:127, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:-1, 754177595 (org.junit.platform.engine.support.hierarchical.NodeTestTask$$Lambda$205)
		 * execute:73, ThrowableCollector (org.junit.platform.engine.support.hierarchical)
		 * executeRecursively:126, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * execute:84, NodeTestTask (org.junit.platform.engine.support.hierarchical)
		 * submit:32, SameThreadHierarchicalTestExecutorService (org.junit.platform.engine.support.hierarchical)
		 * execute:57, HierarchicalTestExecutor (org.junit.platform.engine.support.hierarchical)
		 * execute:51, HierarchicalTestEngine (org.junit.platform.engine.support.hierarchical)
		 * execute:108, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:88, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * lambda$execute$0:54, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * accept:-1, 298430307 (org.junit.platform.launcher.core.EngineExecutionOrchestrator$$Lambda$156)
		 * withInterceptedStreams:67, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:52, EngineExecutionOrchestrator (org.junit.platform.launcher.core)
		 * execute:96, DefaultLauncher (org.junit.platform.launcher.core)
		 * execute:75, DefaultLauncher (org.junit.platform.launcher.core)
		 * startRunnerWithArgs:71, JUnit5IdeaTestRunner (com.intellij.junit5)
		 * execute:38, IdeaTestRunner$Repeater$1 (com.intellij.rt.junit)
		 * repeat:11, TestsRepeater (com.intellij.rt.execution.junit)
		 * startRunnerWithArgs:35, IdeaTestRunner$Repeater (com.intellij.rt.junit)
		 * prepareStreamsAndStart:235, JUnitStarter (com.intellij.rt.junit)
		 * main:54, JUnitStarter (com.intellij.rt.junit)
		 *
		 *
		 *
		 * getContextCustomizers方法的主要作用 就是通过springfactoriesloader 读取spring.factories文件中的 ContextCustomizerFactory 配置类，
		 * 然后调用每一个 ContextCustomizerFactoery的createContextCustomizer
		 *
		 * <code>
		 * # Spring Test ContextCustomizerFactories
		 * org.springframework.test.context.ContextCustomizerFactory=\
		 * org.springframework.boot.test.context.ImportsContextCustomizerFactory,\
		 * org.springframework.boot.test.context.filter.ExcludeFilterContextCustomizerFactory,\
		 * org.springframework.boot.test.json.DuplicateJsonObjectContextCustomizerFactory,\
		 * org.springframework.boot.test.mock.mockito.MockitoContextCustomizerFactory,\
		 * org.springframework.boot.test.web.client.TestRestTemplateContextCustomizerFactory,\
		 * org.springframework.boot.test.web.reactive.server.WebTestClientContextCustomizerFactory
		 * </code>
		 * 在这个配置类中有一个  TestRestTemplateContextCustomizerFactory，的createContextCustomizer方法 会创建一个TestRestTemplateContextCustomizer对象
		 * TestRestTemplateContextCustomizer对象内部的customizeContext 方法 判断如果 当前是web环境 则注册 registerTestRestTemplate(context);
		 *
		 * 因此我们的测试类中 可以直接 @Autowired TestResetTemplate.
		 *
		 * 需要注意的是  创建一个TestRestTemplateContextCustomizer对象, 这个对象本质上是ContextCustomizer 对象， 最终 ContextCustomizer 对象
		 * 又被ContextCustomizerAdapter 包装， 而 ContextCustomizerAdapter 本质上是实现了 ApplicationContextInitializer 接口。
		 * 因此最终 ContextCustomizer的执行是通过 ApplicationContextInitializer接口的调度。
		 * <code>
		 *     //对每一个ContextCustomizer 转为ContextCustomizerAdapter 本质上是ApplicationContextInitializer
		 *     	for (ContextCustomizer contextCustomizer : config.getContextCustomizers()) {
		 * 			initializers.add(new ContextCustomizerAdapter(contextCustomizer, config));
		 *        }
		 * </code>
		 *
		 *
		 *
		 */

		List<ContextCustomizerFactory> factories = getContextCustomizerFactories();
		Set<ContextCustomizer> customizers = new LinkedHashSet<>(factories.size());
		for (ContextCustomizerFactory factory : factories) {
			ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
			if (customizer != null) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	/**
	 * Get the {@link ContextCustomizerFactory} instances for this bootstrapper.
	 * <p>The default implementation uses the {@link SpringFactoriesLoader} mechanism
	 * for loading factories configured in all {@code META-INF/spring.factories}
	 * files on the classpath.
	 * @since 4.3
	 * @see SpringFactoriesLoader#loadFactories
	 */
	protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
		return SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader());
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the
	 * supplied list of {@link ContextConfigurationAttributes} and then instantiate
	 * and return that {@code ContextLoader}.
	 * <p>If the user has not explicitly declared which loader to use, the value
	 * returned from {@link #getDefaultContextLoaderClass} will be used as the
	 * default context loader class. For details on the class resolution process,
	 * see {@link #resolveExplicitContextLoaderClass} and
	 * {@link #getDefaultContextLoaderClass}.
	 * @param testClass the test class for which the {@code ContextLoader} should be
	 * resolved; must not be {@code null}
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
	 * (never {@code null})
	 * @throws IllegalStateException if {@link #getDefaultContextLoaderClass(Class)}
	 * returns {@code null}
	 */
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(testClass, "Class must not be null");
		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
		if (contextLoaderClass == null) {
			contextLoaderClass = getDefaultContextLoaderClass(testClass);
		}
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
					contextLoaderClass.getName(), testClass.getName()));
		}
		return BeanUtils.instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
	 * list of {@link ContextConfigurationAttributes}.
	 * <p>Beginning with the first level in the context configuration attributes hierarchy:
	 * <ol>
	 * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
	 * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
	 * configured with an explicit class, that class will be returned.</li>
	 * <li>If an explicit {@code ContextLoader} class is not specified at the current
	 * level in the hierarchy, traverse to the next level in the hierarchy and return to
	 * step #1.</li>
	 * </ol>
	 * @param configAttributesList the list of configuration attributes to process;
	 * must not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the {@code ContextLoader} class to use for the supplied configuration
	 * attributes, or {@code null} if no explicit loader is found
	 * @throws IllegalArgumentException if supplied configuration attributes are
	 * {@code null} or <em>empty</em>
	 */
	@Nullable
	protected Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Resolving ContextLoader for context configuration attributes %s",
						configAttributes));
			}
			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (ContextLoader.class != contextLoaderClass) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
							"Found explicit ContextLoader class [%s] for context configuration attributes %s",
							contextLoaderClass.getName(), configAttributes));
				}
				return contextLoaderClass;
			}
		}
		return null;
	}

	/**
	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
	 * interaction with the {@code ContextCache}.
	 * <p>The default implementation simply delegates to
	 * {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()}.
	 * <p>Concrete subclasses may choose to override this method to return a custom
	 * {@code CacheAwareContextLoaderDelegate} implementation with custom
	 * {@link org.springframework.test.context.cache.ContextCache ContextCache} support.
	 * @return the context loader delegate (never {@code null})
	 */
	protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return getBootstrapContext().getCacheAwareContextLoaderDelegate();
	}

	/**
	 * Determine the default {@link ContextLoader} {@linkplain Class class}
	 * to use for the supplied test class.
	 * <p>The class returned by this method will only be used if a {@code ContextLoader}
	 * class has not been explicitly declared via {@link ContextConfiguration#loader}.
	 * @param testClass the test class for which to retrieve the default
	 * {@code ContextLoader} class
	 * @return the default {@code ContextLoader} class for the supplied test class
	 * (never {@code null})
	 */
	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

	/**
	 * Process the supplied, newly instantiated {@link MergedContextConfiguration} instance.
	 * <p>The returned {@link MergedContextConfiguration} instance may be a wrapper
	 * around or a replacement for the original.
	 * <p>The default implementation simply returns the supplied instance unmodified.
	 * <p>Concrete subclasses may choose to return a specialized subclass of
	 * {@link MergedContextConfiguration} based on properties in the supplied instance.
	 * @param mergedConfig the {@code MergedContextConfiguration} to process; never {@code null}
	 * @return a fully initialized {@code MergedContextConfiguration}; never {@code null}
	 */
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return mergedConfig;
	}


	private static boolean areAllEmpty(Collection<?>... collections) {
		return Arrays.stream(collections).allMatch(Collection::isEmpty);
	}

}
