# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a Gradle-based project. Use `./gradlew` (or `gradlew.bat` on Windows) for all build operations.

### Common Build Tasks

```bash
# Build the entire project (compile, test, check)
./gradlew build

# Run all tests
./gradlew test

# Run all checks including tests and RuntimeHints tests
./gradlew check

# Clean build artifacts
./gradlew clean

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

### Module-Specific Builds

```bash
# Build a specific module (e.g., spring-core)
./gradlew :spring-core:build

# Run tests for a specific module
./gradlew :spring-core:test

# Compile only a specific module
./gradlew :spring-core:compileJava
```

### Running Single Tests

```bash
# Run a specific test class
./gradlew :spring-core:test --tests "org.springframework.core.io.support.PathMatchingResourcePatternResolverTests"

# Run a specific test method
./gradlew :spring-core:test --tests "org.springframework.core.io.support.PathMatchingResourcePatternResolverTests.testMethodName"
```

### Special Test Types

```bash
# Run RuntimeHints tests (for GraalVM native image compatibility)
./gradlew runtimeHintsTest

# Run integration tests
./gradlew :integration-tests:test

# Run JMH micro-benchmarks
./gradlew jmh
```

### Documentation

```bash
# Generate reference documentation (Antora)
./gradlew antora

# View generated docs
open framework-docs/build/site/index.html
```

### IDE Setup

```bash
# Precompile spring-oxm before IDEA import (required due to repackaged dependencies)
./gradlew :spring-oxm:compileTestJava

# Generate IDE metadata
./gradlew idea
./gradlew eclipse
```

## Project Structure

The Spring Framework is organized into 24 subprojects:

**Core Container:**
- `spring-core` - Core utilities, resource handling, type conversion
- `spring-beans` - Bean factory and dependency injection
- `spring-context` - Application context, events, component scanning
- `spring-context-support` - Additional context support (caching, scheduling)
- `spring-expression` - Spring Expression Language (SpEL)
- `spring-aop` - Aspect-Oriented Programming support

**Data Access:**
- `spring-jdbc` - JDBC abstraction
- `spring-tx` - Transaction management
- `spring-orm` - JPA/Hibernate integration
- `spring-r2dbc` - Reactive database connectivity

**Web:**
- `spring-web` - Core web support (HTTP, client/server)
- `spring-webmvc` - Servlet-based MVC framework
- `spring-webflux` - Reactive web framework
- `spring-websocket` - WebSocket support
- `spring-messaging` - Message handling abstraction

**Testing:**
- `spring-test` - TestContext framework and testing support
- `spring-core-test` - Core testing utilities including RuntimeHints agent

**Infrastructure:**
- `framework-docs` - Reference documentation (AsciiDoc/Antora)
- `integration-tests` - Cross-module integration tests

## High-Level Architecture

### Dependency Injection Container

The core IoC container is implemented across `spring-core`, `spring-beans`, and `spring-context`:

- **BeanFactory** (`spring-beans`) - The root interface for bean container
- **ApplicationContext** (`spring-context`) - Adds event propagation, resource loading, internationalization
- **BeanDefinition** - Metadata for bean configuration (class, scope, properties, etc.)
- **BeanPostProcessor** - Extension point for custom bean initialization logic

Key source locations:
- Bean factories: `spring-beans/src/main/java/org/springframework/beans/factory/`
- Application context: `spring-context/src/main/java/org/springframework/context/support/`
- Annotation config: `spring-context/src/main/java/org/springframework/context/annotation/`

### Resource Abstraction

Spring provides a unified `Resource` interface for loading files, classpath resources, URLs:

- **Resource** - Abstraction over file/URL/classpath resources
- **ResourceLoader** - Strategy for loading resources
- **PathMatchingResourcePatternResolver** - Supports Ant-style patterns (e.g., `classpath*:com/example/**/*.xml`)

Key source: `spring-core/src/main/java/org/springframework/core/io/`

### Type Conversion System

- **Converter** SPI - Simple type conversion interface
- **ConversionService** - Registry and execution of converters
- **PropertyEditor** - JavaBeans property editor support (legacy)
- **ResolvableType** - Generic type resolution utilities

Key source: `spring-core/src/main/java/org/springframework/core/convert/`

### Annotation Processing

Spring has sophisticated annotation introspection supporting composed annotations and attribute overrides:

- **AnnotatedElementUtils** - High-level annotation retrieval with merging
- **AnnotationUtils** - Low-level annotation utilities
- **@AliasFor** - Annotation attribute aliasing mechanism
- **MergedAnnotations** - Immutable representation of merged annotation attributes

Key source: `spring-core/src/main/java/org/springframework/core/annotation/`

### AOP Infrastructure

- **ProxyFactory** - Creates AOP proxies (JDK dynamic or CGLIB)
- **Advisor** - Combines pointcut and advice
- **@AspectJ support** - Annotation-driven aspects
- **Load-time weaving** - AspectJ LTW integration

Key sources:
- Core AOP: `spring-aop/src/main/java/org/springframework/aop/`
- AspectJ integration: `spring-aspects/src/main/java/org/springframework/aspects/`

### Web Frameworks

**Spring MVC** (Servlet-based):
- **DispatcherServlet** - Front controller
- **@Controller/@RequestMapping** - Annotation-driven controllers
- **HandlerMapping/HandlerAdapter** - Request routing and execution

**Spring WebFlux** (Reactive):
- **DispatcherHandler** - Reactive front controller
- **RouterFunction** - Functional routing (alternative to annotations)
- **ReactiveAdapterRegistry** - Adapts reactive types (Reactor, RxJava)

Key sources:
- Spring MVC: `spring-webmvc/src/main/java/org/springframework/web/servlet/`
- WebFlux: `spring-webflux/src/main/java/org/springframework/web/reactive/`

### Ahead-of-Time (AOT) Compilation

For GraalVM native image support:

- **RuntimeHints** - Declares reflection, resources, proxies, serialization requirements
- **BeanFactoryInitializationAotProcessor** - Contributes AOT processing for bean factories
- **RuntimeHintsAgent** - Java agent for detecting runtime hints during tests

Key sources:
- AOT infrastructure: `spring-core/src/main/java/org/springframework/aot/`
- Context AOT: `spring-context/src/main/java/org/springframework/context/aot/`
- Test agent: `spring-core-test/src/main/java/org/springframework/core/test/tools/`

## Testing Approach

- **JUnit 5** (Jupiter) is the primary testing framework
- **Mockito** for mocking
- **AssertJ** for fluent assertions
- **@EnabledIfRuntimeHintsAgent** - Conditionally run tests requiring the RuntimeHints agent

Tests are typically organized alongside source code in `src/test/java`.

## Key Conventions

### Code Style

- Follow existing code formatting in the files you edit
- Use tabs for indentation (not spaces)
- Maximum line length: 120 characters
- Checkstyle configuration is in `buildSrc/config/`

### Commit Messages

- Use 55 characters max for subject line
- Use 72 characters max per line for description
- Reference GitHub issue: `Closes gh-12345`
- All commits must include a `Signed-off-by` trailer (DCO required)

Example:
```
Fix resource loading for jar:file: URLs

This commit fixes an issue where resources with jar:file: URLs
were not being resolved correctly on Windows.

Closes gh-12345

Signed-off-by: Your Name <email@example.com>
```

### Version Information

- Current version: `6.2.16-SNAPSHOT` (defined in `gradle.properties`)
- Baseline Java version: 17
- Multi-release JARs support Java 21 features

## Important Build Notes

1. **Precompile spring-oxm before IDEA import** - Due to repackaged dependencies (XSD schemas), you must run `./gradlew :spring-oxm:compileTestJava` before importing into IntelliJ IDEA.

2. **Exclude spring-aspects in IDEA** - The `spring-aspects` module contains AspectJ aspects that IntelliJ cannot compile. Exclude this module in Project Structure.

3. **Gradle Toolchains** - The build uses Gradle toolchains to automatically download and use appropriate JDK versions. List available toolchains with `./gradlew -q javaToolchains`.

4. **Optional Dependencies** - The build uses a custom `optional` configuration for dependencies that should be on the compile classpath but not propagated to dependents.

## Reference Documentation

- Reference docs source: `framework-docs/modules/ROOT/pages/`
- Wiki pages: https://github.com/spring-projects/spring-framework/wiki
- Build from Source wiki: https://github.com/spring-projects/spring-framework/wiki/Build-from-Source
- Code Style wiki: https://github.com/spring-projects/spring-framework/wiki/Code-Style
