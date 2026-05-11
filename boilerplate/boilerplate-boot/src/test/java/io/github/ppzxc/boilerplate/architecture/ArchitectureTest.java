package io.github.ppzxc.boilerplate.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class ArchitectureTest {

  private static final JavaClasses classes =
      new ClassFileImporter().importPackages("io.github.ppzxc.boilerplate");

  // ── D-1: domain → Spring 금지 ─────────────────────────────────────────
  @Test
  void domain_Spring_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .check(classes);
  }

  // ── D-2: domain → 프레임워크 어노테이션 금지 (javax/jakarta 포함) ──────
  @Test
  void domain_Jakarta_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("jakarta..")
        .check(classes);
  }

  // ── D-3: domain → 로깅 프레임워크 금지 ───────────────────────────────
  @Test
  void domain_SLF4J_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.slf4j..")
        .check(classes);
  }

  // ── D-4: domain → Instant.now() 직접 호출 금지 (Clock 주입) ─────────
  @Test
  void domain_Instant_now_직접_호출_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .callMethod(Instant.class, "now")
        .check(classes);
  }

  // ── D-4(확장): domain → System.currentTimeMillis() 금지 (UUIDv7 생성기 면제)
  // ADR-0011: UUIDv7 generator는 식별자 생성 구현 세부사항이므로 D-4 면제.
  // Instant 주입 시 monotonic counter spin-wait 데드락 위험으로 대안 없음.
  @Test
  void domain_System_currentTimeMillis_금지_UUIDv7_면제() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .doNotHaveSimpleName("UUIDv7")
        .should()
        .callMethod(System.class, "currentTimeMillis")
        .check(classes);
  }

  // ── ADR-0011: domain → UUID.randomUUID() 금지 (UUIDv7 강제) ─────────
  @Test
  void domain_UUID_randomUUID_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .callMethod(UUID.class, "randomUUID")
        .check(classes);
  }

  // ── domain → jOOQ 금지 ────────────────────────────────────────────────
  @Test
  void domain_jOOQ_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.jooq..")
        .check(classes);
  }

  // ── A-1: application → Spring 금지 ───────────────────────────────────
  @Test
  void application_Spring_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .check(classes);
  }

  // ── A-4: application → @Transactional 금지 (클래스 레벨) ─────────────
  @Test
  void application_Transactional_클래스_레벨_금지() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .beAnnotatedWith(Transactional.class)
        .check(classes);
  }

  // ── A-4: application → @Transactional 금지 (메서드 레벨) ─────────────
  @Test
  void application_Transactional_메서드_레벨_금지() {
    noMembers()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..application..")
        .should()
        .beAnnotatedWith(Transactional.class)
        .check(classes);
  }

  // ── AD-1: adapter.input → domain 직접 참조 금지 (model/exception/event/service 전체) ──
  // 예외: @RestControllerAdvice 예외 핸들러 — adapter.md §8에서 adapter-input-api 배치를 허용
  @Test
  void adapter_input_domain_직접_참조_금지() {
    noClasses()
        .that()
        .resideInAPackage("..adapter.input..")
        .and()
        .areNotAnnotatedWith(
            "org.springframework.web.bind.annotation.RestControllerAdvice")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..domain..")
        .check(classes);
  }

  // ── shared(security 타입) → Spring 의존 금지 ────────────────────────
  @Test
  void shared_security_타입_Spring_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("io.github.ppzxc.boilerplate.shared")
        .and()
        .haveSimpleNameNotEndingWith("IntegrationEvent")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .check(classes);
  }

  // ── domain → RequestScope 직접 의존 금지 ─────────────────────────────
  @Test
  void domain_RequestScope_접근_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("io.github.ppzxc.boilerplate.shared.RequestScope")
        .check(classes);
  }

  // ── domain → Spring Security 의존 금지 ───────────────────────────────
  @Test
  void domain_SpringSecurity_의존_금지() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.security..")
        .check(classes);
  }

  // ── ADR-0021: Result.isSuccess / isFailure 메서드 재도입 금지 ───────────
  // switch/fold 패턴 강제 — boolean 분기 anti-pattern 방지
  @Test
  void Result_isSuccess_isFailure_메서드_재도입_금지() {
    noMembers()
        .that()
        .haveName("isSuccess")
        .or()
        .haveName("isFailure")
        .should()
        .beDeclaredInClassesThat()
        .resideInAPackage("..shared.functional..")
        .check(classes);
  }

  // ── Query Type-A: Find/Get/List/Search/Show UseCase → Optional 또는 List 반환 강제 ──
  // ADR-0022: 단순 absence 조회는 Optional<T> 또는 List<T> 직반환.
  @Test
  void typeA_query_usecase_execute_메서드는_Optional_또는_List_반환() {
    methods()
        .that()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Find")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Get")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("List")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Search")
        .or()
        .areDeclaredInClassesThat()
        .haveSimpleNameStartingWith("Show")
        .and()
        .areDeclaredInClassesThat()
        .haveSimpleNameEndingWith("UseCase")
        .and()
        .areDeclaredInClassesThat()
        .resideInAPackage("..application.port.input..")
        .and()
        .haveName("execute")
        .should()
        .haveRawReturnType(Optional.class)
        .orShould()
        .haveRawReturnType(List.class)
        .check(classes);
  }
}
