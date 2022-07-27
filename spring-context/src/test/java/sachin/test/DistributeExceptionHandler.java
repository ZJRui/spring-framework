package sachin.test;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface DistributeExceptionHandler {
	/**
	 * 自定义 DistributeExceptionHandler 注解，该注解接收一个参数 attachmentId 。
	 * 该注解用在方法上，使用该注解作为切点，实现标注该注解的方法抛异常后的统一处理。
	 * @return
	 */
	String attachmentId();

}
