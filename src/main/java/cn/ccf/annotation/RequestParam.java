package cn.ccf.annotation;

import java.lang.annotation.*;

/**
 * @author charles
 * @date 2019/7/2 15:45
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value() default "";
}
