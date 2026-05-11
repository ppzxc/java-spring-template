package io.github.ppzxc.boilerplate.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
        .areNotAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
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

  // ── D-13: domain event record는 5필드 필수 ────────────────────────────
  // eventId, eventType, aggregateId, occurredAt, aggregateVersion
  private static final ArchCondition<JavaClass> HAVE_DOMAIN_EVENT_FIELDS =
      new ArchCondition<>("have required domain event fields") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          Set<String> fieldNames =
              javaClass.getFields().stream().map(f -> f.getName()).collect(Collectors.toSet());
          for (String required :
              Set.of("eventId", "eventType", "aggregateId", "occurredAt", "aggregateVersion")) {
            if (!fieldNames.contains(required)) {
              conditionEvents.add(
                  SimpleConditionEvent.violated(
                      javaClass,
                      "Missing required domain event field '"
                          + required
                          + "' in "
                          + javaClass.getName()));
            }
          }
        }
      };

  @Test
  void domain_event_record_5필드_필수() {
    classes()
        .that()
        .resideInAPackage("..domain.event..")
        .and()
        .areRecords()
        .should(HAVE_DOMAIN_EVENT_FIELDS)
        .check(classes);
  }

  // ── A-2: application.port.input의 UseCase는 인터페이스 강제 ─────────────
  @Test
  void application_UseCase_인터페이스_강제() {
    classes()
        .that()
        .resideInAPackage("..application.port.input..")
        .and()
        .haveSimpleNameEndingWith("UseCase")
        .should()
        .beInterfaces()
        .check(classes);
  }

  // ── AD-3: ApplicationEventPublisher를 주입받는 PersistenceAdapter는 pullDomainEvents() 호출 필수 ──
  // VO-only 저장 어댑터(RefreshTokenPersistenceAdapter 등)는 ApplicationEventPublisher를 갖지 않으므로 제외
  private static final ArchCondition<JavaClass> CALL_PULL_DOMAIN_EVENTS_IF_HAS_PUBLISHER =
      new ArchCondition<>("call pullDomainEvents() when ApplicationEventPublisher is injected") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          boolean hasPublisher =
              javaClass.getFields().stream()
                  .anyMatch(
                      f ->
                          f.getRawType()
                              .getName()
                              .equals("org.springframework.context.ApplicationEventPublisher"));
          if (!hasPublisher) {
            return;
          }
          boolean callsPullDomainEvents =
              javaClass.getMethodCallsFromSelf().stream()
                  .anyMatch(call -> call.getName().equals("pullDomainEvents"));
          if (!callsPullDomainEvents) {
            conditionEvents.add(
                SimpleConditionEvent.violated(
                    javaClass,
                    javaClass.getName()
                        + " has ApplicationEventPublisher but does not call pullDomainEvents() (AD-3)"));
          }
        }
      };

  @Test
  void adapter_output_persist_PersistenceAdapter_pullDomainEvents_호출() {
    classes()
        .that()
        .resideInAPackage("..adapter.output.persist..")
        .and()
        .haveSimpleNameEndingWith("PersistenceAdapter")
        .should(CALL_PULL_DOMAIN_EVENTS_IF_HAS_PUBLISHER)
        .check(classes);
  }

  // ── AD-5: persist 계층은 reconstitute() 호출 필수 ─────────────────────
  private static final ArchCondition<JavaClass> CALL_RECONSTITUTE =
      new ArchCondition<>("call reconstitute()") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          boolean found =
              javaClass.getMethodCallsFromSelf().stream()
                  .anyMatch(call -> call.getName().equals("reconstitute"));
          if (!found) {
            conditionEvents.add(
                SimpleConditionEvent.violated(
                    javaClass, javaClass.getName() + " does not call reconstitute() (AD-5)"));
          }
        }
      };

  @Test
  void adapter_output_persist_Mapper_reconstitute_호출() {
    classes()
        .that()
        .resideInAPackage("..adapter.output.persist..")
        .and()
        .haveSimpleNameEndingWith("PersistenceMapper")
        .should(CALL_RECONSTITUTE)
        .check(classes);
  }

  // ── A-9: 1 TX = 1 Aggregate — Service는 Save*Port를 하나만 주입 ────────
  private static final ArchCondition<JavaClass> HAVE_AT_MOST_ONE_SAVE_PORT =
      new ArchCondition<>("have at most one Save*Port field") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents conditionEvents) {
          long savePortCount =
              javaClass.getFields().stream()
                  .filter(f -> f.getRawType().getSimpleName().startsWith("Save"))
                  .count();
          if (savePortCount > 1) {
            conditionEvents.add(
                SimpleConditionEvent.violated(
                    javaClass,
                    javaClass.getName()
                        + " injects "
                        + savePortCount
                        + " Save*Port(s) — violates A-9 (1 TX = 1 Aggregate)"));
          }
        }
      };

  @Test
  void application_service_Save포트_단일_주입() {
    classes()
        .that()
        .resideInAPackage("..application.service..")
        .and()
        .haveSimpleNameEndingWith("Service")
        .should(HAVE_AT_MOST_ONE_SAVE_PORT)
        .check(classes);
  }

  // ── A-10: application.dto 클래스는 record 강제 ──────────────────────────
  @Test
  void application_dto는_record_강제() {
    classes().that().resideInAPackage("..application.dto..").should().beRecords().check(classes);
  }
}
