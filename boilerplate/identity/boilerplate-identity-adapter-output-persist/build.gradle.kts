// identity-adapter-output-persist: jOOQ Persistence 계층
import org.jooq.meta.jaxb.Property

plugins {
  id("spring-conventions")
  alias(libs.plugins.org.jooq.codegen.gradle)
}

dependencies {
  implementation(project(":boilerplate-identity-application"))
  implementation(project(":boilerplate-identity-domain"))
  implementation(libs.org.springframework.boot.starter.jooq)
  jooqCodegen(libs.org.jooq.meta.extensions)
  testImplementation(project(":boilerplate-test-support"))
  testImplementation(libs.org.testcontainers.postgresql)
  testImplementation(libs.org.testcontainers.junit.jupiter)
  testImplementation(libs.org.postgresql.postgresql)
  testImplementation(libs.org.flywaydb.flyway.database.postgresql)
  testRuntimeOnly(libs.org.springframework.boot.starter.flyway)
}

jooq {
  configuration {
    generator {
      database {
        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
        properties =
            listOf(
                Property().apply {
                  key = "scripts"
                  value = "src/main/resources/db/migration"
                },
                Property().apply {
                  key = "sort"
                  value = "flyway"
                },
                Property().apply {
                  key = "defaultNameCase"
                  value = "lower"
                })
      }
      generate {
        isDeprecated = false
        isRecords = true
        isFluentSetters = false
      }
      target {
        packageName = "io.github.ppzxc.boilerplate.identity.persistence.jooq"
        directory = "build/generated-sources/jooq"
      }
    }
  }
}

sourceSets {
  main {
    java {
      srcDir("build/generated-sources/jooq")
    }
  }
}

tasks.named("compileJava") {
  dependsOn("jooqCodegen")
}

tasks.named("spotlessJava") {
  dependsOn("jooqCodegen")
}
