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

package org.springframework.aop.scope;

import java.lang.reflect.Modifier;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Convenient proxy factory bean for scoped objects.
 *
 * <p>Proxies created using this factory bean are thread-safe singletons
 * and may be injected into shared objects, with transparent scoping behavior.
 *
 * <p>Proxies returned by this class implement the {@link ScopedObject} interface.
 * This presently allows for removing the corresponding object from the scope,
 * seamlessly creating a new instance in the scope on next access.
 *
 * <p>Please note that the proxies created by this factory are
 * <i>class-based</i> proxies by default. This can be customized
 * through switching the "proxyTargetClass" property to "false".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setProxyTargetClass
 */
@SuppressWarnings("serial")
public class ScopedProxyFactoryBean extends ProxyConfig
		implements FactoryBean<Object>, BeanFactoryAware, AopInfrastructureBean {

	/**
	 *
	 * 参考： org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy(
	 * org.springframework.beans.factory.config.BeanDefinitionHolder, org.springframework.beans.factory.support.BeanDefinitionRegistry, boolean)
	 *
	 *
	 *
	 */
	/** The TargetSource that manages scoping. */
	private final SimpleBeanTargetSource scopedTargetSource = new SimpleBeanTargetSource();

	/** The name of the target bean. */
	@Nullable
	private String targetBeanName;

	/** The cached singleton proxy. */
	@Nullable
	private Object proxy;


	/**
	 * Create a new ScopedProxyFactoryBean instance.
	 */
	public ScopedProxyFactoryBean() {
		setProxyTargetClass(true);
	}


	/**
	 * Set the name of the bean that is to be scoped.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;/**/
		this.scopedTargetSource.setTargetBeanName(targetBeanName);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {


		/**
		 * 该方法实现的内容含义参考
		 * org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy
		 *
		 */

		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
		}
		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;

		this.scopedTargetSource.setBeanFactory(beanFactory);

		ProxyFactory pf = new ProxyFactory();
		pf.copyFrom(this);
		pf.setTargetSource(this.scopedTargetSource);

		Assert.notNull(this.targetBeanName, "Property 'targetBeanName' is required");

		/**
		 * 这个 targetBeanName 值是如何来的参考
		 *  org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy
		 */
		Class<?> beanType = beanFactory.getType(this.targetBeanName);
		if (beanType == null) {
			throw new IllegalStateException("Cannot create scoped proxy for bean '" + this.targetBeanName +
					"': Target type could not be determined at the time of proxy creation.");
		}
		if (!isProxyTargetClass() || beanType.isInterface() || Modifier.isPrivate(beanType.getModifiers())) {
			pf.setInterfaces(ClassUtils.getAllInterfacesForClass(beanType, cbf.getBeanClassLoader()));
		}

		/**
		 * 添加一个简介，只实现ScopedObject上的方法。
		 *
		 * 下面这两端代码是什么意思呢？
		 *
		 * 首先 要了解org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy
		 *
		 * this.scopedTargetSource.getTargetBeanName()的 返回值是 scopedTargete.originalBeanName
		 *
		 * ScopedObject 对象能够根据这个 scopedTargete.originalBeanName  从ioc容器中获取到原始的bean对象。
		 *
		 *
		 * DelegatingIntroductionInterceptor 对象创建的时候 会记录入参的接口类型到成员属性 publishedInterfaces
		 *
		 * DelegatingIntroductionInterceptor 作为advice 创建代理对象
		 *
		 * 代理代理的接口是beanFactory.getType(this.targetBeanName); 的接口。
		 *
		 *
		 * DelegatingIntroductionInterceptor 这拦截的实现逻辑 就是 判断当前调用的方法 是不是 scopedObject 对象的接口的方法。如果是则拦截
		 *
		 * 如果当前代理对象执行的方法是 ScopedObject 接口中的方法，则DelegatingIntroductionInterceptor 会进行拦截，转移到在 参数scopedObject 对象上执行 目标方法。
		 *
		 *
		 * 那么问题是： 代理对象 实现的代理接口应该是 Class<?> beanType = beanFactory.getType(this.targetBeanName); beanType的接口， 所以理论上
		 * 代理对应应该不会实现ScopedObject 接口吧。
		 *
		 *
		 *
		 *
		 *  我对 ScopedProxyFactoryBean 的理解是这样的：
		 *  首先Spring支持scope 的概念，假设我的Controller中注入一个 request范围的service
		 *  那么每个请求都需要创建一个Service，但是问题在于Controller中必须要持有一个固定的对象，只不过在实际执行Service的方法的时候 会在不同的service对象上执行。
		 *  我总不能说 每次调用controller的方法之前 先创建一个Service对象，然后再通过反射或者其他技术将新的service设置给controller吧？这种方式存在缺陷，因为controller
		 *  对象就一个，多个请求线程都创建Service设置到controller中，还是无法满足每个请求使用独立的service。
		 *
		 *  因此Controller中的servie引用是固定的，在实际执行service的方法的时候会通过getBean的方式从容器中取出bean，这个取bean的过程 ioc容器
		 *  会根据bean的scoped来判断是否创建新的bean。 那么也就是说 controller中注入的是一个代理对象。
		 *  也就是这里的创建的代理对象。
		 *
		 *  对于代理对象，我们关注的是他为什么代理，代理之后 拦截到方法调用之后会做什么？也就是增强逻辑。
		 *
		 *
		 *
		 */
		// Add an introduction that implements only the methods on ScopedObject.   // 添加一个只实现ScopedObject这个类的方法的增强
		ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName());
		pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject));

		// Add the AopInfrastructureBean marker to indicate that the scoped proxy
		// itself is not subject to auto-proxying! Only its target bean is.
		pf.addInterface(AopInfrastructureBean.class);

		this.proxy = pf.getProxy(cbf.getBeanClassLoader());
	}


	@Override
	public Object getObject() {
		/**
		 * 该方法实现的内容含义参考
		 * org.springframework.aop.scope.ScopedProxyUtils#createScopedProxy
		 */
		if (this.proxy == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		if (this.proxy != null) {
			return this.proxy.getClass();
		}
		return this.scopedTargetSource.getTargetClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
