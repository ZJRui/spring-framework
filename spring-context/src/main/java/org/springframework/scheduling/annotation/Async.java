/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a candidate for <i>asynchronous</i> execution.
 * Can also be used at the type level, in which case all of the type's methods are
 * considered as asynchronous.
 *
 * <p>In terms of target method signatures, any parameter types are supported.
 * However, the return type is constrained to either {@code void} or
 * {@link java.util.concurrent.Future}. In the latter case, you may declare the
 * more specific {@link org.springframework.util.concurrent.ListenableFuture} or
 * {@link java.util.concurrent.CompletableFuture} types which allow for richer
 * interaction with the asynchronous task and for immediate composition with
 * further processing steps.
 *
 * <p>A {@code Future} handle returned from the proxy will be an actual asynchronous
 * {@code Future} that can be used to track the result of the asynchronous method
 * execution. However, since the target method needs to implement the same signature,
 * it will have to return a temporary {@code Future} handle that just passes a value
 * through: e.g. Spring's {@link AsyncResult}, EJB 3.1's {@link javax.ejb.AsyncResult},
 * or {@link java.util.concurrent.CompletableFuture#completedFuture(Object)}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see AnnotationAsyncExecutionInterceptor
 * @see AsyncAnnotationAdvisor
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Async {
	/**
	 *
	 * 将方法标记为异步执行候选方法的注释。也可以在类型级别使用，在这种情况下，类型的所有方法都被认为是异步的。
	 * 就目标方法签名而言，支持任何参数类型。但是，返回类型被限制为void或java.util.concurrent.Future。
	 * 在后一种情况下，你可以声明更具体的org.springframework.util.concurrent.ListenableFuture
	 * 或java.util.concurrent.CompletableFuture类型，
	 * 它们允许与异步任务进行更丰富的交互，并与进一步的处理步骤进行即时组合。
	 * 从代理返回的Future句柄将是一个实际的异步Future，可用于跟踪异步方法执行的结果。
	 * 然而，由于目标方法需要实现相同的签名，它将不得不返回一个临时的Future句柄
	 * ，只传递一个值:例如Spring的AsyncResult, EJB 3.1的javax.ejb。AsyncResult
	 * 或java.util.concurrent.CompletableFuture.completedFuture(对象)。
	 *
	 *
	 */
/**
 * Base class for BeanPostProcessor implementations that apply a
 * Spring AOP Advisor to specific beans.
 *
 * 对特定bean应用Spring AOP Advisor的BeanPostProcessor实现的基类。
 *
 *
 * 我们知道  判断一个Bean是否需要被增强是在Bean 实例化之后， 在执行org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#postProcessAfterInitialization(java.lang.Object, java.lang.String)
 * 的时候进行 wrapIfNecessary的判断。 这就要求我们首先需要开启AOP， 开启AOP之后会注入 aop的BeanPostProcessor，
 * 然后再bean 创建之后进行拦截 判断是否需要代理。
 *
 * 对于 @Async这个 标记方法异步执行的的实现是这样的：
 * （1）@EnableAsync 注解会通过 importSelector 导入一个配置ProxyAsyncConfiguration，
 * 这个配置类里面会注入一个AsyncAnnotationBeanPostProcessor， 这个beanPostprocessor的
 * org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor#setBeanFactory(org.springframework.beans.factory.BeanFactory)
 * 方法内部会创建Advisor 和pointcut,pointcut的判断条件就是判断 方法上是否存在@Async注解。
 * 然后在业务Bean 被创建后执行  AsyncAnnotationBeanPostProcessor的 postProcessAfterInitialization 的时候判断是否增强，生成代理
 * 我们要关注的是 AsyncAnnotationAdvisor中的advice ，这个advice就是 AnnotationAsyncExecutionInterceptor，他拦截到方法执行交给
 * 线程池执行
 *
 *
 *
 *
 */

	/**
	 * A qualifier value for the specified asynchronous operation(s).
	 * <p>May be used to determine the target executor to be used when executing this
	 * method, matching the qualifier value (or the bean name) of a specific
	 * {@link java.util.concurrent.Executor Executor} or
	 * {@link org.springframework.core.task.TaskExecutor TaskExecutor}
	 * bean definition.
	 * <p>When specified on a class level {@code @Async} annotation, indicates that the
	 * given executor should be used for all methods within the class. Method level use
	 * of {@code Async#value} always overrides any value set at the class level.
	 * @since 3.1.2
	 */
	String value() default "";

}
