package cn.ccf.annotation;

import java.lang.annotation.*;

/**
 * @author charles
 * @date 2019/7/2 15:44
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
