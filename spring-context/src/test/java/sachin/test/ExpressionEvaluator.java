package sachin.test;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * https://www.jianshu.com/p/f2c846aeda5e
 */
public class ExpressionEvaluator {

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
	private final Map conditionCache = new ConcurrentHashMap<>(64);
	private final Map targetMethodCache=new ConcurrentHashMap(64);

	public EvaluationContext createEvaluationContext(Object target, Class<?> targetClzss, Method method, Object[] args) {

		//https://www.jianshu.com/p/f2c846aeda5e
		Method targetMethod = getTargetMethod(targetClzss, method);

		return  null;

	}

	private Method getTargetMethod(Class targetClass,Method method){

		AnnotatedElementKey methodKey= new AnnotatedElementKey(method,targetClass);

		Method targetMethod=(Method) this.targetMethodCache.get(methodKey);
		if (targetMethod == null) {
			targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			if (targetMethod == null) {
				targetMethod = method;
			}
			this.targetMethodCache.put(methodKey, targetMethod);
		}

		return targetMethod;
	}
}
