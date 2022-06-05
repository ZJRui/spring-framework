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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Indicates that a component is eligible for registration when one or more
 * {@linkplain #value specified profiles} are active.
 *
 * <p>A <em>profile</em> is a named logical grouping that may be activated
 * programmatically via {@link ConfigurableEnvironment#setActiveProfiles} or declaratively
 * by setting the {@link AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * spring.profiles.active} property as a JVM system property, as an
 * environment variable, or as a Servlet context parameter in {@code web.xml}
 * for web applications. Profiles may also be activated declaratively in
 * integration tests via the {@code @ActiveProfiles} annotation.
 *
 * <p>The {@code @Profile} annotation may be used in any of the following ways:
 * <ul>
 * <li>as a type-level annotation on any class directly or indirectly annotated with
 * {@code @Component}, including {@link Configuration @Configuration} classes</li>
 * <li>as a meta-annotation, for the purpose of composing custom stereotype annotations</li>
 * <li>as a method-level annotation on any {@link Bean @Bean} method</li>
 * </ul>
 *
 * <p>If a {@code @Configuration} class is marked with {@code @Profile}, all of the
 * {@code @Bean} methods and {@link Import @Import} annotations associated with that class
 * will be bypassed unless one or more of the specified profiles are active. A profile
 * string may contain a simple profile name (for example {@code "p1"}) or a profile
 * expression. A profile expression allows for more complicated profile logic to be
 * expressed, for example {@code "p1 & p2"}. See {@link Profiles#of(String...)} for more
 * details about supported formats.
 *
 * <p>This is analogous to the behavior in Spring XML: if the {@code profile} attribute of
 * the {@code beans} element is supplied e.g., {@code <beans profile="p1,p2">}, the
 * {@code beans} element will not be parsed unless at least profile 'p1' or 'p2' has been
 * activated. Likewise, if a {@code @Component} or {@code @Configuration} class is marked
 * with {@code @Profile({"p1", "p2"})}, that class will not be registered or processed unless
 * at least profile 'p1' or 'p2' has been activated.
 *
 * <p>If a given profile is prefixed with the NOT operator ({@code !}), the annotated
 * component will be registered if the profile is <em>not</em> active &mdash; for example,
 * given {@code @Profile({"p1", "!p2"})}, registration will occur if profile 'p1' is active
 * or if profile 'p2' is <em>not</em> active.
 *
 * <p>If the {@code @Profile} annotation is omitted, registration will occur regardless
 * of which (if any) profiles are active.
 *
 * <p><b>NOTE:</b> With {@code @Profile} on {@code @Bean} methods, a special scenario may
 * apply: In the case of overloaded {@code @Bean} methods of the same Java method name
 * (analogous to constructor overloading), an {@code @Profile} condition needs to be
 * consistently declared on all overloaded methods. If the conditions are inconsistent,
 * only the condition on the first declaration among the overloaded methods will matter.
 * {@code @Profile} can therefore not be used to select an overloaded method with a
 * particular argument signature over another; resolution between all factory methods
 * for the same bean follows Spring's constructor resolution algorithm at creation time.
 * <b>Use distinct Java method names pointing to the same {@link Bean#name bean name}
 * if you'd like to define alternative beans with different profile conditions</b>;
 * see {@code ProfileDatabaseConfig} in {@link Configuration @Configuration}'s javadoc.
 *
 * <p>When defining Spring beans via XML, the {@code "profile"} attribute of the
 * {@code <beans>} element may be used. See the documentation in the
 * {@code spring-beans} XSD (version 3.1 or greater) for details.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see ConfigurableEnvironment#setActiveProfiles
 * @see ConfigurableEnvironment#setDefaultProfiles
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
 * @see Conditional
 * @see org.springframework.test.context.ActiveProfiles
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

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



	/**
	 * The set of profiles for which the annotated component should be registered.
	 */
	String[] value();

}
