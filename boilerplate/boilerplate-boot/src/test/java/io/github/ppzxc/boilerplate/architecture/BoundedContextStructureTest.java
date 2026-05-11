package io.github.ppzxc.boilerplate.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

/** DDD 구조 규칙 — ArchitectureTest.java와 중복 없는 10개 추가 검증. */
class BoundedContextStructureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_PACKAGE_INFOS)
          .importPackages("io.github.ppzxc.boilerplate");

  /** Aggregate Root 식별자: pullDomainEvents() 메서드를 보유한 도메인 클래스. */
  private static final DescribedPredicate<JavaClass> HAS_PULL_DOMAIN_EVENTS =
      new DescribedPredicate<>("have pullDomainEvents() method") {
        @Override
        public boolean test(JavaClass javaClass) {
          return javaClass.getMethods().stream()
              .anyMatch(m -> m.getName().equals("pullDomainEvents"));
        }
      };

  /** ArchUnit 1.4.x에 areNotAbstract() 없음 — JavaModifier로 직접 검사. */
  private static final DescribedPredicate<JavaClass> IS_NOT_ABSTRACT =
      new DescribedPredicate<>("not be abstract") {
        @Override
        public boolean test(JavaClass javaClass) {
          return !javaClass.getModifiers().contains(JavaModifier.ABSTRACT);
        }
      };

  // ── D-8: domain 구체 클래스(Entity/Aggregate Root)는 final ─────────────
  @Test
  void domain_concreteClasses_areFinal() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .and()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .and()
        .areNotRecords()
        .and(IS_NOT_ABSTRACT)
        .should(
            new ArchCondition<>("be final") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                if (!item.getModifiers().contains(JavaModifier.FINAL)) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getFullName() + " is not final (D-8)"));
                } else {
                  events.add(
                      SimpleConditionEvent.satisfied(item, item.getFullName() + " is final"));
                }
              }
            })
        .check(CLASSES);
  }

  // ── D-8: Aggregate Root는 private 생성자만 보유 ─────────────────────────
  @Test
  void aggregateRoot_hasPrivateConstructorOnly() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .and(HAS_PULL_DOMAIN_EVENTS)
        .should()
        .haveOnlyPrivateConstructors()
        .check(CLASSES);
  }

  // ── scaffold: Aggregate Root는 static create 또는 reconstitute 보유 ─────
  @Test
  void aggregateRoot_hasStaticFactoryMethod() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .and(HAS_PULL_DOMAIN_EVENTS)
        .should(
            new ArchCondition<>("have static create() or reconstitute() method") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                boolean hasFactory =
                    item.getMethods().stream()
                        .anyMatch(
                            m ->
                                (m.getName().equals("create")
                                        || m.getName().equals("reconstitute"))
                                    && m.getModifiers().contains(JavaModifier.STATIC));
                if (!hasFactory) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getFullName()
                              + " does not have static create() or reconstitute() method"));
                }
              }
            })
        .check(CLASSES);
  }

  // ── D-5: domain public set* 메서드 금지 (Anemic Model 방어) ──────────────
  @Test
  void domain_noPublicSetterMethods() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .should(
            new ArchCondition<>("have no public set* methods") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                item.getMethods().stream()
                    .filter(
                        m ->
                            m.getName().startsWith("set")
                                && m.getModifiers().contains(JavaModifier.PUBLIC))
                    .forEach(
                        m ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item,
                                    item.getFullName()
                                        + "."
                                        + m.getName()
                                        + "() is a public setter (Anemic Model)")));
              }
            })
        .check(CLASSES);
  }

  // ── D-13: domain.event 패키지의 구현체(non-interface)는 record ──────────
  @Test
  void domain_eventImplementations_areRecords() {
    classes()
        .that()
        .resideInAPackage("..domain.event..")
        .and()
        .areNotInterfaces()
        .and(IS_NOT_ABSTRACT)
        .should()
        .beRecords()
        .check(CLASSES);
  }

  // ── D-10: domain은 adapter 패키지에 의존하지 않는다 ──────────────────────
  @Test
  void domain_noAdapterDependency() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .check(CLASSES);
  }

  // ── A-1 보완: application은 jOOQ에 의존하지 않는다 ──────────────────────
  @Test
  void application_noJooqDependency() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.jooq..")
        .check(CLASSES);
  }

  // ── A-1 보완: application은 Jackson에 의존하지 않는다 ────────────────────
  @Test
  void application_noJacksonDependency() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.fasterxml..")
        .check(CLASSES);
  }

  // ── AD-2: adapter.input은 adapter.output에 의존하지 않는다 ───────────────
  @Test
  void adapter_input_notDependsOnAdapterOutput() {
    noClasses()
        .that()
        .resideInAPackage("..adapter.input..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter.output..")
        .check(CLASSES);
  }

  // ── AD-2: adapter.output은 adapter.input에 의존하지 않는다 ───────────────
  @Test
  void adapter_output_notDependsOnAdapterInput() {
    noClasses()
        .that()
        .resideInAPackage("..adapter.output..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter.input..")
        .check(CLASSES);
  }
}
