package com.yusuf.simpleecommercesite.entities.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@javax.persistence.Entity
@Target({ElementType.TYPE})
public @interface Entity {
}
