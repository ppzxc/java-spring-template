# ADR-0017: Jib 컨테이너 이미지 빌드 도구 채택

## Status

Accepted

## Context

컨테이너 이미지 빌드 방법:
- **Dockerfile**: Docker 데몬 필요, 레이어 캐시 수동 관리, 빌드 환경 의존.
- **Buildpacks**: Cloud Native Buildpacks, 설정 제어 제한적.
- **Jib** (채택): Docker 데몬 불필요, Gradle/Maven 플러그인, 레이어 자동 최적화, OCI 표준 이미지 생성.

CI/CD 환경에서 Docker 데몬 없이 이미지를 빌드해야 하며, 레이어 캐시 최적화로 빌드 시간을 줄여야 한다.

Jib은 의존성, 리소스, 클래스 파일을 별도 레이어로 분리하여 변경된 레이어만 재빌드한다.

## Decision

Google Jib Gradle 플러그인을 채택한다.

```kotlin
// boilerplate-boot/build.gradle.kts
jib {
    from { image = "eclipse-temurin:25-jre" }
    to {
        image = "ghcr.io/ppzxc/boilerplate"
        tags = setOf("latest", project.version.toString())
    }
    container {
        jvmFlags = listOf(
            "-XX:+UseZGC",
            "-XX:+ZGenerational",
            "-XX:MaxRAMPercentage=75.0",
            "-Djava.security.egd=file:/dev/urandom"
        )
        ports = listOf("8080")
    }
}
```

기반 이미지: `eclipse-temurin:25-jre` (JRE only, 최소 크기).
JVM 플래그: ZGC (Java 25 권장 GC), MaxRAMPercentage 75%.

## Consequences

- ✅ Docker 데몬 불필요 → CI/CD 환경 단순화
- ✅ 레이어 자동 최적화 → 의존성 변경 없으면 클래스 레이어만 재빌드
- ✅ GHCR(GitHub Container Registry) 직접 푸시 가능
- ⚠️ Dockerfile 기반 커스터마이징 불가 → 일반적 Spring Boot 앱에는 문제 없음
