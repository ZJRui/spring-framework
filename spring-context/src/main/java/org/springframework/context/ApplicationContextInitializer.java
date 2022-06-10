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

package org.springframework.context;

/**
 * Callback interface for initializing a Spring {@link ConfigurableApplicationContext}
 * prior to being {@linkplain ConfigurableApplicationContext#refresh() refreshed}.
 *
 * <p>Typically used within web applications that require some programmatic initialization
 * of the application context. For example, registering property sources or activating
 * profiles against the {@linkplain ConfigurableApplicationContext#getEnvironment()
 * context's environment}. See {@code ContextLoader} and {@code FrameworkServlet} support
 * for declaring a "contextInitializerClasses" context-param and init-param, respectively.
 *
 * <p>{@code ApplicationContextInitializer} processors are encouraged to detect
 * whether Spring's {@link org.springframework.core.Ordered Ordered} interface has been
 * implemented or if the @{@link org.springframework.core.annotation.Order Order}
 * annotation is present and to sort instances accordingly if so prior to invocation.
 *
 * @author Chris Beams
 * @since 3.1
 * @param <C> the application context type
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * Initialize the given application context.
	 *
	 * ApplicationContextInitializer 接口用于在 Spring 容器刷新之前执行的一个回调函数，
	 * 通常用于向 SpringBoot 容器中注入属性。
	 * 比如注入别名：
	 * @Override
	 * public void initialize(GenericApplicationContext applicationContext) {
	 * 		applicationContext.registerAlias("foo", "bar");
	 *    }
	 *
	 * Springboot中有较多的内置实现类：
	 * 1.DelegatingApplicationContextInitializer：使用环境属性 context.initializer.classes 指定的初始化
	 * 器(initializers)进行初始化工作，如果没有指定则什么都不做。通过它使得我们可以把自定义实现类配置在 application.properties 里成为了可能。
	 *
	 * 2.ServerPortInfoApplicationContextInitializer
	 *  将内置 servlet容器实际使用的监听端口写入到 Environment 环境属性中。这样属性 local.server.port 就可以直接通过 @Value 注入到测试中，或者通过环境属性 Environment 获取。
	 *  比如测试类中 启动随机端口，如何获取这个随机端口
	 *     @LocalServerPort
	 *     private int port;
	 *
	 *
	 * 问题： 这个方法什么时候被调用？ 方法接受的参数是 ApplicationContext， 那么这个Context是已经 初始化好了么？
	 *
	 * 关于ApplicationContextInitializer和Spring的关系
	 *
	 * 注意我们查看 initialize函数的调用栈会发现， 他有两个调用地方（1）ContextLoaderListener （2）FrameworkServlet
	 *
	 *对于ContextLoaderListener 我们知道 他实现了Spring的contextLoader接口，同时也实现了Servlet规范的servletContextListener接口。
	 * 当ServletContext创建的时候 就会调用这个ContextLoaderListener创建 Spring容器（父容器）。
	 *
	 * 从 org.springframework.web.context.ContextLoader#determineContextInitializerClasses(javax.servlet.ServletContext) 的代码
	 * 中我们可以看到 Spring是如何寻找 有哪些ApplicationContextInitializer可用的， 他主要是从servletContext的配置信息中获取配置的类
	 * 同时在 org.springframework.web.context.ContextLoader#customizeContext(javax.servlet.ServletContext, org.springframework.web.context.ConfigurableWebApplicationContext)
	 * 方法中我们看到 当 从Servletcontext的配置信息中获取到 ApplicationContextInitializer 类之后 会通过反射技术创建 对应的对象。
	 *
	 * 因此： 第一个问题就是 ApplicationContextInitializer 实现类不需要手动 注册到Spring容器中。Spring的listener是需要手动注入的。
	 *
	 * 而且值得注意的是 ContextLoaderListener或者FrameworkServlet的org.springframework.web.servlet.FrameworkServlet#applyInitializers(org.springframework.context.ConfigurableApplicationContext)
	 * 方法中 反射创建的 ApplicationContextInitializer 都被 有放入到 Spring容器中，而是被自身缓存了起来。 也就是说
	 * ApplicationContextInitializer是独立与Spring容器之外的。
	 *
	 *
	 *
	 * 3：哪里调用的？
	 * 结合这篇文章可以很容易知道，SpringBoot中的调用是在方法org.springframework.boot.SpringApplication#prepareContext,源码如下：
	 *
	 * private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment,
	 * 			SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments, Banner printedBanner) {
	 * 	...snip...
	 * 	// <202106071723>
	 * 	applyInitializers(context);
	 * 	...snip...
	 * }
	 * <202106071723>处源码如下：
	 *
	 * org.springframework.boot.SpringApplication#applyInitializers
	 * // 在configurable application context刷新之前，应用所有的ApplicationContextInitializer
	 * @SuppressWarnings({ "rawtypes", "unchecked" })
	 * protected void applyInitializers(ConfigurableApplicationContext context) {
	 * 	// 获取所有的ApplicationContextInitializer，并循环调用其initialize方法
	 * 	for (ApplicationContextInitializer initializer : getInitializers()) {
	 * 		Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
	 * 				ApplicationContextInitializer.class);
	 * 		Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
	 * 		initializer.initialize(context);
	 *      }
	 * }

	 *
	 *
	 *
	 *
	 *
	 * @param applicationContext the application to configure
	 */
	void initialize(C applicationContext);

}
