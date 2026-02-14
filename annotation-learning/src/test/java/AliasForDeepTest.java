import java.lang.annotation.*;
import java.util.Map;

import org.springframework.core.annotation.*;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * 练习3: 深入理解 @AliasFor 的三种用法
 */
public class AliasForDeepTest {

	// ========== 场景1: 注解内的显式别名 ==========
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface ContextConfiguration {
		@AliasFor("locations")
		String[] value() default {};

		@AliasFor("value")
		String[] locations() default {};
	}

	// ========== 场景2: 元注解属性覆盖 ==========
	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface XmlTestConfig {
		@AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
		String[] xmlFiles();
	}

	// ========== 场景3: 隐式别名 ==========
	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MyTestConfig {
		@AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
		String[] value() default {};

		@AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
		String[] groovyScripts() default {};

		@AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
		String[] xmlFiles() default {};
	}

	// 使用示例
	@ContextConfiguration("/app.xml")
	static class Test1 {}

	@ContextConfiguration(locations = "/app.xml")
	static class Test2 {}

	@XmlTestConfig(xmlFiles = "/test.xml")
	static class Test3 {}

	@MyTestConfig(value = "/a.xml", groovyScripts = "/b.xml", xmlFiles = "/c.xml")
	static class Test4 {}

	@MyTestConfig(value = "/only-value.xml")
	static class Test5 {}

	public static void main(String[] args) {
		System.out.println("=== 场景1: 使用 value ===");
		printMergedAnnotation(Test1.class, ContextConfiguration.class);

		System.out.println("\n=== 场景1: 使用 locations ===");
		printMergedAnnotation(Test2.class, ContextConfiguration.class);

		System.out.println("\n=== 场景2: 元注解覆盖 ===");
		printMergedAnnotation(Test3.class, ContextConfiguration.class);

		System.out.println("\n=== 场景3: 隐式别名（全部设置不同值，会报错）===");
		try {
			printMergedAnnotation(Test4.class, ContextConfiguration.class);
		} catch (Exception e) {
			System.out.println("预期错误: " + e.getMessage());
		}

		System.out.println("\n=== 场景3: 隐式别名（只设置一个，其他自动同步）===");
		printMergedAnnotation(Test5.class, ContextConfiguration.class);

		// 打印合并后的注解属性 Map
		System.out.println("\n=== 查看完整的合并后属性 ===");
		MergedAnnotation<ContextConfiguration> merged = MergedAnnotations
			.from(Test5.class)
			.get(ContextConfiguration.class);

		Map<String, Object> map = merged.asMap();
		map.forEach((k, v) -> {
			if (v instanceof String[]) {
				System.out.println(k + " = " + java.util.Arrays.toString((String[])v));
			} else {
				System.out.println(k + " = " + v);
			}
		});
	}

	static <A extends Annotation> void printMergedAnnotation(Class<?> clazz, Class<A> annoType) {
		MergedAnnotation<A> merged = MergedAnnotations.from(clazz).get(annoType);
		System.out.println("找到注解: " + merged.isPresent());
		if (merged.isPresent()) {
			System.out.println("类型: " + merged.getType().getSimpleName());
			System.out.println("距离: " + merged.getDistance());
		}
	}
}
