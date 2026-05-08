// identity-application: identity-domain만 의존 (A-1), 커버리지 80% 게이트
plugins {
  id("coverage-gate-conventions")
}

dependencies {
  api(project(":boilerplate-identity-domain"))
  testImplementation(project(":boilerplate-test-support"))
}
