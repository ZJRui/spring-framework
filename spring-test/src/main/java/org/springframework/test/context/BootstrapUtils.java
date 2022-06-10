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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
abstract class BootstrapUtils {

	private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
			"org.springframework.test.context.support.DefaultBootstrapContext";

	private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
			"org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.web.WebTestContextBootstrapper";

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
			"org.springframework.test.context.web.WebAppConfiguration";

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	/**
	 * Create the {@code BootstrapContext} for the specified {@linkplain Class test class}.
	 * <p>Uses reflection to create a {@link org.springframework.test.context.support.DefaultBootstrapContext}
	 * that uses a {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}.
	 * @param testClass the test class for which the bootstrap context should be created
	 * @return a new {@code BootstrapContext}; never {@code null}
	 */
	@SuppressWarnings("unchecked")
	static BootstrapContext createBootstrapContext(Class<?> testClass) {
		//创建 CacheAwareContextLoaderDelegate 对象
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
		Class<? extends BootstrapContext> clazz = null;
		try {
			/**
			 * 使用类加载器  加载默认的 bootstrapContext类 ： DefaultBootstrapContext
			 * 然后使用反射的技术创建DefaultBootstrapContext
			 *
			 *
			 */
			clazz = (Class<? extends BootstrapContext>) ClassUtils.forName(
					DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME, BootstrapUtils.class.getClassLoader());
			/**
			 * 注意 在 创建DefaultBootStrapContext 之前， 首先是 获取了DefaultBootstrapContext的构造器， 这个构造器的第一个参数
			 * 是一个Class类型， 第二个参数是CacheAwareContextLoaderDelegate
			 *
			 * 也就是 我们调用 了DefaultBootstrapContext 的构造器，传递了 testClassz作为第一个参数， cacheAwareContextLoaderDelegate
			 * 作为第二个参数
			 */
			Constructor<? extends BootstrapContext> constructor = clazz.getConstructor(
					Class.class, CacheAwareContextLoaderDelegate.class);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating BootstrapContext using constructor [%s]", constructor));
			}
			return BeanUtils.instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load BootstrapContext [" + clazz + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
		Class<? extends CacheAwareContextLoaderDelegate> clazz = null;
		try {
			clazz = (Class<? extends CacheAwareContextLoaderDelegate>) ClassUtils.forName(
				DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME, BootstrapUtils.class.getClassLoader());

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]",
					clazz.getName()));
			}
			return BeanUtils.instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load CacheAwareContextLoaderDelegate [" + clazz + "]", ex);
		}
	}

	/**
	 * Resolve the {@link TestContextBootstrapper} type for the test class in the
	 * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
	 * to the {@link BootstrapContext}.
	 * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
	 * the test class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type.
	 * Otherwise, either the
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} or the
	 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
	 * WebTestContextBootstrapper} will be used, depending on the presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}.
	 * @param bootstrapContext the bootstrap context to use
	 * @return a fully configured {@code TestContextBootstrapper}
	 */
	static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		/**
		 * BootstrapContext 对象持有 TestClass测试类的类对象 和一个CacheAwareContextLoaderDelegate 对象
		 */
		Class<?> testClass = bootstrapContext.getTestClass();

		Class<?> clazz = null;
		try {
			/**
			 * 解析测试类上的 BootstrapWith 注解
			 * 对于SpringBoot 的测试类  @SpringBootTest注解聚合了 @BootstrapWith(SpringBootTestContextBootstrapper.class)
			 *
			 * 当不使用SpringBoot时，默认情况下  解析得到的BootstrapWith的注解 得到的是 DefaultTestContextBootstrapper
			 */
			clazz = resolveExplicitTestContextBootstrapper(testClass);
			if (clazz == null) {
				clazz = resolveDefaultTestContextBootstrapper(testClass);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
						testClass.getName(), clazz.getName()));
			}
			/**
			 * @TestContextBootstrapper注解需要指定一个TestContextBootstrapper 类，默认情况下是
			 * DefaultTestContextBootstrapper， 这里就是通过反射创建 DefaultTestContextBootstrapper对象
			 *
			 * 那么TestContextBootstrapper有什么用呢？
			 * A TestContextBootstrapper is used by the TestContextManager to get the TestExecutionListeners for the current test and to build the TestContext that it manages.
			 * Configuration
			 * 也就是说 TestContextBootstrapper 这个类中有一个 buildTestContext 方法
			 * 这个 方法可以创建一个TextContext对象。
			 *
			 *
			 *
			 *
			 */
			TestContextBootstrapper testContextBootstrapper =
					BeanUtils.instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);
			return testContextBootstrapper;
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz +
					"]. Specify @BootstrapWith's 'value' attribute or make the default bootstrapper class available.",
					ex);
		}
	}

	@Nullable
	private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
		/**
		 *
		 *classes:-1, $Proxy7 (com.sun.proxy)---------》提取@Bootstrap注解中的value参数
		 * invoke0:-1, NativeMethodAccessorImpl (sun.reflect)
		 * invoke:62, NativeMethodAccessorImpl (sun.reflect)
		 * invoke:43, DelegatingMethodAccessorImpl (sun.reflect)
		 * invoke:498, Method (java.lang.reflect)
		 * isValid:112, AttributeMethods (org.springframework.core.annotation)
		 * getDeclaredAnnotations:460, AnnotationsScanner (org.springframework.core.annotation)------------》寻找@BootstrapWith注解
		 * isKnownEmpty:492, AnnotationsScanner (org.springframework.core.annotation)
		 * from:251, TypeMappedAnnotations (org.springframework.core.annotation)
		 * from:351, MergedAnnotations (org.springframework.core.annotation)
		 * from:330, MergedAnnotations (org.springframework.core.annotation)
		 * from:313, MergedAnnotations (org.springframework.core.annotation)
		 * from:300, MergedAnnotations (org.springframework.core.annotation)
		 * isAnnotationDeclaredLocally:675, AnnotationUtils (org.springframework.core.annotation)
		 * findAnnotationDescriptor:240, TestContextAnnotationUtils (org.springframework.test.context)
		 * findAnnotationDescriptor:214, TestContextAnnotationUtils (org.springframework.test.context)
		 * resolveExplicitTestContextBootstrapper:165, BootstrapUtils (org.springframework.test.context)
		 * resolveTestContextBootstrapper:138, BootstrapUtils (org.springframework.test.context)
		 * <init>:122, TestContextManager (org.springframework.test.context)------------->TestContextManager创建
		 *
		 *
		 * TestContextManager创建的时候户i执行如下内容， resolveTestContextBootstrapper 会触发 解析测试类上的@BootstrapWith注解 从而提取这个注解中的value参数
		 * 		this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
		 *
		 * 	对于SpringBoot测试类而言，他使用@SpringBootTest注解标注，这个@SpringBootTest 默认聚合了@BootstrapWith(SpringBootTestContextBootstrapper.class)
		 *
		 * 	因此他会创建SpringbootTestContextbootstrapper对象，然后 这个对象会负责解析SpringBootTest注解本身的内容。
		 *
		 * 	也就是说@SpringBootTest注解本身有两部分内容（1）聚合了Spring的注解（SpringBootTestContextBootstrapper），通过这个Sprig的注解告诉 spring 你可以
		 * 	我这个注解指定的springboot的类来完成某项工作 （2）SpringBoot框架自己的@SpringBootTest注解自身的 内容，比如@SpringBootTest注解内可以配置classes 、exclude等
		 * 	信息，Spring本身不会解析@SpringBootTest，因为这个是SpringBoot的注解， 但是SpringBoot可以通过Spring的注解告诉spring 使用springboot的SpringbootTestContextbootstrapper
		 * 	来解析@SPringBootTest
		 *
		 *
		 *
		 *
		 */
		Set<BootstrapWith> annotations = AnnotatedElementUtils.findAllMergedAnnotations(testClass, BootstrapWith.class);
		if (annotations.isEmpty()) {
			return null;
		}

		if (annotations.size() == 1) {
			return annotations.iterator().next().value();
		}

		// Allow directly-present annotation to override annotations that are meta-present.
		BootstrapWith bootstrapWith = testClass.getDeclaredAnnotation(BootstrapWith.class);
		if (bootstrapWith != null) {
			return bootstrapWith.value();
		}

		throw new IllegalStateException(String.format(
				"Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
				testClass.getName(), annotations));
	}

	private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
		ClassLoader classLoader = BootstrapUtils.class.getClassLoader();
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(testClass,
			WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, false, false);
		if (attributes != null) {
			return ClassUtils.forName(DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
		}
		return ClassUtils.forName(DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
	}

}
