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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface to be implemented by types that register additional bean definitions when
 * processing @{@link Configuration} classes. Useful when operating at the bean definition
 * level (as opposed to {@code @Bean} method/instance level) is desired or necessary.
 *
 * <p>Along with {@code @Configuration} and {@link ImportSelector}, classes of this type
 * may be provided to the @{@link Import} annotation (or may also be returned from an
 * {@code ImportSelector}).
 *
 * <p>An {@link ImportBeanDefinitionRegistrar} may implement any of the following
 * {@link org.springframework.beans.factory.Aware Aware} interfaces, and their respective
 * methods will be called prior to {@link #registerBeanDefinitions}:
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
 * </ul>
 *
 * <p>See implementations and associated unit tests for usage examples.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Import
 * @see ImportSelector
 * @see Configuration
 */
public interface ImportBeanDefinitionRegistrar {

	/**
	 * Register bean definitions as necessary based on the given annotation metadata of
	 * the importing {@code @Configuration} class.
	 * <p>Note that {@link BeanDefinitionRegistryPostProcessor} types may <em>not</em> be
	 * registered here, due to lifecycle constraints related to {@code @Configuration}
	 * class processing.
	 *
	 * 什么时候被调用？
	 * registerBeanDefinitions:86, ImportBeanDefinitionRegistrar (org.springframework.context.annotation)
	 * lambda$loadBeanDefinitionsFromRegistrars$1:396, ConfigurationClassBeanDefinitionReader (org.springframework.context.annotation)
	 * accept:-1, 380616082 (org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader$$Lambda$266)
	 * forEach:684, LinkedHashMap (java.util)
	 * loadBeanDefinitionsFromRegistrars:395, ConfigurationClassBeanDefinitionReader (org.springframework.context.annotation)
	 * loadBeanDefinitionsForConfigurationClass:157, ConfigurationClassBeanDefinitionReader (org.springframework.context.annotation)
	 * loadBeanDefinitions:129, ConfigurationClassBeanDefinitionReader (org.springframework.context.annotation)
	 * processConfigBeanDefinitions:343, ConfigurationClassPostProcessor (org.springframework.context.annotation)
	 * postProcessBeanDefinitionRegistry:247, ConfigurationClassPostProcessor (org.springframework.context.annotation)
	 * invokeBeanDefinitionRegistryPostProcessors:311, PostProcessorRegistrationDelegate (org.springframework.context.support)
	 * invokeBeanFactoryPostProcessors:112, PostProcessorRegistrationDelegate (org.springframework.context.support)
	 * invokeBeanFactoryPostProcessors:746, AbstractApplicationContext (org.springframework.context.support)
	 * refresh:564, AbstractApplicationContext (org.springframework.context.support)
	 * refresh:147, ServletWebServerApplicationContext (org.springframework.boot.web.servlet.context)
	 * refresh:734, SpringApplication (org.springframework.boot)
	 * refreshContext:408, SpringApplication (org.springframework.boot)
	 * run:308, SpringApplication (org.springframework.boot)
	 * run:1306, SpringApplication (org.springframework.boot)
	 * run:1295, SpringApplication (org.springframework.boot)
	 * main:10, SpringbootDemoApplication (com.example.springbootdemo)
	 *
	 * 也就是说在BeanFactoryPostProcessor的时候 会执行ConfigurationClassPostProcessor ，
	 *
	 * 然后这个Processor 会处理@Configuration注解的类， 而且还会处理这个类上的@Import注解
	 * 同时 还会通过getImportBeanDefinitionRegistrars方法获取这个类上配置的Registrars
	 * configClass.getImportBeanDefinitionRegistrars()
	 * 然后执行Registrars的registerBeanDefinitions 方法
	 *
	 *
	 * 你像@SpringbootApplication 集合了 @EnableAutoConfiguration ,然后 @EnableAutoConfiguration 集合了
	 * @AutoConfigurationPackage， 这个@AutoConfigPackage 注解 通过@Import注解 导入了AutoConfigurationPackages.registrar
	 *
	 * @Import(AutoConfigurationPackages.Registrar.class)
	 * public @interface AutoConfigurationPackage {}
	 *
	 * 而Registarar就是ImportBeanDefinitionRegistrar的子类
	 * 	static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {}
	 *
	 * 因此spring启动的时候就会执行 AutoConfigurationPackages.Registrar的registerBeanDefinitions
	 *
	 * ----------
	 * 还有
	 * @Import(EnableConfigurationPropertiesRegistrar.class)   这里也是导入了一个 Registrar
	 * public @interface EnableConfigurationProperties {}
	 *
	 *
	 * @param importingClassMetadata annotation metadata of the importing class
	 * @param registry current bean definition registry
	 */
	void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry);

}
