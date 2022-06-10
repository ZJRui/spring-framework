/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility methods for {@link SmartContextLoader SmartContextLoaders} that deal
 * with component classes (e.g., {@link Configuration @Configuration} classes).
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
public abstract class AnnotationConfigContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoaderUtils.class);


	/**
	 * Detect the default configuration classes for the supplied test class.
	 * <p>The returned class array will contain all static nested classes of
	 * the supplied class that meet the requirements for {@code @Configuration}
	 * class implementations as specified in the documentation for
	 * {@link Configuration @Configuration}.
	 * <p>The implementation of this method adheres to the contract defined in the
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
	 * SPI. Specifically, this method uses introspection to detect default
	 * configuration classes that comply with the constraints required of
	 * {@code @Configuration} class implementations. If a potential candidate
	 * configuration class does not meet these requirements, this method will log a
	 * debug message, and the potential candidate class will be ignored.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never {@code null}
	 */
	public static Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");
		/**
		 * 检测提供的测试类的默认配置类。
		 * 返回的类数组将包含所提供类的所有静态嵌套类，它们满足@Configuration类实现的要求，如@Configuration文档中指定的那样。
		 * 这个方法的实现遵循在SmartContextLoader SPI中定义的契约。具体来说，该方法使用内省来检测符合@Configuration类实现所需约束的默认配置类。
		 * 如果潜在的候选配置类不满足这些要求，该方法将记录一条调试消息，并且将忽略潜在的候选类。
		 */

		List<Class<?>> configClasses = new ArrayList<>();

		/**
		 *
		 *getDeclaredClasses得到该类所有的内部类，除去父类的。
		 *
		 * 首先要注意当前类是 测试 模块下的类，正式启动的时候不会执行这里的逻辑
		 *
		 * 实际测试发现: 非测试路径下的配置类， 如果这个配置类能被加载那么这个配置类中的静态和非静态的内部配置类都能 使用@Bean注入Bean。但是一般我们都是使用静态内部类。
		 *
		 * 在运行测试用例的时候： 项目正式路径下的配置类不会 被这个detectDefaultConfigurationClasses 方法执行。也就是说 如果你的测试类能够 扫描到了 正式路径下的配置类，
		 * 那么这个正式配置类中的静态和非静态内部配置类中的@Bean都会生效。
		 *
		 *
		 * 在运行测试用例的时候 我们会 分析测试类上的@ContextConfigruation注解  或者通过这里的declaringClass等于测试类 分析测试类内部是否有静态内部配置类 来作为配置类。
		 * 如果发现了有静态内部类作为配置了，那么这个时候就不会再 寻找@SpringBootConfiguration 注解标注的类 作为配置类了。
		 *
		 * 以上对于 静态内部类生效的前提条件是这个静态内部类没有使用 @TestConfiguration注解标注，在没有使用@@TestConfiguration注解标注的情况下，我们说这个配置类可以替代@SpringBoootApplication
		 *
		 * 如果 静态内部配置类使用了@TestConfiguration注解标注，那么这个静态内部类只是对@SpringBootappliction配置类的补充，因此还会继续寻找 @SpringBootApplication注解标注的类
		 *
		 *
		 *
		 *
		 *
		 *
		 *
		 */
		//Springboot的实现：   这个实现中体现了 什么时候 会使用 @SpringBootApplication注解类作为配置类，什么时候不会使用他
//		protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
//			Class<?>[] classes = mergedConfig.getClasses();
		//---》 注意这里的containsNonTestComponent 会娇艳 classes上是否存在@TestConfiguration注解，如果没有@TestConfiguration注解则表明这个类可以替代@SpringBootApplication配置了。如果有@TestConfiguration配置类则说明是对@SpringBootApplication配置类的补充。

//			if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
//				return classes;
//			}
//			Class<?> found = new AnnotatedClassFinder(SpringBootConfiguration.class)
//					.findFromClass(mergedConfig.getTestClass());
//			Assert.state(found != null, "Unable to find a @SpringBootConfiguration, you need to use "
//					+ "@ContextConfiguration or @SpringBootTest(classes=...) with your test");
//			logger.info("Found @SpringBootConfiguration " + found.getName() + " for test " + mergedConfig.getTestClass());
//			return merge(found, classes);
//		}
		for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
			/**
			 * 内部 配置类 必须要是 非private 非 final 且静态才可以作为配置类
			 */
			if (isDefaultConfigurationClassCandidate(candidate)) {
				configClasses.add(candidate);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Ignoring class [%s]; it must be static, non-private, non-final, and annotated " +
								"with @Configuration to be considered a default configuration class.",
						candidate.getName()));
				}
			}
		}

		if (configClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Could not detect default configuration classes for test class [%s]: " +
						"%s does not declare any static, non-private, non-final, nested classes " +
						"annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
			}
		}

		return ClassUtils.toClassArray(configClasses);
	}

	/**
	 * Determine if the supplied {@link Class} meets the criteria for being
	 * considered a <em>default configuration class</em> candidate.
	 * <p>Specifically, such candidates:
	 * <ul>
	 * <li>must not be {@code null}</li>
	 * <li>must not be {@code private}</li>
	 * <li>must not be {@code final}</li>
	 * <li>must be {@code static}</li>
	 * <li>must be annotated or meta-annotated with {@code @Configuration}</li>
	 * </ul>
	 * @param clazz the class to check
	 * @return {@code true} if the supplied class meets the candidate criteria
	 */
	private static boolean isDefaultConfigurationClassCandidate(@Nullable Class<?> clazz) {
		return (clazz != null && isStaticNonPrivateAndNonFinal(clazz) &&
				AnnotatedElementUtils.hasAnnotation(clazz, Configuration.class));
	}

	private static boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		int modifiers = clazz.getModifiers();
		return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
	}

}
