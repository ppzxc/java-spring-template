// identity-configuration: Bean 등록, TX 프록시, EventTranslator
plugins {
  id("spring-conventions")
}

dependencies {
  implementation(project(":boilerplate-identity-domain"))
  implementation(project(":boilerplate-identity-application"))
  implementation(project(":boilerplate-identity-adapter-input-api"))
  implementation(project(":boilerplate-identity-adapter-output-persist"))
  implementation(project(":boilerplate-shared-event"))
  implementation(libs.org.springframework.modulith.events.api)
}
