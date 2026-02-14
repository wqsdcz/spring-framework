import java.lang.annotation.*;
import java.util.Arrays;

import org.springframework.core.annotation.*;

/**
 * 任务3: 对比 Java 原生反射和 Spring 合并注解
 * 理解为什么需要 MergedAnnotations
 */
public class CompareWithJavaReflection {

	// 元注解
	@Retention(RetentionPolicy.RUNTIME)
	@interface Meta {
		String value() default "";
	}

	// 组合注解
	@Meta("from-composed")
	@Retention(RetentionPolicy.RUNTIME)
	@interface Composed {
		@AliasFor(annotation = Meta.class, attribute = "value")
		String name() default "";
	}

	@Composed(name = "actual-value")
	static class TestClass {}

	public static void main(String[] args) {
		System.out.println("========== Java 原生反射 ==========\n");

		// Java 原生方式
		Meta meta = TestClass.class.getAnnotation(Meta.class);
		System.out.println("getAnnotation(Meta.class): " + meta);
		// 输出 null！因为 @Meta 是元注解，不是直接声明的

		Composed composed = TestClass.class.getAnnotation(Composed.class);
		System.out.println("getAnnotation(Composed.class): " + composed);
		if (composed != null) {
			System.out.println("  composed.name() = " + composed.name());
			// 注意：无法通过 composed 获取 @Meta 的属性值！
		}

		// 获取元注解
		Meta metaOnComposed = Composed.class.getAnnotation(Meta.class);
		System.out.println("Composed 上的 @Meta: " + metaOnComposed);
		if (metaOnComposed != null) {
			System.out.println("  但这是声明时的默认值，不是实际值");
			System.out.println("  metaOnComposed.value() = " + metaOnComposed.value());
		}

		System.out.println("\n========== Spring MergedAnnotations ==========\n");

		// Spring 方式
		MergedAnnotations annotations = MergedAnnotations.from(TestClass.class);

		// 可以获取到元注解！
		MergedAnnotation<Meta> mergedMeta = annotations.get(Meta.class);
		System.out.println("MergedAnnotations.get(Meta.class).isPresent(): " + mergedMeta.isPresent());
		System.out.println("  value = " + mergedMeta.getString("value"));
		// 输出 "actual-value"！

		// 可以看到属性来源
		System.out.println("  distance = " + mergedMeta.getDistance());
		System.out.println("  isMetaPresent = " + mergedMeta.isMetaPresent());

		// 获取根注解
		MergedAnnotation<?> root = mergedMeta.getRoot();
		System.out.println("\n  根注解: " + root.getType().getSimpleName());
		System.out.println("  根注解 name = " + root.getString("name"));

		System.out.println("\n========== 对比总结 ==========\n");
		System.out.println("Java 原生反射:");
		System.out.println("  ✗ 无法获取元注解（除非直接声明）");
		System.out.println("  ✗ 无法看到属性覆盖效果");
		System.out.println("  ✗ 无法处理 @AliasFor");

		System.out.println("\nSpring MergedAnnotations:");
		System.out.println("  ✓ 可以获取元注解");
		System.out.println("  ✓ 属性值已经合并");
		System.out.println("  ✓ 完全支持 @AliasFor");
		System.out.println("  ✓ 可以追踪属性来源");
	}
}
