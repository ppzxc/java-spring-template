// boilerplate-boot: Spring Boot 진입점 (전체 조립 + 보안 + 관측 인프라, port 8080)
// boot-conventions가 java + spring + boot 누적 적용 (BootJar 활성화 포함)
plugins {
  id("boot-conventions")
}

tasks.withType<Test>().configureEach {
  systemProperty("spring.profiles.active", "test")
}

dependencies {
  implementation(project(":boilerplate-shared-event"))
  implementation(project(":boilerplate-shared-security"))
  implementation(project(":boilerplate-identity-configuration"))
  implementation(project(":boilerplate-notification-configuration"))
  implementation(project(":boilerplate-audit-configuration"))
  implementation(libs.org.springframework.modulith.starter.jdbc)
  implementation(libs.org.springframework.modulith.events.api)
  implementation(libs.org.springframework.boot.starter.web)
  implementation(libs.org.springframework.boot.starter.actuator)
  implementation(libs.org.springframework.boot.starter.flyway)
  implementation(libs.org.springframework.boot.starter.security)
  implementation(libs.org.springframework.boot.starter.oauth2.resource.server)
  implementation(libs.org.springframework.boot.starter.opentelemetry)
  developmentOnly(libs.org.springframework.boot.docker.compose)
  runtimeOnly(libs.org.flywaydb.flyway.database.postgresql)
  runtimeOnly(libs.org.postgresql.postgresql)
  testImplementation(libs.org.springframework.modulith.core)
  testImplementation(libs.org.springframework.modulith.test)
  testImplementation(project(":boilerplate-identity-domain"))
  testImplementation(project(":boilerplate-identity-application"))
  testImplementation(libs.org.springframework.security.test)
  testImplementation(project(":boilerplate-test-support"))
  testImplementation(libs.org.awaitility)
}
