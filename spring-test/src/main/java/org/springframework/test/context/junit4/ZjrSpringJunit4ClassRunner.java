package org.springframework.test.context.junit4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ZjrSpringJunit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final Log logger = LogFactory.getLog(ZjrSpringJunit4ClassRunner.class);

	private static final Method withRulesMethod;

	static {
		Assert.state(ClassUtils.isPresent("org.junit.internal.Throwables", SpringJUnit4ClassRunner.class.getClassLoader()),
				"SpringJunit4ClassRunner requires Junit 4.13 or higher.");

		Method method = ReflectionUtils.findMethod(SpringJUnit4ClassRunner.class, "withRules",
				FrameworkMethod.class, Object.class, Statement.class);

		Assert.state(method != null, "SpringJunit4ClassRunner requires Junit 4.13 or higher");
		ReflectionUtils.makeAccessible(method);
		withRulesMethod = method;

	}

	private final TestContextManager testContextManager;


	/**
	 * Creates a BlockJUnit4ClassRunner to run {@code klass}
	 *
	 * @param klass
	 * @throws InitializationError if the test class is malformed.
	 */
	public ZjrSpringJunit4ClassRunner(Class<?> klass) throws InitializationError {
		super(klass);
		//
		ensuerSpringRulesAreNotPresent(klass);
		this.testContextManager = createTestContextManager(klass);

	}


	//构造器的时候被调用
	private static void ensuerSpringRulesAreNotPresent(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			//state的第一个参数为false的时候 就会跑出异常
			// 也就是SpringClassRule.isAssignableFrom返回true， 表示 field的类型是 SpringClassRule或其子类
			Assert.state(!SpringClassRule.class.isAssignableFrom(field.getType()), () -> String.format(

					"Detected SpringClassRule field in test class [%s]," +
							"but springclassrule cannot be used with springjunit4runner .",
					testClass.getName()
			));

			Assert.state(!SpringMethodRule.class.isAssignableFrom(field.getType()),
					() -> String.format("" +
									"Detected SpringMethodRule field in test class [%s]" +
									"but springmethodrule can not be used with springjunit4classrunner",
							testClass.getName())
			);

		}
	}

	protected  TestContextManager createTestContextManager(Class<?> clazz){
		return new TestContextManager(clazz);
	}
	public TestContextManager getTestContextManager() {

		return testContextManager;
	}

	@Override
	public Description getDescription() {
		/**
		 *
		 */
		if(!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())){
			return Description.createSuiteDescription(getTestClass().getJavaClass());
		}
		//构建测试树
		return super.getDescription();
	}


	@Override
	protected Statement withBeforeClasses(Statement statement) {
		Statement junitBeforeClasses = super.withBeforeClasses(statement);
		return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
	}
}
