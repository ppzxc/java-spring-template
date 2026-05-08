// identity-domain: 순수 Java, 외부 의존 제로 (D-1), 커버리지 80% 게이트
plugins {
  id("coverage-gate-conventions")
}

dependencies {
  testImplementation(project(":boilerplate-test-support"))
}
