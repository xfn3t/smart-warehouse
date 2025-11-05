package ru.rtc.warehouse.common.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.auth.UserDetailsImpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class OwnershipAspect {

	private final OwnershipGuard ownershipGuard;

	@Before("@within(requiresOwnership) || @annotation(requiresOwnership)")
	public void checkAccess(JoinPoint jp, RequiresOwnership requiresOwnership) {

		// Собираем все аннотации RequiresOwnership (включая повторяющиеся)
		List<AccessCheck> checks = collectAccessChecks(jp, requiresOwnership);

		for (AccessCheck check : checks) {
			String entityCode = extractEntityCode(jp, check.paramName);
			if (entityCode == null) {
				continue;
			}

			Long userId = getCurrentUserId();

			ownershipGuard.assertOwnership(check.entityType, entityCode, userId);
		}
	}

	private List<AccessCheck> collectAccessChecks(JoinPoint jp, RequiresOwnership requiresOwnership) {
		List<AccessCheck> checks = new ArrayList<>();

		MethodSignature signature = (MethodSignature) jp.getSignature();
		Method method = signature.getMethod();
		Class<?> targetClass = jp.getTarget().getClass();

		// Если аннотация передана через параметр
		if (requiresOwnership != null) {
			checks.add(new AccessCheck(requiresOwnership.codeParam(), requiresOwnership.entityType()));
		}

		// Ищем аннотации на методе
		RequiresOwnership methodAnn = method.getAnnotation(RequiresOwnership.class);
		if (methodAnn != null) {
			checks.add(new AccessCheck(methodAnn.codeParam(), methodAnn.entityType()));
		}

		// Ищем аннотации на классе
		RequiresOwnership classAnn = targetClass.getAnnotation(RequiresOwnership.class);
		if (classAnn != null) {
			checks.add(new AccessCheck(classAnn.codeParam(), classAnn.entityType()));
		}

		// Обрабатываем повторяющиеся аннотации на методе
		RequiresOwnership.List methodList = method.getAnnotation(RequiresOwnership.List.class);
		if (methodList != null) {
			for (RequiresOwnership ann : methodList.value()) {
				checks.add(new AccessCheck(ann.codeParam(), ann.entityType()));
			}
		}

		// Обрабатываем повторяющиеся аннотации на классе
		RequiresOwnership.List classList = targetClass.getAnnotation(RequiresOwnership.List.class);
		if (classList != null) {
			for (RequiresOwnership ann : classList.value()) {
				checks.add(new AccessCheck(ann.codeParam(), ann.entityType()));
			}
		}

		return checks;
	}

	private String extractEntityCode(JoinPoint jp, String paramName) {
		MethodSignature signature = (MethodSignature) jp.getSignature();
		String[] paramNames = signature.getParameterNames();
		Object[] args = jp.getArgs();

		if (paramNames == null) {
			// Пытаемся найти по типу String
			for (Object arg : args) {
				if (arg instanceof String) {
					return (String) arg;
				}
			}
			return null;
		}

		// Ищем по точному имени параметра
		for (int i = 0; i < paramNames.length; i++) {
			if (paramNames[i].equals(paramName) && args[i] instanceof String code) {
				return code;
			}
		}

		// Если не нашли по точному имени, ищем по частичному совпадению
		for (int i = 0; i < paramNames.length; i++) {
			if (args[i] instanceof String code && paramNames[i].toLowerCase().contains(paramName.toLowerCase())) {
				return code;
			}
		}

		return null;
	}

	private Long getCurrentUserId() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetailsImpl userDetails) {
			return userDetails.getUser().getId();
		}
		throw new AccessDeniedException("User not authenticated");
	}

	private record AccessCheck(String paramName, RequiresOwnership.EntityType entityType) {}
}