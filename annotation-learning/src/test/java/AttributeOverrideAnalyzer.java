import java.lang.annotation.*;
import java.util.*;

import org.springframework.core.annotation.*;

/**
 * 任务2: 实现属性覆盖分析器
 * 分析一个组合注解是如何覆盖元注解属性的
 */
public class AttributeOverrideAnalyzer {

	/**
	 * 分析注解覆盖关系
	 */
	public static void analyzeOverride(Class<? extends Annotation> composedAnnotationType) {
		System.out.println("\n========== 分析 @" + composedAnnotationType.getSimpleName() + " ==========\n");

		// 获取注解上的所有元注解
		Retention retention = composedAnnotationType.getAnnotation(Retention.class);
		Target target = composedAnnotationType.getAnnotation(Target.class);

		System.out.println("元注解:");
		for (Annotation anno : composedAnnotationType.getAnnotations()) {
			if (anno.annotationType() != Retention.class &&
				anno.annotationType() != Target.class &&
				anno.annotationType() != Documented.class &&
				anno.annotationType() != Inherited.class) {
				System.out.println("  - @" + anno.annotationType().getSimpleName());
				analyzeOverrideRelationship(composedAnnotationType, anno.annotationType());
			}
		}
	}

	/**
	 * 分析两个注解之间的覆盖关系
	 */
	private static void analyzeOverrideRelationship(
			Class<? extends Annotation> composedType,
			Class<? extends Annotation> metaType) {

		System.out.println("    属性覆盖分析:");

		// 获取组合注解的所有方法
		for (java.lang.reflect.Method method : composedType.getDeclaredMethods()) {
			AliasFor aliasFor = method.getAnnotation(AliasFor.class);

			if (aliasFor != null) {
				Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
				if (targetAnnotation == Annotation.class) {
					targetAnnotation = composedType;
				}

				String targetAttribute = aliasFor.attribute();
				if (targetAttribute.isEmpty()) {
					targetAttribute = aliasFor.value();
				}
				if (targetAttribute.isEmpty()) {
					targetAttribute = method.getName();
				}

				if (targetAnnotation == metaType) {
					System.out.println("      ✓ @" + composedType.getSimpleName() + "." + method.getName() +
						"() → @" + metaType.getSimpleName() + "." + targetAttribute + "()");
				}
			} else {
				// 检查是否有同名属性（约定映射）
				try {
					java.lang.reflect.Method metaMethod = metaType.getMethod(method.getName());
					if (metaMethod.getReturnType().equals(method.getReturnType())) {
						System.out.println("      ~ @" + composedType.getSimpleName() + "." + method.getName() +
							"() → @" + metaType.getSimpleName() + "." + method.getName() +
							"() (约定映射，已废弃)");
					}
				} catch (NoSuchMethodException e) {
					// 没有同名属性
				}
			}
		}
	}

	// Spring 风格的组合注解示例
	@RequestMapping
	@interface PostMapping {
		@AliasFor(annotation = RequestMapping.class, attribute = "value")
		String[] value() default {};

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] path() default {};

		@AliasFor(annotation = RequestMapping.class, attribute = "method")
		RequestMethod[] method() default {RequestMethod.POST};
	}

	// 模拟 Spring 的注解
	@interface RequestMapping {
		String[] value() default {};
		String[] path() default {};
		RequestMethod[] method() default {};
	}

	enum RequestMethod {
		GET, POST, PUT, DELETE
	}

	public static void main(String[] args) {
		analyzeOverride(PostMapping.class);
	}
}
