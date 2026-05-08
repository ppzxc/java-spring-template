pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

rootProject.name = "boilerplate"

fun module(name: String, path: String) {
  include(name)
  project(name).projectDir = file("$rootDir/$path")
}

// ── Shared ─────────────────────────────────────────────────────────────
module(name = ":boilerplate-shared-event",    path = "boilerplate/boilerplate-shared-event")
module(name = ":boilerplate-shared-security", path = "boilerplate/boilerplate-shared-security")

// ── Test Support ───────────────────────────────────────────────────────
module(name = ":boilerplate-test-support", path = "boilerplate/boilerplate-test-support")

// ── Core ───────────────────────────────────────────────────────────────
module(name = ":boilerplate-domain", path = "boilerplate/boilerplate-domain")
module(name = ":boilerplate-application", path = "boilerplate/boilerplate-application")

// ── Identity BC ────────────────────────────────────────────────────────
module(name = ":boilerplate-identity-domain",               path = "boilerplate/identity/boilerplate-identity-domain")
module(name = ":boilerplate-identity-application",          path = "boilerplate/identity/boilerplate-identity-application")
module(name = ":boilerplate-identity-adapter-input-api",    path = "boilerplate/identity/boilerplate-identity-adapter-input-api")
module(name = ":boilerplate-identity-adapter-output-persist", path = "boilerplate/identity/boilerplate-identity-adapter-output-persist")
module(name = ":boilerplate-identity-configuration",        path = "boilerplate/identity/boilerplate-identity-configuration")

// ── Apps (실행 가능한 애플리케이션) ────────────────────────────────────
module(name = ":boilerplate-boot-api", path = "boilerplate/boilerplate-boot-api")
