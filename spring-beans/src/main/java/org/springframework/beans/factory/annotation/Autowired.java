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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor, field, setter method, or config method as to be autowired by
 * Spring's dependency injection facilities. This is an alternative to the JSR-330
 * {@link javax.inject.Inject} annotation, adding required-vs-optional semantics.
 *
 * <p>将构造函数、字段、setter方法或配置方法标记为由Spring的依赖注入工具自动连接。这是JSR-330 javax.inject注释的替代方法，添加了required-vs-optional语义。
 * jsr-330 注解： javax.inject.Named，javax.inject.Inject，javax.inject.Qualifier，javax.inject.Scope，javax.inject.Singleton
 *
 * 在QualifierAnnotationAutowireCandidateResolver 的构造器中 你会发现  这个QualifierAnnotationAutowireCandidateResolver 注解解析器增加了对 jsr-330的支持，
 * 他获取了javax.inject.Qualifier 这个类
 * <p/>
 *
 *
 *
 *
 * <p>Only one constructor of any given bean class may declare this annotation with
 * the 'required' attribute set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Spring bean. Furthermore, if the 'required' attribute
 * is set to {@code true}, only a single constructor may be annotated with
 * {@code @Autowired}. If multiple <i>non-required</i> constructors declare the
 * annotation, they will be considered as candidates for autowiring. The constructor
 * with the greatest number of dependencies that can be satisfied by matching beans
 * in the Spring container will be chosen. If none of the candidates can be satisfied,
 * then a primary/default constructor (if present) will be used. If a class only
 * declares a single constructor to begin with, it will always be used, even if not
 * annotated. An annotated constructor does not have to be public.
 *
 * <p>
 *     任何给定bean类只有一个构造函数可以声明这个注释，并将'required'属性设置为true，
 *     表明当作为Spring bean使用时，构造函数将自动装配。此外，如果'required'属性设置为true，
 *     则只有一个构造函数可以使用@Autowired进行注释。如果多个非必需的构造函数声明了注释，
 *     它们将被认为是自动装配的候选者。通过匹配Spring容器中的bean，将选择具有最多依赖项数量的构
 *     造函数。如果没有一个候选构造函数可以满足要求，那么将使用一个主/默认构造函数(如果存在)。
 *     如果一个类在开始时只声明了一个构造函数，那么它将始终被使用，即使没有注释。
 *     带注释的构造函数不必是公共的。
 * </p>
 *
 * <p>Fields are injected right after construction of a bean, before any config methods
 * are invoked. Such a config field does not have to be public.
 * <p>
 *     字段是在构建bean之后，在调用任何配置方法之前注入的。这样的配置字段不必是公共的。
 * </p>
 *
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Spring container.
 * Bean property setter methods are effectively just a special case of such a general
 * config method. Such config methods do not have to be public.
 *
 * <p>
 *     配置方法可以有任意的名称和任意数量的参数;每个参数都将与Spring容器中的匹配bean自动连接。
 *     Bean属性setter方法实际上只是这种通用配置方法的一种特殊情况。这样的配置方法不必是公共的。
 * </p>
 *
 * <p>In the case of a multi-arg constructor or method, the 'required' attribute is
 * applicable to all arguments. Individual parameters may be declared as Java-8-style
 * {@link java.util.Optional} or, as of Spring Framework 5.0, also as {@code @Nullable}
 * or a not-null parameter type in Kotlin, overriding the base required semantics.
 *
 * <p>In case of a {@link java.util.Collection} or {@link java.util.Map} dependency type,
 * the container autowires all beans matching the declared value type. For such purposes,
 * the map keys must be declared as type String which will be resolved to the corresponding
 * bean names. Such a container-provided collection will be ordered, taking into account
 * {@link org.springframework.core.Ordered}/{@link org.springframework.core.annotation.Order}
 * values of the target components, otherwise following their registration order in the
 * container. Alternatively, a single matching target bean may also be a generally typed
 * {@code Collection} or {@code Map} itself, getting injected as such.
 *
 * <p>Note that actual injection is performed through a
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} which in turn means that you <em>cannot</em>
 * use {@code @Autowired} to inject references into
 * {@link org.springframework.beans.factory.config.BeanPostProcessor
 * BeanPostProcessor} or
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * types. Please consult the javadoc for the {@link AutowiredAnnotationBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * Declares whether the annotated dependency is required.
	 * <p>Defaults to {@code true}.
	 */
	boolean required() default true;

}
