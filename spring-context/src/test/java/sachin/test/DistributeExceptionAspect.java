package sachin.test;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Aspect

public class DistributeExceptionAspect {


	private ExpressionEvaluator evaluator = new ExpressionEvaluator();

	@Pointcut("@annotation(DistributeExceptionHandler)")
	private void exceptionHandleMethod() {

	}

	@AfterThrowing(value = "exceptionHandlerMethod()", throwing = "ex")
	public void doThrowing(JoinPoint joinPoint, Throwable ex) {
		System.out.println("捕获异常");

	}

	private DistributeExceptionHandler getDistributeExceptionHandler(JoinPoint joinPoint){
		MethodSignature methodSignature=(MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		return method.getAnnotation(DistributeExceptionHandler.class);
	}

	private String getAttachmentId(JoinPoint joinPoint) {

		DistributeExceptionHandler distributeExceptionHandler = getDistributeExceptionHandler(joinPoint);
		if (joinPoint.getArgs() == null) {
			return null;
		}
		EvaluationContext evaluationContext = evaluator.createEvaluationContext(joinPoint.getTarget(), joinPoint.getTarget().getClass(),
				((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());


	}
}



