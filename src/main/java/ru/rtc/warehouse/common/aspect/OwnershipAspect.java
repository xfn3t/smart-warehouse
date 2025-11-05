package ru.rtc.warehouse.common.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.auth.UserDetailsImpl;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

	private final OwnershipGuard ownershipGuard;

	@Before("@annotation(requiresAccess) || @within(requiresAccess)")
	public void checkAccess(JoinPoint jp, RequiresWarehouseAccess requiresAccess) {
		// если аннотация стоит только на классе, а не на методе,
		// нужно достать ее оттуда вручную
		if (requiresAccess == null) {
			requiresAccess = jp.getTarget().getClass().getAnnotation(RequiresWarehouseAccess.class);
		}

		String warehouseCode = getWarehouseCodeFromArgs(jp, requiresAccess.codeParam());
		Long userId = getCurrentUserId();

		ownershipGuard.assertWarehouseOwnership(warehouseCode, userId);
	}

	private String getWarehouseCodeFromArgs(JoinPoint jp, String paramName) {
		MethodSignature signature = (MethodSignature) jp.getSignature();
		String[] paramNames = signature.getParameterNames();
		Object[] args = jp.getArgs();

		for (int i = 0; i < paramNames.length; i++) {
			if (paramNames[i].equals(paramName)) {
				return (String) args[i];
			}
		}
		throw new IllegalArgumentException("Parameter not found: " + paramName);
	}

	private Long getCurrentUserId() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetailsImpl userDetails) {
			return userDetails.getUser().getId();
		}
		throw new AccessDeniedException("User not authenticated");
	}
}
