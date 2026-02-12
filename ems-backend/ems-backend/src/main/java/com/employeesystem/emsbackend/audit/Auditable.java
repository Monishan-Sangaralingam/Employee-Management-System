package com.employeesystem.emsbackend.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();

    String entity();

    /**
     * Optional argument index to read the entityId from (e.g., deleteEmployeeById(Long id) => 0).
     * If -1, the aspect will try to extract id from the returned object via getId().
     */
    int entityIdArgIndex() default -1;

    /** Optional extra details to store as-is. */
    String details() default "";
}
