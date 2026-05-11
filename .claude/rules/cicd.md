---
description: CI/CD 핵심 정책 — 보안 스캔 게이트, 배포 전략, Jib 빌드, GitHub Actions, 릴리스 관리
alwaysApply: true
---

# CI/CD 정책 Rules

CI/CD 핵심 정책 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. CI 필수 게이트

모든 PR은 아래 게이트를 통과해야 병합된다.

| 게이트 | 도구 | 실패 시 |
|--------|------|--------|
| 컴파일 | Gradle `compileJava` | 즉시 차단 |
| 단위/통합 테스트 | `./gradlew test` | 즉시 차단 |
| 커버리지 (domain, app) | JaCoCo 80% LINE | 즉시 차단 |
| 코드 품질 | Spotless + Checkstyle | 즉시 차단 |
| 정적 분석 | ErrorProne + NullAway | 즉시 차단 |
| Modulith 구조 검증 | `ApplicationModules.verify()` | 즉시 차단 |
| **보안 취약점 스캔** | OWASP Dependency-Check | CVSS 7.0+ 즉시 차단 |

- MUST: CVSS 7.0 이상 취약점이 있는 의존성은 PR 병합 전에 업데이트하거나 억제(`suppress`) ADR을 작성한다.
- MUST NOT: 보안 스캔을 skip하거나 threshold를 임의로 높인다.

---

## 2. 보안 스캔 설정

```kotlin
// boilerplate-boot/build.gradle.kts
plugins {
    id("org.owasp.dependencycheck") version "12.x.x"  // libs.versions.toml에서 최신 버전 확인
}

dependencyCheck {
    failBuildOnCVSS = 7.0f          // CVSS 7.0+ = 빌드 실패
    suppressionFile = "dependency-check-suppressions.xml"
}
```

```xml
<!-- dependency-check-suppressions.xml 억제 형식 -->
<suppressions>
  <suppress>
    <notes>ADR-XXXX: [근거] 업그레이드 불가 이유</notes>
    <cve>CVE-XXXX-XXXXX</cve>
  </suppress>
</suppressions>
```

- MUST: 억제된 CVE는 반드시 ADR 번호와 근거를 포함한다.
- MUST: 억제된 CVE는 분기별 재검토한다.

---

## 3. 배포 전략 원칙

| 환경 | 전략 | 설명 |
|------|------|------|
| **운영(prod)** | Blue-Green 또는 Canary | 무중단 배포 필수 |
| **스테이징(staging)** | Rolling Update | 이전 버전과 새 버전 동시 실행 가능해야 함 |
| **개발(dev)** | 직접 교체 | 가용성 SLA 없음 |

- MUST: 운영 배포는 Blue-Green 또는 Canary 중 하나를 선택한다.
- MUST: DDL 변경은 Backward Compatible이어야 한다 (Rolling Update 중 구/신 앱 동시 실행 — scaffold.md §DDL Backward Compatibility 참조).
- MUST NOT: 운영에 직접 교체(In-place)로 배포한다.

---

## 4. 환경별 설정 분리

- MUST: `application.yml` 기본값 + `application-{env}.yml` 환경별 오버라이드 패턴 사용.
- MUST: 시크릿(DB 비밀번호, JWT Secret, API Key)은 환경 변수로 주입한다. 파일에 하드코딩 금지 (security.md §10 참조).
- MUST: `spring.profiles.active`는 배포 파이프라인에서 주입한다. 코드에 하드코딩 금지.

```yaml
# application.yml — 기본값 (하드코딩 금지)
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

---

## 5. 헬스체크 / 배포 완료 기준

- MUST: `/actuator/health/readiness` 가 `UP`을 반환한 후 트래픽 라우팅을 전환한다.
- MUST NOT: 컨테이너 시작 직후 바로 트래픽을 라우팅한다 (Spring Context 초기화 시간 필요).
- MUST: Liveness probe 실패 시 컨테이너를 재시작하는 정책을 설정한다.

---

## 6. Jib 컨테이너 이미지 빌드

Docker 파일 없이 OCI 이미지를 빌드. CI/CD 파이프라인에서 Gradle로 직접 이미지 생성.

> 근거: ADR-0017

```kotlin
// boilerplate-boot/build.gradle.kts
plugins {
    id("com.google.cloud.tools.jib") version "3.x.x"  // libs.versions.toml에서 최신 버전 확인
}

jib {
    from {
        image = "eclipse-temurin:25-jre"
    }
    to {
        image = "ghcr.io/ppzxc/boilerplate"
        tags = setOf("latest", project.version.toString())
    }
    container {
        jvmFlags = listOf(
            "-XX:+UseZGC",                          // ZGC — 저지연 GC (Java 25 기본)
            "-XX:+ZGenerational",                    // Generational ZGC
            "-XX:MaxRAMPercentage=75.0",             // 컨테이너 메모리 75% 제한
            "-Djava.security.egd=file:/dev/urandom"  // 빠른 난수 초기화
        )
        ports = listOf("8080")
        environment = mapOf(
            "SPRING_PROFILES_ACTIVE" to "prod"
        )
    }
}
```

- MUST: `jib.to.image`에 버전 태그와 `latest` 태그를 모두 포함한다.
- MUST: JVM 플래그에 ZGC(`-XX:+UseZGC`)를 포함한다. Java 25의 기본 GC이나 명시적으로 설정한다.
- MUST: `-XX:MaxRAMPercentage`로 컨테이너 메모리 상한을 설정한다.
- MUST NOT: `Dockerfile`을 직접 작성하여 이미지를 빌드한다. Jib을 사용한다.

**CD 워크플로우 (push step)**

```yaml
# .github/workflows/cd.yml (일부)
- name: Build and push image
  run: ./gradlew :boilerplate-boot:jib
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 7. GitHub Actions CI 파이프라인

> 근거: ADR-0018

모든 PR에 대해 5개 게이트를 순서대로 실행한다.

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: 'gradle'

      # Gate 1: 컴파일 + 코드 품질
      - name: Compile & Code Quality
        run: ./gradlew compileJava spotlessCheck checkstyleMain --no-daemon

      # Gate 2: 단위/통합 테스트 + 커버리지
      - name: Test & Coverage
        run: ./gradlew test jacocoTestCoverageVerification --no-daemon

      # Gate 3: 정적 분석 (ErrorProne + NullAway — compileJava에 포함)
      - name: Static Analysis
        run: ./gradlew compileTestJava --no-daemon

      # Gate 4: Modulith 구조 검증 + ArchUnit
      - name: Architecture Verification
        run: ./gradlew test --tests "*ModulithStructureTest*" --tests "*ArchitectureTest*" --no-daemon

      # Gate 5: 보안 취약점 스캔
      - name: Security Scan (OWASP)
        run: ./gradlew dependencyCheckAnalyze --no-daemon
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
```

- MUST: CI 파이프라인은 `pull_request` + `push` 이벤트 모두에서 실행한다.
- MUST: Gradle 실행 시 `--no-daemon` 플래그를 포함한다 (메모리 누수 방지 — harness.md §2 참조).
- MUST: Java 캐시를 활성화하여 빌드 시간을 단축한다 (`cache: 'gradle'`).
- MUST: NVD API Key를 GitHub Secrets에 등록한다. 없으면 OWASP 스캔이 느려짐.
- MUST NOT: `secrets.*`를 코드에 하드코딩한다.

---

## 8. 릴리스 관리

### Semantic Versioning

- MUST: 버전 형식은 `MAJOR.MINOR.PATCH` (SemVer 2.0).
- MUST: Breaking Change → MAJOR 증가. 새 기능 → MINOR 증가. 버그 수정 → PATCH 증가.
- MUST NOT: SNAPSHOT, RC 등 비표준 접미사를 운영 배포에 사용한다.

| 변경 유형 | 예시 | 버전 변경 |
|----------|------|---------|
| Breaking API change | Command/Event 스키마 비호환 변경 | 1.0.0 → 2.0.0 |
| 새 기능 (호환) | 신규 UseCase 추가 | 1.0.0 → 1.1.0 |
| 버그 수정 | 특정 조건에서의 NPE 수정 | 1.0.0 → 1.0.1 |

### Git 태깅

```bash
# 릴리스 태그 생성 (v 접두어 필수)
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

- MUST: 릴리스 태그는 `v{MAJOR}.{MINOR}.{PATCH}` 형식 (`v` 접두어 필수).
- MUST NOT: `main` 브랜치에 직접 태그를 수동으로 붙이지 않는다. GitHub Actions Release 워크플로우를 통해 자동화한다.

### Release 워크플로우

```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Extract version from tag
        id: version
        run: echo "version=${GITHUB_REF_NAME#v}" >> $GITHUB_OUTPUT

      - name: Build image and push
        run: ./gradlew :boilerplate-boot:jib -Pversion=${{ steps.version.outputs.version }} --no-daemon
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          generate_release_notes: true
          tag_name: ${{ github.ref_name }}
```

- MUST: Release 워크플로우는 `v*` 태그 푸시 이벤트에만 실행한다.
- MUST: `packages: write` 권한으로 GHCR에 이미지를 푸시한다.
- MUST: `generate_release_notes: true`로 GitHub이 PR 기반 릴리스 노트를 자동 생성한다.

---

## fallback 지시문

> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 관련 ADR을 `docs/decisions/`에서 직접 읽어 결정 배경을 파악하라.
