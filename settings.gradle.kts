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

// ── Identity BC ────────────────────────────────────────────────────────
module(name = ":boilerplate-identity-domain",               path = "boilerplate/identity/boilerplate-identity-domain")
module(name = ":boilerplate-identity-application",          path = "boilerplate/identity/boilerplate-identity-application")
module(name = ":boilerplate-identity-adapter-input-api",    path = "boilerplate/identity/boilerplate-identity-adapter-input-api")
module(name = ":boilerplate-identity-adapter-output-persist", path = "boilerplate/identity/boilerplate-identity-adapter-output-persist")
module(name = ":boilerplate-identity-configuration",        path = "boilerplate/identity/boilerplate-identity-configuration")

// ── Notification BC ───────────────────────────────────────────────────
module(name = ":boilerplate-notification-domain",               path = "boilerplate/notification/boilerplate-notification-domain")
module(name = ":boilerplate-notification-application",          path = "boilerplate/notification/boilerplate-notification-application")
module(name = ":boilerplate-notification-adapter-input-event",  path = "boilerplate/notification/boilerplate-notification-adapter-input-event")
module(name = ":boilerplate-notification-adapter-output-persist", path = "boilerplate/notification/boilerplate-notification-adapter-output-persist")
module(name = ":boilerplate-notification-configuration",        path = "boilerplate/notification/boilerplate-notification-configuration")

// ── Audit BC ──────────────────────────────────────────────────────────
module(name = ":boilerplate-audit-domain",                 path = "boilerplate/audit/boilerplate-audit-domain")
module(name = ":boilerplate-audit-application",            path = "boilerplate/audit/boilerplate-audit-application")
module(name = ":boilerplate-audit-adapter-input-event",    path = "boilerplate/audit/boilerplate-audit-adapter-input-event")
module(name = ":boilerplate-audit-adapter-output-persist", path = "boilerplate/audit/boilerplate-audit-adapter-output-persist")
module(name = ":boilerplate-audit-configuration",          path = "boilerplate/audit/boilerplate-audit-configuration")

// ── Apps (실행 가능한 애플리케이션) ────────────────────────────────────
module(name = ":boilerplate-boot-api", path = "boilerplate/boilerplate-boot-api")
