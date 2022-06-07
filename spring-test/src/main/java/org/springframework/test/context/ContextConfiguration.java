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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AliasFor;

/**
 * {@code @ContextConfiguration} defines class-level metadata that is used to determine
 * how to load and configure an {@link org.springframework.context.ApplicationContext
 * ApplicationContext} for integration tests.
 *
 * <h3>Supported Resource Types</h3>
 *
 * <p>Prior to Spring 3.1, only path-based resource locations (typically XML configuration
 * files) were supported. As of Spring 3.1, {@linkplain #loader context loaders} may
 * choose to support <em>either</em> path-based <em>or</em> class-based resources. As of
 * Spring 4.0.4, {@linkplain #loader context loaders} may choose to support path-based
 * <em>and</em> class-based resources simultaneously. Consequently
 * {@code @ContextConfiguration} can be used to declare either path-based resource
 * locations (via the {@link #locations} or {@link #value} attribute) <em>or</em>
 * component classes (via the {@link #classes} attribute). Note, however, that most
 * implementations of {@link SmartContextLoader} only support a single resource type. As
 * of Spring 4.1, path-based resource locations may be either XML configuration files or
 * Groovy scripts (if Groovy is on the classpath). Of course, third-party frameworks may
 * choose to support additional types of path-based resources.
 *
 * <h3>Component Classes</h3>
 *
 * <p>The term <em>component class</em> can refer to any of the following.
 *
 * <ul>
 * <li>A class annotated with {@link org.springframework.context.annotation.Configuration @Configuration}</li>
 * <li>A component (i.e., a class annotated with
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Service @Service},
 * {@link org.springframework.stereotype.Repository @Repository}, etc.)</li>
 * <li>A JSR-330 compliant class that is annotated with {@code javax.inject} annotations</li>
 * <li>Any class that contains {@link org.springframework.context.annotation.Bean @Bean}-methods</li>
 * <li>Any other class that is intended to be registered as a Spring component (i.e., a Spring bean
 * in the {@code ApplicationContext}), potentially taking advantage of automatic autowiring of a
 * single constructor without the use of Spring annotations</li>
 * </ul>
 *
 * A bean will be registered in the {@code ApplicationContext} for each component
 * class, and such beans can therefore be injected into other beans or into the
 * instance of the test class.
 *
 * <p>Consult the Javadoc for {@link org.springframework.context.annotation.Configuration @Configuration}
 * and {@link org.springframework.context.annotation.Bean @Bean} for further
 * information regarding the configuration and semantics of <em>component classes</em>.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @ContextConfiguration定义了类级别的元数据，用于确定如何为集成测试加载和配置ApplicationContext。
 * 支持资源类型
 * 在Spring 3.1之前，只支持基于路径的资源位置(通常是XML配置文件)。
 * 从Spring 3.1开始，上下文加载器可以选择支持基于路径的资源或基于类的资源。
 * 从Spring 4.0.4开始，上下文加载器可以选择同时支持基于路径的资源和基于类的资源。
 * 因此，@ContextConfiguration可以用来声明基于路径的资源位置(通过location或value属性)或组件类(通过classes属性)。
 * 但是请注意，大多数SmartContextLoader的实现只支持一种资源类型。从Spring 4.1开始，
 * 基于路径的资源位置可以是XML配置文件或Groovy脚本(如果Groovy在类路径上)。当然，第三方框架也可以选择支持其他类型的基于路径的资源。
 * 组件类
 * 术语组件类可以指以下任何一种。
 * 用@Configuration注释的类
 * 一个组件(例如，一个用@Component、@Service、@Repository等标注的类)
 * 一个兼容JSR-330的类，用javax做了注释。注入注解
 * 任何包含@ bean方法的类
 * 任何其他打算注册为Spring组件的类(例如，ApplicationContext中的Spring bean)，可能利用单个构造函数的自动装配，而不使用Spring注释
 * 将在ApplicationContext中为每个组件类注册一个bean，这样的bean就可以注入到其他bean或测试类的实例中。
 * 有关组件类的配置和语义的进一步信息，请参阅Javadoc的@Configuration和@Bean。
 * 这个注释可以作为元注释来创建自定义的组合注释。
 * 自:
 * 2.5
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextHierarchy
 * @see ActiveProfiles
 * @see TestPropertySource
 * @see ContextLoader
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 * @see org.springframework.context.ApplicationContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextConfiguration {

	/**
	 *
	 * @ContextConfiguration Spring整合JUnit4测试时，使用注解引入多个配置文件
	 * 1.1 单个文件
	 * @ContextConfiguration(locations="../applicationContext.xml")
	 *
	 * @ContextConfiguration(classes = SimpleConfiguration.class)
	 * 可用{}
	 *
	 * @ContextConfiguration(locations = { "classpath*:/spring1.xml", "classpath*:/spring2.xml" })
	 *
	 * 1.3 默认不写
	 * 可以根据测试的类名，去找到与之对应的配置文件。比如当前 使用@ContextConfiguration 标记的类 A， 如果没有配置注解的value属性
	 * 那么就会在对应的resource下的类目录下寻找A-context.xml
	 * 信息: Could not detect default resource locations for test class
	 * [soundsystem.CNamespaceValueTest]: class path resource [soundsystem/CNamespaceValueTest-context.xml] does not exist
	 *
	 *
	 */

	/**
	 * Alias for {@link #locations}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #locations}, but it may be used instead of {@link #locations}.
	 * @since 3.0
	 * @see #inheritLocations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * The resource locations to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>Check out the Javadoc for
	 * {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 * AbstractContextLoader.modifyLocations()} for details on how a location
	 * will be interpreted at runtime, in particular in case of a relative
	 * path. Also, check out the documentation on
	 * {@link org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 * AbstractContextLoader.generateDefaultLocations()} for details on the
	 * default locations that are going to be used if none are specified.
	 * <p>Note that the aforementioned default rules only apply for a standard
	 * {@link org.springframework.test.context.support.AbstractContextLoader
	 * AbstractContextLoader} subclass such as
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader} or
	 * {@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader}
	 * which are the effective default implementations used at runtime if
	 * {@code locations} are configured. See the documentation for {@link #loader}
	 * for further details regarding default loaders.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used instead of {@link #value}.
	 * @since 2.5
	 * @see #inheritLocations
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * The <em>component classes</em> to use for loading an
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
	 * <p>Check out the javadoc for
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader#detectDefaultConfigurationClasses
	 * AnnotationConfigContextLoader.detectDefaultConfigurationClasses()} for details
	 * on how default configuration classes will be detected if no
	 * <em>component classes</em> are specified. See the documentation for
	 * {@link #loader} for further details regarding default loaders.
	 * @since 3.1
	 * @see org.springframework.context.annotation.Configuration
	 * @see org.springframework.test.context.support.AnnotationConfigContextLoader
	 * @see #inheritLocations
	 */
	Class<?>[] classes() default {};

	/**
	 * The application context <em>initializer classes</em> to use for initializing
	 * a {@link ConfigurableApplicationContext}.
	 * <p>The concrete {@code ConfigurableApplicationContext} type supported by each
	 * declared initializer must be compatible with the type of {@code ApplicationContext}
	 * created by the {@link SmartContextLoader} in use.
	 * <p>{@code SmartContextLoader} implementations typically detect whether
	 * Spring's {@link org.springframework.core.Ordered Ordered} interface has been
	 * implemented or if the @{@link org.springframework.core.annotation.Order Order}
	 * annotation is present and sort instances accordingly prior to invoking them.
	 * @since 3.2
	 * @see org.springframework.context.ApplicationContextInitializer
	 * @see org.springframework.context.ConfigurableApplicationContext
	 * @see #inheritInitializers
	 * @see #loader
	 */
	Class<? extends ApplicationContextInitializer<?>>[] initializers() default {};

	/**
	 * Whether or not {@linkplain #locations resource locations} or
	 * {@linkplain #classes <em>component classes</em>} from test superclasses
	 * should be <em>inherited</em>.
	 * <p>The default value is {@code true}. This means that an annotated test
	 * class will <em>inherit</em> the resource locations or component classes
	 * defined by test superclasses. Specifically, the resource locations or
	 * component classes for a given test class will be appended to the list of
	 * resource locations or component classes defined by test superclasses.
	 * Thus, subclasses have the option of <em>extending</em> the list of resource
	 * locations or component classes.
	 * <p>If {@code inheritLocations} is set to {@code false}, the
	 * resource locations or component classes for the annotated test class
	 * will <em>shadow</em> and effectively replace any resource locations
	 * or component classes defined by superclasses.
	 * <p>In the following example that uses path-based resource locations, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be loaded from
	 * {@code "base-context.xml"} <strong>and</strong>
	 * {@code "extended-context.xml"}, in that order. Beans defined in
	 * {@code "extended-context.xml"} may therefore override those defined
	 * in {@code "base-context.xml"}.
	 * <pre class="code">
	 * &#064;ContextConfiguration("base-context.xml")
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration("extended-context.xml")
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * <p>Similarly, in the following example that uses component classes, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be loaded from the
	 * {@code BaseConfig} <strong>and</strong> {@code ExtendedConfig}
	 * configuration classes, in that order. Beans defined in
	 * {@code ExtendedConfig} may therefore override those defined in
	 * {@code BaseConfig}.
	 * <pre class="code">
	 * &#064;ContextConfiguration(classes=BaseConfig.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(classes=ExtendedConfig.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * @since 2.5
	 */
	boolean inheritLocations() default true;

	/**
	 * Whether or not {@linkplain #initializers context initializers} from test
	 * superclasses should be <em>inherited</em>.
	 * <p>The default value is {@code true}. This means that an annotated test
	 * class will <em>inherit</em> the application context initializers defined
	 * by test superclasses. Specifically, the initializers for a given test
	 * class will be added to the set of initializers defined by test
	 * superclasses. Thus, subclasses have the option of <em>extending</em> the
	 * set of initializers.
	 * <p>If {@code inheritInitializers} is set to {@code false}, the initializers
	 * for the annotated test class will <em>shadow</em> and effectively replace
	 * any initializers defined by superclasses.
	 * <p>In the following example, the
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * for {@code ExtendedTest} will be initialized using
	 * {@code BaseInitializer} <strong>and</strong> {@code ExtendedInitializer}.
	 * Note, however, that the order in which the initializers are invoked
	 * depends on whether they implement {@link org.springframework.core.Ordered
	 * Ordered} or are annotated with {@link org.springframework.core.annotation.Order
	 * &#064;Order}.
	 * <pre class="code">
	 * &#064;ContextConfiguration(initializers = BaseInitializer.class)
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;ContextConfiguration(initializers = ExtendedInitializer.class)
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }
	 * </pre>
	 * @since 3.2
	 */
	boolean inheritInitializers() default true;

	/**
	 * The type of {@link SmartContextLoader} (or {@link ContextLoader}) to use
	 * for loading an {@link org.springframework.context.ApplicationContext
	 * ApplicationContext}.
	 * <p>If not specified, the loader will be inherited from the first superclass
	 * that is annotated or meta-annotated with {@code @ContextConfiguration} and
	 * specifies an explicit loader. If no class in the hierarchy specifies an
	 * explicit loader, a default loader will be used instead.
	 * <p>The default concrete implementation chosen at runtime will be either
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader
	 * DelegatingSmartContextLoader} or
	 * {@link org.springframework.test.context.web.WebDelegatingSmartContextLoader
	 * WebDelegatingSmartContextLoader} depending on the absence or presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration
	 * &#064;WebAppConfiguration}. For further details on the default behavior
	 * of various concrete {@code SmartContextLoaders}, check out the Javadoc for
	 * {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader},
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader},
	 * {@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader},
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader},
	 * {@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader},
	 * {@link org.springframework.test.context.web.GenericGroovyXmlWebContextLoader GenericGroovyXmlWebContextLoader}, and
	 * {@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}.
	 * @since 2.5
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

	/**
	 * The name of the context hierarchy level represented by this configuration.
	 * <p>If not specified the name will be inferred based on the numerical level
	 * within all declared contexts within the hierarchy.
	 * <p>This attribute is only applicable when used within a test class hierarchy
	 * that is configured using {@code @ContextHierarchy}, in which case the name
	 * can be used for <em>merging</em> or <em>overriding</em> this configuration
	 * with configuration of the same name in hierarchy levels defined in superclasses.
	 * See the Javadoc for {@link ContextHierarchy @ContextHierarchy} for details.
	 *
	 * 此配置表示的上下文层次结构级别的名称。如果未指定，则将根据层次结构中所有声明的上下文中的数字级别推断名称。
	 * 此属性仅在使用 @ContextHierarchy 配置的测试类层次结构中使用时适用，在这种情况下，
	 * 该名称可用于合并或覆盖此配置与超类中定义的层次结构级别中的相同名称的配置。有关详细信息，请参阅 @ContextHierarchy 的 Javadoc。
	 * @since 3.2.2
	 */
	String name() default "";

}
