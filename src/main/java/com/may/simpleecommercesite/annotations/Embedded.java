package com.may.simpleecommercesite.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.TYPE})
public @interface Embedded {
}
