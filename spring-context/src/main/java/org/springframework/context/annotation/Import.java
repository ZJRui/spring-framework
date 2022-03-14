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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates one or more <em>component classes</em> to import &mdash; typically
 * {@link Configuration @Configuration} classes.
 *
 * <p>Provides functionality equivalent to the {@code <import/>} element in Spring XML.
 * Allows for importing {@code @Configuration} classes, {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations, as well as regular component
 * classes (as of 4.2; analogous to {@link AnnotationConfigApplicationContext#register}).
 *
 * <p>{@code @Bean} definitions declared in imported {@code @Configuration} classes should be
 * accessed by using {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * injection. Either the bean itself can be autowired, or the configuration class instance
 * declaring the bean can be autowired. The latter approach allows for explicit, IDE-friendly
 * navigation between {@code @Configuration} class methods.
 *
 * <p>May be declared at the class level or as a meta-annotation.
 *
 * <p>If XML or other non-{@code @Configuration} bean definition resources need to be
 * imported, use the {@link ImportResource @ImportResource} annotation instead.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Configuration
 * @see ImportSelector
 * @see ImportResource
 */

/**
 *
 * 之前有一个疑问 @SpringBootApplication这个注解是如何生效的？
 * SpringBootApplication注解 聚合了@EnableAutoConfiguration主机，这个EnableAutoConfiguration注解通过@import注解导入了一个ImportSelector实现类
 *
 * 在这个ImportSelector的实现类的方法中通过SpringFactoriesLoader 加载了Spring.factories配置文件中key为EnableAutoConfiguration的配置项。
 *
 * 那么问题究竟 是什么时候 处理了@SpringBootApplication这个注解呢？
 *
 * 实际上是在BeanFactoryPostProcessor 容器后置处理器
 * ConfigurationClassPostProcessor这个处理器中 会解析 判断这个类上是否存在@Configuration注解，以及是否存在@Import注解。
 * 如果存在则就 对@Import注解进行解析。 从而把spring.factories配置文件中的配置类纳入到spring的ioc容器中。
 *
 *
 *
 * ConfigurationClassParser.getImports(SourceClass)  (org.springframework.context.annotation)
 *     ConfigurationClassParser.doProcessConfigurationClass(ConfigurationClass, SourceClass)  (org.springframework.context.annotation)
 *         ConfigurationClassParser.processConfigurationClass(ConfigurationClass)  (org.springframework.context.annotation)
 *             ConfigurationClassParser.parse(String, String)  (org.springframework.context.annotation)
 *                 ConfigurationClassParser.doProcessConfigurationClass(ConfigurationClass, SourceClass)  (org.springframework.context.annotation)
 *                 ConfigurationClassParser.parse(Set<BeanDefinitionHolder>)  (org.springframework.context.annotation)
 *                     ConfigurationClassPostProcessor.processConfigBeanDefinitions(BeanDefinitionRegistry)  (org.springframework.context.annotation)
 *                     //这里是  容器后置处理器 对class进行处理
 *                         ConfigurationClassPostProcessor.postProcessBeanFactory(ConfigurableListableBeanFactory)  (org.springframework.context.annotation)
 *
 *
 * 值得注意的是  ConfigurationClassPostProcessor 这个容器后置处理器 在调用 doProcessConfigurationClass 方法的时候 不仅仅 会处理@Configuration配置类上的
 * @Import注解，也会处理器上面的 @ComponentScan注解
 *
 *   但是有一点需要思考： @SpringBootApplication标记的类 本质上是使用了@Configuration，这个类被转为一个BeanDefinition，在容器的后置处理器中对BeanDefinition 进行处理的时候
 *   会判断BeanDefinition 这个类的class上是否存在@Configuration注解 @Import注解等等。
 *   那么@SpringBootApplication标记的类 又是什么时候 被 被转化 成了BeanDefinition 放置到了容器中的呢？ ComponentScan需要依赖于一个已经在容器中的BeanDefinition。
 *
 *
 *   SpringApplication。run方法接收一个类参数。 这个参数 会被 注册为一个BeanDefinition，这个就是入口点，下面的调用栈
 *   setBeanClass:409, AbstractBeanDefinition (org.springframework.beans.factory.support)
 * <init>:57, AnnotatedGenericBeanDefinition (org.springframework.beans.factory.annotation)
 * doRegisterBean:253, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
 * registerBean:147, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
 * register:137, AnnotatedBeanDefinitionReader (org.springframework.context.annotation)
 * load:157, BeanDefinitionLoader (org.springframework.boot)
 * load:136, BeanDefinitionLoader (org.springframework.boot)
 * load:128, BeanDefinitionLoader (org.springframework.boot)
 * load:691, SpringApplication (org.springframework.boot)
 * prepareContext:392, SpringApplication (org.springframework.boot)
 * run:314, SpringApplication (org.springframework.boot)
 * run:1237, SpringApplication (org.springframework.boot)
 * run:1226, SpringApplication (org.springframework.boot)
 * main:12, DemoApplication (com.example.demo)
 *
 * 从上的内容我们看到 在SpringAPplication的 prepareContext方法中 调用了BeanDefinitionLoader的load方法。在load的时候 会将
 * SpringApplication.run 方法接收到的参数 转化成一个BeanDefinition注册到Ioc容器中。
 * 然后在容器后置处理器中 对这个BeanDefinition 进行解析处理 ，判断beanClass上是否存在@Configuration注解
 *
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * {@link Configuration @Configuration}, {@link ImportSelector},
	 * {@link ImportBeanDefinitionRegistrar}, or regular component classes to import.
	 */
	Class<?>[] value();

}
