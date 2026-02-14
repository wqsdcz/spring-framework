import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.core.annotation.*;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * 阶段1: 学习使用 MergedAnnotations API
 */
public class AnnotationLearningTest {

	// 定义测试注解
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE, ElementType.METHOD})
	@interface MetaAnnotation {
		String value() default "";
		int count() default 0;
	}

	// 组合注解（使用 @AliasFor）
	@MetaAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MyComposedAnnotation {
		@AliasFor(annotation = MetaAnnotation.class, attribute = "value")
		String name() default "";

		@AliasFor(annotation = MetaAnnotation.class, attribute = "count")
		int num() default 0;
	}

	// 测试类
	@MyComposedAnnotation(name = "test", num = 5)
	static class TestClass {
	}

	public static void main(String[] args) {
		// 1. 基础使用 - 从类获取合并注解
		MergedAnnotations annotations = MergedAnnotations.from(TestClass.class);

		// 2. 检查注解是否存在
		boolean hasMeta = annotations.isPresent(MetaAnnotation.class);
		System.out.println("是否有 @MetaAnnotation: " + hasMeta);

		// 3. 获取合并后的注解
		MergedAnnotation<MetaAnnotation> merged = annotations.get(MetaAnnotation.class);

		// 4. 读取属性（注意：属性值已经被合并！）
		String value = merged.getString("value");
		int count = merged.getInt("count");

		System.out.println("value = " + value);  // "test"
		System.out.println("count = " + count);  // 5

		// 5. 查看注解来源信息
		System.out.println("distance = " + merged.getDistance());  // 1（因为是元注解）
		System.out.println("isDirectlyPresent = " + merged.isDirectlyPresent());  // false
		System.out.println("isMetaPresent = " + merged.isMetaPresent());  // true

		// 6. 获取根注解
		MergedAnnotation<?> root = merged.getRoot();
		System.out.println("root type = " + root.getType().getSimpleName());  // MyComposedAnnotation

		// 7. 合成回真实注解
		MetaAnnotation synthesize = merged.synthesize();
		System.out.println("synthesize.value() = " + synthesize.value());
	}
}
