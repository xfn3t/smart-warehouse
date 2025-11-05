package ru.rtc.warehouse.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * Эта аннотация нужна для проверки прав доступа пользователя к складу
 * если пользователь попытается получить данные другого склада,
 * которые ему не принадлежат то будет исключение AccessDeniedException
**/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresWarehouseAccess {
	String codeParam();
}
