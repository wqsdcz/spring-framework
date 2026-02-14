import java.lang.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.annotation.*;

/**
 * 任务1: 实现注解家谱打印机
 * 打印一个注解的完整继承层次
 */
public class AnnotationFamilyPrinter {

	/**
	 * 打印指定类上所有注解的"家谱"
	 */
	public static void printAnnotationFamily(Class<?> clazz) {
		System.out.println("\n========== 注解家谱: " + clazz.getSimpleName() + " ==========\n");

		MergedAnnotations annotations = MergedAnnotations.from(
			clazz, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY
		);

		annotations.forEach(merged -> {
			printAnnotationNode(merged, 0);
		});
	}

	/**
	 * 递归打印注解节点
	 */
	private static void printAnnotationNode(MergedAnnotation<?> merged, int indent) {
		String prefix = "  ".repeat(indent);

		// 基本信息
		System.out.println(prefix + "┌─ " + merged.getType().getSimpleName());
		System.out.println(prefix + "│  Distance: " + merged.getDistance());
		System.out.println(prefix + "│  Direct: " + merged.isDirectlyPresent());
		System.out.println(prefix + "│  Meta: " + merged.isMetaPresent());

		// 属性值
		if (merged.getDistance() == 0) {
			System.out.println(prefix + "│  属性:");
			merged.asMap().forEach((key, value) -> {
				String valueStr = value instanceof String[] ?
					java.util.Arrays.toString((String[]) value) : String.valueOf(value);
				System.out.println(prefix + "│    - " + key + " = " + valueStr);
			});
		}

		// 打印元注解层次
		MergedAnnotation<?> metaSource = merged.getMetaSource();
		if (metaSource != null) {
			System.out.println(prefix + "│  元注解来源:");
			printAnnotationNode(metaSource, indent + 1);
		}

		System.out.println(prefix + "└─");
	}

	// 测试注解
	@Retention(RetentionPolicy.RUNTIME)
	@interface GrandParent {
		String value() default "";
	}

	@GrandParent("parent")
	@Retention(RetentionPolicy.RUNTIME)
	@interface Parent {
		String name() default "";
	}

	@Parent(name = "child")
	@Retention(RetentionPolicy.RUNTIME)
	@interface Child {
		int count() default 0;
	}

	@Child(count = 5)
	static class TestSubject {}

	public static void main(String[] args) {
		printAnnotationFamily(TestSubject.class);
	}
}
