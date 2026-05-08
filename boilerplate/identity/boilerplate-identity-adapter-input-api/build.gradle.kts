// identity-adapter-input-api: HTTP Controller 계층 — Spring 의존, domain 직접 참조 금지 (AD-1)
plugins {
  id("spring-conventions")
}

dependencies {
  implementation(project(":boilerplate-identity-application"))
  implementation(project(":boilerplate-shared-security"))
  implementation(libs.org.springframework.boot.starter.web)
  testImplementation(project(":boilerplate-test-support"))
  testImplementation(libs.org.springframework.boot.starter.webmvc.test)
}
