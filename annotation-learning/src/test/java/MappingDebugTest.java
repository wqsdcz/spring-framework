import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.annotation.*;

/**
 * 阶段3: 调试理解 AnnotationTypeMapping
 */
public class MappingDebugTest {

	// 定义带各种特性的注解
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Meta {
		String value() default "";
		int count() default 0;
		String extra() default "meta-default";
	}

	@Meta
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Composed {
		@AliasFor(annotation = Meta.class, attribute = "value")
		String name() default "";

		@AliasFor(annotation = Meta.class, attribute = "count")
		int num() default 0;

		// 这个没有 @AliasFor，会使用约定映射（同名）
		String extra() default "composed-default";
	}

	@Composed(name = "test", num = 5)
	static class TestClass {}

	public static void main(String[] args) throws Exception {
		// 使用反射获取 AnnotationTypeMapping 内部信息
		// 注意：这是内部类，实际生产中不要这样用，仅用于学习

		System.out.println("=== 步骤1: 获取 MergedAnnotation ===");
		MergedAnnotation<Meta> merged = MergedAnnotations.from(TestClass.class).get(Meta.class);

		System.out.println("注解类型: " + merged.getType().getName());
		System.out.println("距离: " + merged.getDistance());
		System.out.println("是否直接存在: " + merged.isDirectlyPresent());
		System.out.println("是否元存在: " + merged.isMetaPresent());

		System.out.println("\n=== 步骤2: 读取属性值 ===");
		System.out.println("value (来自name): " + merged.getString("value"));
		System.out.println("count (来自num): " + merged.getInt("count"));
		System.out.println("extra (约定映射): " + merged.getString("extra"));

		System.out.println("\n=== 步骤3: 查看默认值 ===");
		System.out.println("value 默认值: " + merged.getDefaultValue("value").orElse("null"));
		System.out.println("count 默认值: " + merged.getDefaultValue("count").orElse("null"));

		// 使用合成后的注解
		System.out.println("\n=== 步骤4: 合成注解 ===");
		Meta meta = merged.synthesize();
		System.out.println("合成注解 value(): " + meta.value());
		System.out.println("合成注解 count(): " + meta.count());
		System.out.println("合成注解 extra(): " + meta.extra());

		// 查看原始注解
		System.out.println("\n=== 步骤5: 查看根注解 ===");
		MergedAnnotation<?> root = merged.getRoot();
		System.out.println("根注解类型: " + root.getType().getSimpleName());
		if (root.isPresent()) {
			// 获取 Composed 的 name 属性
			String name = root.getString("name");
			System.out.println("根注解 name: " + name);
		}
	}
}
