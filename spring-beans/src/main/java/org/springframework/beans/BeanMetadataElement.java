/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by bean metadata elements
 * that carry a configuration source object.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("all")
public interface BeanMetadataElement {

	/**
	 * Return the configuration source {@code Object} for this metadata element
	 * (may be {@code null}).
	 *
	 *
	 * 翻译：这个接口提供了一个方法去获取配置源对象，其实就是我们的原文件。
	 * 我们可以理解为，当我们通过注解的方式定义了一个IndexService时，那么此时的IndexService对应的BeanDefinition通过getSource方
	 * 法返回的就是IndexService.class这个文件对应的一个File对象。
	 *
	 * 如果我们通过@Bean方式定义了一个IndexService的话，那么此时的source是被@Bean注解所标注的一个Mehthod对象。
	 *
	 * BeanDefinition还实现了BeanMetadataElement接口，这个接口可以返回元信息，什么是元信息呢？一个对象的元信息是类，
	 * 那么一个类的元信息是什么呢？是类文件的路径。BeanDefinition返回的元信息，即是类文件的路径，
	 * 这里要注意，如果是传入给spring应用上下文初始化的配置类，返回的元信息为null，是因为配置类是我们主动传入的，
	 * spring不需要类文件路径也能拿到这个类，而像标记了@Component的类，spring在扫描时需要先拿到类文件的路径，在通过路
	 * 径（classes\org\example\service\A1Service.class）推断时类的包名（org.example.service.A1Service）。
	 *
	 * 测试用例：
	 *
	 * @Test
	 * public void test08() {
	 *     AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(MyConfig.class);
	 *     BeanDefinition myConfigBd = ac.getBeanFactory().getBeanDefinition("myConfig");
	 *     System.out.println("myConfigBd source:" + myConfigBd.getSource());
	 *     BeanDefinition a1ServiceBd = ac.getBeanFactory().getBeanDefinition("a1Service");
	 *     System.out.println("a1Service source:" + a1ServiceBd.getSource());
	 *     System.out.println("________________");
	 *     ClassPathXmlApplicationContext cc = new ClassPathXmlApplicationContext("spring.xml");
	 *     BeanDefinition amyBd = cc.getBeanFactory().getBeanDefinition("amy");
	 *     System.out.println("amy source:" + amyBd.getSource());
	 * }
	 *
	 * 运行结果：
	 *
	 * myConfigBd source:null
	 * a1Service source:file [D:\F\java_space\spring-source\spring-bd\target\classes\org\example\service\A1Service.class]
	 * ________________
	 * amy source:null
	 *
	 */
	@Nullable
	Object getSource();

}
