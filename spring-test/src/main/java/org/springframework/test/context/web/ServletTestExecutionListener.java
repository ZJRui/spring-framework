/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.context.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@code TestExecutionListener} which provides mock Servlet API support to
 * {@link WebApplicationContext WebApplicationContexts} loaded by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Specifically, {@code ServletTestExecutionListener} sets up thread-local
 * state via Spring Web's {@link RequestContextHolder} during {@linkplain
 * #prepareTestInstance(TestContext) test instance preparation} and {@linkplain
 * #beforeTestMethod(TestContext) before each test method} and creates a {@link
 * MockHttpServletRequest}, {@link MockHttpServletResponse}, and
 * {@link ServletWebRequest} based on the {@link MockServletContext} present in
 * the {@code WebApplicationContext}. This listener also ensures that the
 * {@code MockHttpServletResponse} and {@code ServletWebRequest} can be injected
 * into the test instance, and once the test is complete this listener {@linkplain
 * #afterTestMethod(TestContext) cleans up} thread-local state.
 *
 * <p>Note that {@code ServletTestExecutionListener} is enabled by default but
 * generally takes no action if the {@linkplain TestContext#getTestClass() test
 * class} is not annotated with {@link WebAppConfiguration @WebAppConfiguration}.
 * See the javadocs for individual methods in this class for details.
 *
 *
 * TestExecutionListener为Spring TestContext框架加载的
 * WebApplicationContexts提供了模拟Servlet API支持。
 *
 * 具体来说，ServletTestExecutionListener在测试实例准备期间和每个测试方法之前
 * 通过Spring Web的RequestContextHolder设置线程本地状态，
 * 并基于WebApplicationContext中呈现的MockServletContext创建一个
 * MockHttpServletRequest、MockHttpServletResponse和ServletWebRequest。
 * 这个监听器还确保可以将MockHttpServletResponse和ServletWebRequest注入到测试
 * 实例中，一旦测试完成，这个监听器就会清除线程本地状态。
 *
 * 注意ServletTestExecutionListener在默认情况下是启用的，
 * 但是如果测试类没有使用@WebAppConfiguration注释，
 * 通常不会采取任何操作。有关这个类中的各个方法的详细信息，请参阅javadoc。
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 */
@SuppressWarnings("AlibabaRemoveCommentedCode")
public class ServletTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates
	 * whether or not the {@code ServletTestExecutionListener} should {@linkplain
	 * RequestContextHolder#resetRequestAttributes() reset} Spring Web's
	 * {@code RequestContextHolder} in {@link #afterTestMethod(TestContext)}.
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 */
	public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "resetRequestContextHolder");

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates that
	 * {@code ServletTestExecutionListener} has already populated Spring Web's
	 * {@code RequestContextHolder}.
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 */
	public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "populatedRequestContextHolder");

	/**
	 * Attribute name for a request attribute which indicates that the
	 * {@link MockHttpServletRequest} stored in the {@link RequestAttributes}
	 * in Spring Web's {@link RequestContextHolder} was created by the TestContext
	 * framework.
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 * @since 4.2
	 */
	public static final String CREATED_BY_THE_TESTCONTEXT_FRAMEWORK = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "createdByTheTestContextFramework");

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates that the
	 * {@code ServletTestExecutionListener} should be activated. When not set to
	 * {@code true}, activation occurs when the {@linkplain TestContext#getTestClass()
	 * test class} is annotated with {@link WebAppConfiguration @WebAppConfiguration}.
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 * @since 4.3
	 */
	public static final String ACTIVATE_LISTENER = Conventions.getQualifiedAttributeName(
			ServletTestExecutionListener.class, "activateListener");


	private static final Log logger = LogFactory.getLog(ServletTestExecutionListener.class);


	/**
	 * Returns {@code 1000}.
	 */
	@Override
	public final int getOrder() {
		return 1000;
	}

	/**
	 * Sets up thread-local state during the <em>test instance preparation</em>
	 * callback phase via Spring Web's {@link RequestContextHolder}, but only if
	 * the {@linkplain TestContext#getTestClass() test class} is annotated with
	 * {@link WebAppConfiguration @WebAppConfiguration}.
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		/**
		 * 在测试实例准备回调阶段，通过Spring Web的RequestContextHolder设置线程本地状态，
		 * 但前提是测试类使用@WebAppConfiguration注释。
		 *
		 * 问题： 测试类的实例是什么时候创建的？
		 * 在测试框架 org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor#prepare(org.junit.jupiter.engine.execution.JupiterEngineExecutionContext)
		 * 方法中会通过 下面的方式创建 测试实例对象
		 * TestInstances testInstances = context.getTestInstancesProvider().getTestInstances(registry,
		 * 				throwableCollector);
		 *
		 * TestInstances instances = instantiateTestClass(parentExecutionContext, registry, registrar, extensionContext,
		 * 			throwableCollector);
		 *
		 * 	在创建实例之后会执行实例的 TestInstancePostProcessor
		 * 	invokeTestInstancePostProcessors(instances.getInnermostInstance(), registry, extensionContext);
		 *
		 * 其中有一个 特殊的TestInstancePostProcessor 就是SpringExtension
		 *
		 * SpringExtension的回调方法postProcessTestInstance  会执行 prepareTestInstance
		 * getTestContextManager(context).prepareTestInstance(testInstance);
		 *
		 * 在prepareTestInstance的过程中会执行 所有的 TestExecutionListener
		 *
		 *其中有两个重要的 TestExecutionListener
		 * （1）DependencyInjectionTestExecutionListener
		 * （2）这里的ServletTestExecutionListener
		 * 在listener中一般都会根据方法中接收到的 TestContext，主动获取TestContext中的Applicationcontext
		 * prepareTestInstance(TestContext testContext)
		 *
		 * ApplicationContext context = testContext.getApplicationContext();
		 *
		 * testContext中如果ApplicationContext还没有 创建，那么就会触发 loadContext 创建Spring容器
		 *
		 */
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * Sets up thread-local state before each test method via Spring Web's
	 * {@link RequestContextHolder}, but only if the
	 * {@linkplain TestContext#getTestClass() test class} is annotated with
	 * {@link WebAppConfiguration @WebAppConfiguration}.
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 * @see #setUpRequestContextIfNecessary(TestContext)
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		setUpRequestContextIfNecessary(testContext);
	}

	/**
	 * If the {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} in the supplied
	 * {@code TestContext} has a value of {@link Boolean#TRUE}, this method will
	 * (1) clean up thread-local state after each test method by {@linkplain
	 * RequestContextHolder#resetRequestAttributes() resetting} Spring Web's
	 * {@code RequestContextHolder} and (2) ensure that new mocks are injected
	 * into the test instance for subsequent tests by setting the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * in the test context to {@code true}.
	 * <p>The {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} and
	 * {@link #POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} will be subsequently
	 * removed from the test context, regardless of their values.
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Resetting RequestContextHolder for test context %s.", testContext));
			}
			RequestContextHolder.resetRequestAttributes();
			testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
				Boolean.TRUE);
		}
		testContext.removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		testContext.removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
	}

	private boolean isActivated(TestContext testContext) {
		return (Boolean.TRUE.equals(testContext.getAttribute(ACTIVATE_LISTENER)) ||
				AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), WebAppConfiguration.class));
	}

	private boolean alreadyPopulatedRequestContextHolder(TestContext testContext) {
		return Boolean.TRUE.equals(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE));
	}

	private void setUpRequestContextIfNecessary(TestContext testContext) {
		if (!isActivated(testContext) || alreadyPopulatedRequestContextHolder(testContext)) {
			return;
		}

		ApplicationContext context = testContext.getApplicationContext();

		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext servletContext = wac.getServletContext();
			Assert.state(servletContext instanceof MockServletContext, () -> String.format(
						"The WebApplicationContext for test context %s must be configured with a MockServletContext.",
						testContext));

			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Setting up MockHttpServletRequest, MockHttpServletResponse, ServletWebRequest, and RequestContextHolder for test context %s.",
						testContext));
			}

			MockServletContext mockServletContext = (MockServletContext) servletContext;
			MockHttpServletRequest request = new MockHttpServletRequest(mockServletContext);
			request.setAttribute(CREATED_BY_THE_TESTCONTEXT_FRAMEWORK, Boolean.TRUE);
			MockHttpServletResponse response = new MockHttpServletResponse();
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

			RequestContextHolder.setRequestAttributes(servletWebRequest);
			testContext.setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
			testContext.setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

			if (wac instanceof ConfigurableApplicationContext) {
				@SuppressWarnings("resource")
				ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) wac;
				ConfigurableListableBeanFactory bf = configurableApplicationContext.getBeanFactory();
				bf.registerResolvableDependency(MockHttpServletResponse.class, response);
				bf.registerResolvableDependency(ServletWebRequest.class, servletWebRequest);
			}
		}
	}

}
