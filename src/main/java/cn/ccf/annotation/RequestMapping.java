package cn.ccf.annotation;

import java.lang.annotation.*;

/**
 * @author charles
 * @date 2019/7/2 15:41
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
