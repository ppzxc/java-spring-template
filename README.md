# spring-vibe-boilerplate

사내 프로젝트 시작점. Java 25 + Spring Boot 4.0.6 기반의 IDP 서버 + Resource Server 보일러플레이트.


**아키텍처**: 순수주의 DDD + 헥사고날 아키텍처 + CQRS Level 1 + Spring Modulith 2.0

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring Modulith | 2.0.1 |
| jOOQ | 3.19.30 |
| PostgreSQL | 17 |

## 모듈 구조

```
boilerplate/
  boilerplate-boot/        # 전체 조립, Controller, Adapter, TX 프록시
  boilerplate-shared-event/    # BC 간 Integration Event (Published Language)
  boilerplate-shared-security/ # 공통 보안 설정
  boilerplate-test-support/    # 테스트 픽스처 공용 인프라
  identity/                    # Identity BC (Core Domain)
  notification/                # Notification BC (Supporting Domain)
  audit/                       # Audit BC (Supporting Domain)
```

각 BC 모듈 패턴: `boilerplate-{bc}-{domain|application|adapter-input-api|adapter-output-persist|configuration}`

## 빠른 시작

```bash
# 인프라 기동 (PostgreSQL + Grafana LGTM)
docker compose up -d

# 빌드 + 테스트
./gradlew build --no-daemon

# 애플리케이션 실행
./gradlew :boilerplate-boot:bootRun --no-daemon
```

## CI/CD

- **CI**: `.github/workflows/ci.yml` — PR/push 시 5게이트 자동 실행
- **릴리스**: `.github/workflows/release.yml` — `v*` 태그 푸시 → Jib GHCR 이미지 빌드

### GitHub Secrets 필요 항목

| Secret | 용도 |
|--------|------|
| `NVD_API_KEY` | OWASP Dependency-Check NVD API (없으면 스캔 느림) |

## Development Setup (로컬 개발 환경)

### Git Hooks (Lefthook)

`./gradlew build` 실행 시 Lefthook 훅이 자동 설치됨. 이후 커밋/푸시 시 자동 검사 실행.

수동 설치:
```bash
./gradlew lefthookInstall
```

개인 hook 추가/비활성화(팀 공유 안 됨):
```bash
# lefthook-local.yml 생성 (이미 .gitignore에 포함)
printf 'pre-commit:\n  skip: true\n' > lefthook-local.yml
```

### Hook 우회 (비권장)

```bash
# pre-commit/commit-msg 건너뜀 — 긴급 상황에만 사용
git commit --no-verify -m "..."
# pre-push 건너뜀
git push --no-verify
```

> ⚠️ `--no-verify`는 품질 검사를 무력화함. CI에서 재차 차단.

---

## 개발 규칙

- **규칙 정본**: `.claude/rules/*.md` (15개 파일)
- **결정 기록**: `docs/decisions/0001~0020-*.md` (ADR 20건)
- **Inside-Out 개발**: domain → application → adapter → configuration → DDL 순서 (`scaffold.md` 참조)

## 헬스체크

```
GET /actuator/health/readiness  → 트래픽 라우팅 기준
GET /actuator/health/liveness   → 컨테이너 재시작 기준
```
