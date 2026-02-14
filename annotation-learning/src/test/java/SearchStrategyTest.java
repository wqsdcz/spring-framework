import java.lang.annotation.*;

import org.springframework.core.annotation.*;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * 练习2: 理解不同的搜索策略
 */
public class SearchStrategyTest {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MyAnnotation {
		String value();
		String source() default "unknown";
	}

	// 父类
	@MyAnnotation(value = "parent", source = "ParentClass")
	static class ParentClass {}

	// 接口
	@MyAnnotation(value = "interface", source = "MyInterface")
	interface MyInterface {}

	// 子类（没有直接声明注解）
	static class ChildClass extends ParentClass implements MyInterface {}

	// 子类（覆盖声明）
	@MyAnnotation(value = "child", source = "ChildWithAnnotation")
	static class ChildWithAnnotation extends ParentClass {}

	public static void main(String[] args) {
		System.out.println("=== DIRECT 策略（仅直接声明）===");
		testStrategy(ChildClass.class, SearchStrategy.DIRECT);

		System.out.println("\n=== INHERITED_ANNOTATIONS 策略（直接 + @Inherited继承）===");
		testStrategy(ChildClass.class, SearchStrategy.INHERITED_ANNOTATIONS);

		System.out.println("\n=== SUPERCLASS 策略（直接 + 所有父类）===");
		testStrategy(ChildClass.class, SearchStrategy.SUPERCLASS);

		System.out.println("\n=== TYPE_HIERARCHY 策略（完整类型层次）===");
		testStrategy(ChildClass.class, SearchStrategy.TYPE_HIERARCHY);

		System.out.println("\n=== 子类覆盖父类注解的情况 ===");
		testStrategy(ChildWithAnnotation.class, SearchStrategy.TYPE_HIERARCHY);
	}

	static void testStrategy(Class<?> clazz, SearchStrategy strategy) {
		MergedAnnotations annotations = MergedAnnotations.from(clazz, strategy);

		// 统计找到的注解数量
		long count = annotations.stream(MyAnnotation.class).count();
		System.out.println("找到 " + count + " 个 @MyAnnotation");

		// 打印每个注解的详细信息
		annotations.stream(MyAnnotation.class).forEach(merged -> {
			System.out.println("  value=" + merged.getString("value") +
				", source=" + merged.getString("source") +
				", distance=" + merged.getDistance() +
				", aggregateIndex=" + merged.getAggregateIndex() +
				", isDirectlyPresent=" + merged.isDirectlyPresent());
		});
	}
}
