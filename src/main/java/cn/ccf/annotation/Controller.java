package cn.ccf.annotation;

import java.lang.annotation.*;

/**
 * @author charles
 * @date 2019/7/2 15:40
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String value() default "";
}
