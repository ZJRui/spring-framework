package sachin.test;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpressionEvaluator {

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
	private final Map conditionCache = new ConcurrentHashMap<>(64);
	private final Map targetMethodCache=new ConcurrentHashMap(64);

	public EvaluationContext createEvaluationContext(Object target, Class<?> targetClzss, Method method, Object[] args) {

		Method targetMethod = getTargetMethod(targetClzss, method);

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
