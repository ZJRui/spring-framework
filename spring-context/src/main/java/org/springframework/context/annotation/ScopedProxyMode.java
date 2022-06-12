/*
 * Copyright 2002-2020 the original author or authors.
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

/**
 * Enumerates the various scoped-proxy options.
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * section of the Spring reference documentation entitled '<em>Scoped beans as
 * dependencies</em>'.
 *
 * 枚举各种作用域代理选项。
 * 要更完整地讨论什么是限定作用域的代理，请参阅Spring参考文档中题为“限定作用域的bean作为依赖项”的部分。
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

	/**
	 * Default typically equals {@link #NO}, unless a different default
	 * has been configured at the component-scan instruction level.
	 *
	 *
	 * Default通常等于NO，除非在组件扫描指令级别配置了不同的默认值。
	 */
	DEFAULT,

	/**
	 * Do not create a scoped proxy.
	 * <p>This proxy-mode is not typically useful when used with a
	 * non-singleton scoped instance, which should favor the use of the
	 * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
	 * is to be used as a dependency.
	 *
	 *
	 * 不要创建作用域代理。
	 * 当与非单例作用域实例一起使用时，这种代理模式通常不太有用，
	 * 如果要将其用作依赖项，则应该倾向于使用INTERFACES或TARGET_CLASS代理模式。
	 */
	NO,

	/**
	 * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
	 * the class of the target object.
	 *创建一个JDK动态代理，实现由目标对象的类公开的所有接口。
	 * //interface
	 */
	INTERFACES,

	/**
	 * Create a class-based proxy (uses CGLIB).
	 * target_class :类代理的模式
	 */
	TARGET_CLASS

}
