package io.github.ppzxc.boilerplate.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.time.Instant;
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

  // ── AD-1: adapter.input → domain.model 직접 참조 금지 ───────────────
  @Test
  void adapter_input_domain_model_직접_참조_금지() {
    noClasses()
        .that()
        .resideInAPackage("..adapter.input..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..domain.model..")
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
}
