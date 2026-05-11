---
description: Harness Engineering 도구 설정 — build-recipe 라벨 상세, 커버리지 게이트, 코드 품질 도구
alwaysApply: true
---

# Harness Engineering Rules

빌드 도구 및 코드 품질 설정 규칙 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. build-recipe-plugin 라벨 상세

linecorp build-recipe-plugin 라벨 → `configureByLabel()` 자동 적용.

### `java` 라벨 포함 도구

| 도구 | 버전 | 역할 |
|------|------|------|
| Java | 25 | toolchain 설정 |
| ErrorProne | 최신 | 컴파일 타임 버그 탐지 |
| NullAway | 최신 | Null 안전성 정적 분석 |
| Spotless | 최신 | 코드 포맷 (Google Java Format) |
| Checkstyle | 최신 | 스타일 규칙 강제 |
| ArchUnit | 1.4.1 | 패키지 의존 방향 검증 |
| Fixture Monkey | 1.1.19 | 테스트 픽스처 자동 생성 |
| JUnit 5 | Spring Boot BOM | 단위/통합 테스트 |

```kotlin
// {bc}-domain/build.gradle.kts
label("java")
label("coverage-gate")
// dependencies {} 블록 없음 — 외부 의존 제로 (D-1)
```

### `spring` 라벨 포함 의존성

```kotlin
// 자동 추가됨 (선언 불필요)
implementation("org.springframework.boot:spring-boot-starter")
implementation("org.springframework.boot:spring-boot-starter-validation")
implementation("org.springframework.boot:spring-boot-starter-json")
```

### `boot` 라벨

```kotlin
// boilerplate-boot/build.gradle.kts
label("java", "spring", "boot")
// BootJar 태스크 활성화
```

### `jooq` 라벨

```kotlin
// {bc}-adapter-output-persist/build.gradle.kts
label("java", "spring", "jooq")
// jOOQ Codegen + H2(스키마 추출용) + spring-boot-starter-jooq 자동 추가
```

### `mapstruct` 라벨

```kotlin
// {bc}-adapter-input-api, {bc}-adapter-output-persist 에 필요 시 추가
label("java", "spring", "mapstruct")
// MapStruct + annotationProcessor 자동 설정
```

### `coverage-gate` 라벨

```kotlin
// domain, application 모듈에 선언
label("java", "coverage-gate")
// JaCoCo LINE 커버리지 80% 미달 시 빌드 실패
```

---

## 2. Gradle 태스크 실행 순서

```
compileJava → processResources → classes
    → test (JUnit 5)
        → jacocoTestReport → jacocoTestCoverageVerification (coverage-gate)
    → spotlessCheck → checkstyleMain
    → archUnitTest
```

- MUST: `./gradlew check` 로컬 실행 후 커밋한다.
- MUST NOT: `--no-daemon` 없이 CI에서 Gradle 실행한다 (메모리 누수 방지).

---

## 3. ArchUnit 검증 규칙 위치

```
boilerplate-boot/src/test/java/.../architecture/
    ArchitectureTest.java          # 의존 방향 + 금지 import
    ModulithStructureTest.java     # ApplicationModules.verify()
```

- MUST: ArchUnit 테스트는 `boilerplate-boot` 모듈에 위치한다.
- MUST: `ModulithStructureTest.verify()` 를 CI에서 항상 실행한다.

### ArchUnit 핵심 규칙 목록

```java
// domain → Spring 금지 (D-1)
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
    .check(classes);

// domain → UUID.randomUUID() 직접 호출 금지 (ADR-0011)
noClasses().that().resideInAPackage("..domain..")
    .should().callMethod(UUID.class, "randomUUID")
    .check(classes);

// application → Spring 금지 (A-1)
noClasses().that().resideInAPackage("..application..")
    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
    .check(classes);

// application → @Transactional 금지 (A-4) — 클래스 레벨
noClasses().that().resideInAPackage("..application..")
    .should().beAnnotatedWith(Transactional.class)
    .check(classes);

// application → @Transactional 금지 (A-4) — 메서드 레벨 (Tier 1 완전 보장)
noMembers().that().areDeclaredInClassesThat().resideInAPackage("..application..")
    .should().beAnnotatedWith(Transactional.class)
    .check(classes);

// adapter-input → domain 직접 참조 금지 (AD-1)
noClasses().that().resideInAPackage("..adapter.input..")
    .should().dependOnClassesThat().resideInAPackage("..domain.model..")
    .check(classes);
```

---

## 4. Spotless (코드 포맷)

```kotlin
// build-recipe-plugin이 java 라벨에서 자동 적용
spotless {
    java {
        googleJavaFormat("1.22.0")
        removeUnusedImports()
    }
}
```

- MUST: 커밋 전 `./gradlew spotlessApply` 실행하거나 IDE 저장 시 자동 포맷.
- MUST NOT: `spotlessCheck` 실패를 무시하고 PR을 병합한다.

---

## 5. NullAway + ErrorProne 설정

```kotlin
// build-recipe-plugin java 라벨 자동 설정
tasks.withType<JavaCompile> {
    options.errorprone {
        option("NullAway:AnnotatedPackages", "io.github.ppzxc.boilerplate")
    }
}
```

- MUST: NullAway 경고를 오류로 처리한다. 억제(`@SuppressWarnings`)는 ADR 근거 필수.
- MUST: domain/application 계층은 JSpecify 없이 `Objects.requireNonNull`로 Null-Safety 보장.
- MAY: adapter/configuration 계층에서 `@Nullable`/`@NonNull` 어노테이션 사용.

---

## 6. 커버리지 게이트 기준

| 모듈 | 라벨 | 커버리지 기준 | 측정 대상 |
|------|------|-------------|----------|
| `{bc}-domain` | `coverage-gate` | LINE 80% | 모든 Domain 클래스 |
| `{bc}-application` | `coverage-gate` | LINE 80% | 모든 Service 클래스 |
| `{bc}-adapter-*` | — | 커버리지 게이트 없음 | 통합 테스트로 보완 |

- MUST NOT: `{bc}-domain`, `{bc}-application`에서 커버리지 80% 미달 상태로 PR을 병합한다.

---

## 7. test-support 모듈

- MUST: 모든 BC의 테스트는 `test-support` 모듈을 `testImplementation`으로 의존한다. 하네스 코드 중복 금지.
- MUST: test-support 모듈은 DomainTestBase, AdapterTestBase, Fixture Factory 등 공용 테스트 인프라를 제공한다.

```kotlin
// 각 모듈 build.gradle.kts
dependencies {
    testImplementation(project(":test-support"))
}
```

### 7.1 DomainTestBase

Domain 테스트의 공통 기반. Spring Context 절대 금지.

```java
// test-support/src/main/java/.../DomainTestBase.java
public abstract class DomainTestBase {
    protected static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    protected static final Instant LATER = NOW.plusHours(1);

    protected static UUID fixedUuidV7(long epochMillis) {
        long msb = (epochMillis << 16) | 0x7000L | 0x0001L;
        long lsb = 0x8000000000000001L;
        return new UUID(msb, lsb);
    }
}
```

### 7.2 Testcontainers Singleton

모든 Adapter 통합 테스트가 공유하는 단일 PostgreSQL 컨테이너.

```java
// test-support/src/main/java/.../AdapterTestBase.java
@SpringBootTest
@Testcontainers
public abstract class AdapterTestBase {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("app").withUsername("app").withPassword("app");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

- MUST: `@Container` + `static`으로 테스트 클래스 간 컨테이너 재사용 (Singleton 패턴).

### 7.3 DB 클린업

- MUST: 각 테스트 메서드 전에 `@BeforeEach`에서 TRUNCATE로 데이터 초기화. `flyway_schema_history`와 `event_publication` 제외.

### 7.4 Fixture Factory

- MUST: Fixture 생성 시 `reconstitute()`를 사용한다. `create()`는 이벤트를 발행하므로 상태 설정 Fixture에 부적합.
- MUST: 고정 `Instant`를 사용한다. `Instant.now()` 호출 금지.

### 7.5 Clock 제어

- MUST: 테스트 환경에서 `Clock.fixed()`를 Bean으로 등록하여 결정적 시간 제어.

---

## 8. Lefthook (로컬 Git Hooks)

Lefthook은 Git hooks 관리자. CI 전 로컬에서 품질 검사를 실행하여 위반 코드 커밋/푸시 차단.
Lefthook의 목적은 "빠른 로컬 차단". CI(`cicd.md §1`)가 동일 검사를 다시 수행함.

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.com.fizzpod.lefthook)
}
```

**주요 구성 패턴 (실제 `lefthook.yml` 기준)**:

```yaml
# lefthook.yml — 핵심 패턴
min_version: 1.7.0
assert_lefthook_installed: true

pre-commit:
  parallel: true                      # job 동시 실행
  jobs:
    - name: spotless-check
      glob: "*.{java,kt,kts}"
      run: ./gradlew spotlessCheck    # --no-daemon 불필요 (로컬은 daemon ON)
      skip: [merge, rebase]
      fail_text: "❌ 자동 수정: ./gradlew spotlessApply"
    - name: checkstyle
      glob: "*.java"
      run: ./gradlew checkstyleMain
      skip: [merge, rebase]

commit-msg:
  jobs:
    - name: conventional-commits
      run: |
        head -1 "{1}" | grep -qE \
          '^(feat|fix|refactor|perf|chore|build|ci|docs|test|style|revert)(\(.+\))?!?: .+$' \
          || { printf '❌ Conventional Commits 형식 위반\n예: feat(identity): JWT 로그인 구현\n'; exit 1; }
      skip: [merge, rebase]

pre-push:
  parallel: true
  jobs:
    - name: unit-tests
      run: ./gradlew test -x :boilerplate-boot:test
      skip: [merge, rebase]
    - name: arch-and-modulith
      run: ./gradlew :boilerplate-boot:test --tests "*ArchitectureTest*" --tests "*ModulithStructureTest*"
      skip: [merge, rebase]
```

**핵심 원칙**:
- SHOULD: `lefthook.yml`은 리포지토리에 버전 관리. 팀 전원 동일 hooks. 로컬 오버라이드는 `lefthook-local.yml`(gitignore).
- MUST: `./gradlew lefthookInstall`로 Git hooks 자동 설치.
- MUST: 모든 job에 `skip: [merge, rebase]` 적용하여 머지/리베이스 중 hook 발동 방지.
- MUST NOT: `--no-daemon`을 로컬 hook에 적용한다. CI에만 적용 (§2 참조).

---

## 9. PIT Mutation Testing

PIT는 소스 코드에 의도적 변이(mutation)를 주입하고, 테스트가 이를 감지(kill)하는지 확인한다.

```kotlin
// 모듈별 적용
pitest {
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(setOf("io.github.ppzxc.boilerplate.*"))
    threads.set(Runtime.getRuntime().availableProcessors())
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    mutationThreshold.set(80)
    coverageThreshold.set(70)
}
```

- MUST: domain, application 모듈에 PIT를 적용한다. mutationThreshold 80% 미달 시 빌드 실패.
- JaCoCo(양적 검증) + PIT(질적 검증)을 보완적으로 사용한다.

---

## 10. OpenRewrite

OpenRewrite는 자동 코드 리팩토링 엔진. 감지뿐 아니라 자동 수정을 수행한다.

```kotlin
apply(plugin = "org.openrewrite.rewrite")
configure<RewriteExtension> {
    configFile = rootProject.file("rewrite.yml")
    activeRecipe("io.github.ppzxc.boilerplate.CodeQuality")
}
dependencies {
    add("rewrite", libs.org.openrewrite.recipe.static.analysis)
    add("rewrite", libs.org.openrewrite.recipe.migrate.java)
    add("rewrite", libs.org.openrewrite.recipe.spring)
}
```

| 레시피 | 역할 |
|--------|------|
| `static-analysis` | 불필요 import 제거, `final` 추가, 빈 블록 제거 |
| `migrate-java` | Java 25 API 활용, deprecated API 교체 |
| `spring` | Spring Boot 4 API 변경 자동 적용 |

- SHOULD: `./gradlew rewriteDryRun`으로 미리보기 후 `./gradlew rewriteRun`으로 자동 수정.

---

## fallback 지시문

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> `docs/decisions/` 디렉토리에서 관련 ADR을 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
