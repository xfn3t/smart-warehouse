package ru.rtc.warehouse.common.aspect;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RequiresOwnership.List.class)
public @interface RequiresOwnership {
	String codeParam();
	EntityType entityType();

	enum EntityType {
		WAREHOUSE,
		ROBOT
	}

	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		RequiresOwnership[] value();
	}
}