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

package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that creates a JPA
 * {@link javax.persistence.EntityManagerFactory} according to JPA's standard
 * <i>standalone</i> bootstrap contract. This is the simplest way to set up a
 * shared JPA EntityManagerFactory in a Spring application context; the
 * EntityManagerFactory can then be passed to JPA-based DAOs via
 * dependency injection. Note that switching to a JNDI lookup or to a
 * {@link LocalContainerEntityManagerFactoryBean}
 * definition is just a matter of configuration!
 *
 * <p>Configuration settings are usually read from a {@code META-INF/persistence.xml}
 * config file, residing in the class path, according to the JPA standalone bootstrap
 * contract. Additionally, most JPA providers will require a special VM agent
 * (specified on JVM startup) that allows them to instrument application classes.
 * See the Java Persistence API specification and your provider documentation
 * for setup details.
 *
 * <p>This EntityManagerFactory bootstrap is appropriate for standalone applications
 * which solely use JPA for data access. If you want to set up your persistence
 * provider for an external DataSource and/or for global transactions which span
 * multiple resources, you will need to either deploy it into a full Java EE
 * application server and access the deployed EntityManagerFactory via JNDI,
 * or use Spring's {@link LocalContainerEntityManagerFactoryBean} with appropriate
 * configuration for local setup according to JPA's container contract.
 *
 * <p><b>Note:</b> This FactoryBean has limited configuration power in terms of
 * what configuration it is able to pass to the JPA provider. If you need more
 * flexible configuration, for example passing a Spring-managed JDBC DataSource
 * to the JPA provider, consider using Spring's more powerful
 * {@link LocalContainerEntityManagerFactoryBean} instead.
 *
 * <p><b>NOTE: Spring's JPA support requires JPA 2.1 or higher, as of Spring 5.0.</b>
 * JPA 1.0/2.0 based applications are still supported; however, a JPA 2.1 compliant
 * persistence provider is needed at runtime.
 *
 *
 * 根据JPA的标准独立引导契约创建JPA EntityManagerFactory。这是在Spring应用程序上下文中建立共享JPA EntityManagerFactory的最简单的方法;
 * 然后，EntityManagerFactory可以通过依赖注入传递给基于jpa的dao。注意，切换到JNDI查找或LocalContainerEntityManagerFactoryBean定义只是一个配置问题!
 * 根据JPA独立引导契约，配置设置通常从META-INF/persistence.xml配置文件中读取，该文件驻留在类路径中。
 * 此外，大多数JPA提供者将需要一个特殊的VM代理(在JVM启动时指定)，该代理允许它们检测应用程序类。
 * 有关设置细节，请参阅Java Persistence API规范和您的提供商文档。
 * 这个EntityManagerFactory引导程序适用于仅使用JPA进行数据访问的独立应用程序。如果您想为外部DataSource和/或跨多个资源的全局事务设置持久提供程序，
 * 您需要将其部署到完整的Java EE应用服务器，并通过JNDI访问部署的EntityManagerFactory。
 * 或者使用Spring的LocalContainerEntityManagerFactoryBean，并根据JPA的容器契约进行适当的本地设置配置。
 * 注意:就能够传递给JPA提供者的配置而言，这个FactoryBean的配置能力有限。如果您需要更灵活的配置，例如将一个Spring管理的JDBC数据源传递给JPA提供者，
 * 请考虑使用Spring更强大的LocalContainerEntityManagerFactoryBean。
 * 注意:从Spring 5.0开始，Spring的JPA支持需要JPA 2.1或更高。仍然支持基于JPA 1.0/2.0的应用程序;然而，在运行时需要一个兼容JPA 2.1的持久性提供者。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see JpaTransactionManager#setEntityManagerFactory
 * @see LocalContainerEntityManagerFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see javax.persistence.Persistence#createEntityManagerFactory
 * @see javax.persistence.spi.PersistenceProvider#createEntityManagerFactory
 *
 */
@SuppressWarnings("serial")
public class LocalEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

	/**
	 * Initialize the EntityManagerFactory for the given configuration.
	 * @throws javax.persistence.PersistenceException in case of JPA initialization errors
	 */
	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		if (logger.isDebugEnabled()) {
			logger.debug("Building JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		PersistenceProvider provider = getPersistenceProvider();
		if (provider != null) {
			// Create EntityManagerFactory directly through PersistenceProvider.
			EntityManagerFactory emf = provider.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
			if (emf == null) {
				throw new IllegalStateException(
						"PersistenceProvider [" + provider + "] did not return an EntityManagerFactory for name '" +
						getPersistenceUnitName() + "'");
			}
			return emf;
		}
		else {
			// Let JPA perform its standard PersistenceProvider autodetection.
			return Persistence.createEntityManagerFactory(getPersistenceUnitName(), getJpaPropertyMap());
		}
	}

}
