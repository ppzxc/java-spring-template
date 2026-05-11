---
description: 테스트 피라미드 70/15/10/5, 계층별 테스트 전략, AI 자기검증 체크리스트
alwaysApply: true
---

# Testing Rules

테스트 전략 — 항상 로드.

> **요구 수준 키워드**: MUST, MUST NOT, SHOULD는 RFC 2119 기준.

---

## 1. 테스트 피라미드 (70/15/10/5)

| 계층 | 비율 | Spring Context | 도구 | 속도 |
|------|:---:|:---:|------|:---:|
| Domain 단위 | 70% | ❌ 금지 | JUnit 5 + AssertJ + jqwik | 최빠름 |
| Application 단위 | 15% | ❌ 금지 | JUnit 5 + Mockito (또는 In-Memory) | 빠름 |
| Adapter 통합 | 10% | ✅ 최소 | Testcontainers + @WebMvcTest + WireMock | 보통 |
| E2E | 5% | ✅ 전체 | RestTestClient + @SpringBootTest | 느림 |

- MUST: 테스트 작성 시 위 비율을 목표로 한다.
- MUST NOT: E2E 테스트에서 모든 케이스를 검증한다. 핵심 경로만.
- MUST: Domain 테스트는 순수 Java만 사용한다. Spring Context 로딩 금지.

---

## 2. Domain 단위 테스트

Spring 없이 순수 Java. `create()` / `reconstitute()` / 행위 메서드 / VO 자기검증 검증.

```java
class UserTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void create_정상_이벤트_발행() {
        var user = User.create(
            new UserName("홍길동"), new Email("test@example.com"),
            new HashedPassword("hashed_pw"), NOW
        );

        assertThat(user.id()).isNotNull();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);

        var events = user.pullDomainEvents();
        assertThat(events).hasSize(1)
            .first().isInstanceOf(UserRegisteredEvent.class);

        var event = (UserRegisteredEvent) events.get(0);
        assertThat(event.aggregateId()).isEqualTo(user.id().value());
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.aggregateVersion()).isEqualTo(0L);
    }

    @Test
    void create_null_파라미터_실패() {
        assertThatThrownBy(() ->
            User.create(null, new Email("a@b.com"), new HashedPassword("pw"), NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void 정지된_사용자_재정지_실패() {
        var user = User.create(new UserName("홍"), new Email("a@b.com"),
            new HashedPassword("p"), NOW);
        user.pullDomainEvents();  // create 이벤트 비움
        user.suspend(NOW.plusSeconds(1));

        assertThatThrownBy(() -> user.suspend(NOW.plusSeconds(2)))
            .isInstanceOf(UserAlreadySuspendedException.class);
    }
}
```

**VO 테스트**:

```java
class EmailTest {
    @Test
    void 유효한_이메일() {
        assertThat(new Email("user@example.com").value()).isEqualTo("user@example.com");
    }

    @Test
    void 잘못된_형식_거부() {
        assertThatThrownBy(() -> new Email("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Property-Based 테스트 (jqwik)**:

```java
class EmailPropertyTest {
    @Property
    void 유효한_이메일은_항상_생성_성공(@ForAll("validEmails") String email) {
        assertThatCode(() -> new Email(email)).doesNotThrowAnyException();
    }

    @Property
    void 골뱅이_없는_문자열은_항상_실패(
            @ForAll @StringLength(min = 1, max = 50) String input) {
        Assume.that(!input.contains("@"));
        assertThatThrownBy(() -> new Email(input))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Provide
    Arbitrary<String> validEmails() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(10)
        ).as((local, domain) -> local + "@" + domain + ".com");
    }
}
```

**Fixture 원칙**:
- MUST: Fixture 생성 시 `reconstitute()`를 사용한다. `create()`는 이벤트를 발행하므로 상태 설정 Fixture에 부적합.
- MUST: 고정 `Instant`를 사용한다. `Instant.now()` 호출 금지.

---

## 3. Application 단위 테스트

Port를 Mockito Mock으로 대체. Spring Context 금지. `new`로 Service 직접 생성.

```java
class RegisterUserServiceTest {
    private final LoadUserPort loadPort   = mock(LoadUserPort.class);
    private final SaveUserPort savePort   = mock(SaveUserPort.class);
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final RegisterUserService sut =
        new RegisterUserService(loadPort, savePort, clock);

    @Test
    void 정상_등록() {
        when(loadPort.existsByEmail(any())).thenReturn(false);

        var result = sut.execute(
            new RegisterUserCommand("홍길동", "test@example.com", "hashedPw"));

        assertThat(result.userId()).isNotNull();
        verify(savePort).save(any(User.class));
    }

    @Test
    void 중복_이메일_거부() {
        when(loadPort.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() ->
            sut.execute(new RegisterUserCommand("홍길동", "dup@example.com", "pw")))
            .isInstanceOf(DuplicateEmailException.class);
        verify(savePort, never()).save(any());
    }
}
```

**In-Memory Adapter 패턴 (상태 기반 검증이 필요할 때)**:

```java
// testFixtures 모듈에 위치
class InMemorySaveUserPort implements SaveUserPort {
    private final Map<UserId, User> store = new HashMap<>();

    @Override
    public void save(User user) { store.put(user.id(), user); }

    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    public int size() { return store.size(); }
}
```

| 기준 | Mockito Mock | In-Memory Adapter |
|------|:---:|:---:|
| 행위 검증 (`verify()`) | ✅ | ❌ |
| 상태 검증 (`findById()`) | ❌ | ✅ |
| 구현 부담 | 최소 | InMemory 구현 필요 |
| 테스트 현실성 | 보통 | 높음 |

---

## 4. Adapter Input API 테스트 (@WebMvcTest)

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean RegisterUserUseCase registerUseCase;

    @Test
    void 회원가입_201() throws Exception {
        when(registerUseCase.execute(any()))
            .thenReturn(new RegisterUserResult(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/identity/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"홍길동","email":"test@example.com","password":"secure"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    void 유효성_검증_실패_400() throws Exception {
        mockMvc.perform(post("/api/identity/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","email":"invalid","password":""}"""))
            .andExpect(status().isBadRequest());
    }
}
```

- MUST: Controller 테스트는 `@WebMvcTest`만 사용한다. `@SpringBootTest` 전체 로딩 금지.
- MUST: UseCase를 `@MockitoBean`으로 대체한다.

---

## 5. Adapter Input Event 테스트 (@ApplicationModuleTest)

```java
@ApplicationModuleTest
class UserEventHandlerTest {
    @Autowired SendWelcomeNotificationUseCase useCase;

    @Test
    void UserRegistered_이벤트_수신_시_알림_발송(Scenario scenario) {
        var event = new UserRegisteredIntegrationEvent(
            UUID.randomUUID(), Instant.parse("2026-01-01T00:00:00Z"));

        scenario.publish(event)
            .andWaitForStateChange(() -> /* 상태 확인 */)
            .andVerify(result -> assertThat(result).isNotNull());
    }
}

@ApplicationModuleTest
class IdentityModuleEventTest {
    @Autowired RegisterUserUseCase registerUseCase;

    @Test
    void 회원가입_시_IntegrationEvent_발행(PublishedEvents events) {
        registerUseCase.execute(
            new RegisterUserCommand("홍길동", "test@example.com", "pw"));

        assertThat(events.ofType(UserRegisteredIntegrationEvent.class)
            .matching(e -> e.userId() != null))
            .hasSize(1);
    }
}
```

- MUST NOT: 이벤트 핸들러를 직접 메서드 호출한다. Spring 이벤트 메커니즘을 경유해야 한다.
- MUST NOT: `Thread.sleep()`으로 비동기 대기한다. awaitility 또는 `Scenario` 사용.

---

## 6. Adapter Output Persist 테스트 (Testcontainers)

- MUST: 실제 PostgreSQL Testcontainers를 사용한다. H2 InMemory DB 금지.
- MUST: Flyway 마이그레이션을 적용한 후 테스트한다.

```java
@Testcontainers
class UserPersistenceAdapterTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("app").withUsername("app").withPassword("app");

    private DSLContext dsl;
    private UserPersistenceAdapter adapter;

    @BeforeEach
    void setup() {
        var ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser("app"); ds.setPassword("app");
        Flyway.configure().dataSource(ds).load().migrate();
        dsl = DSL.using(ds, SQLDialect.POSTGRES);
        adapter = new UserPersistenceAdapter(dsl, new UserPersistenceMapper(), event -> {});
    }

    @Test
    void save_and_load_왕복() {
        var now = Instant.parse("2026-01-01T00:00:00Z");
        var user = User.create(new UserName("홍길동"), new Email("test@example.com"),
            new HashedPassword("hashed"), now);
        adapter.save(user);

        var loaded = adapter.findById(user.id()).orElseThrow();
        assertThat(loaded.email()).isEqualTo(user.email());
        // reconstitute()로 복원 — 이벤트 없음
        assertThat(loaded.pullDomainEvents()).isEmpty();
    }

    @Test
    void optimistic_lock_충돌(AD-7) {
        var user = User.create(new UserName("홍"), new Email("lock@test.com"),
            new HashedPassword("p"), Instant.parse("2026-01-01T00:00:00Z"));
        adapter.save(user);

        var copy1 = adapter.findById(user.id()).orElseThrow();
        var copy2 = adapter.findById(user.id()).orElseThrow();

        copy1.suspend(Instant.parse("2026-01-01T00:00:01Z"));
        adapter.save(copy1);

        copy2.suspend(Instant.parse("2026-01-01T00:00:02Z"));
        assertThatThrownBy(() -> adapter.save(copy2))
            .isInstanceOf(OptimisticLockException.class);
    }
}
```

---

## 7. Adapter Output External 테스트 (WireMock)

```java
@WireMockTest(httpPort = 8089)
class EmailApiAdapterTest {
    private EmailApiAdapter adapter;

    @BeforeEach
    void setup() { adapter = new EmailApiAdapter("http://localhost:8089"); }

    @Test
    void 이메일_발송_성공() {
        stubFor(post(urlEqualTo("/api/emails"))
            .willReturn(aResponse().withStatus(200)
                .withBody("""{"messageId":"msg-123","status":"SENT"}""")));

        assertThat(adapter.send("to@test.com", "Subject", "Body").status())
            .isEqualTo("SENT");
    }

    @Test
    void 외부_API_500_실패() {
        stubFor(post(urlEqualTo("/api/emails"))
            .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> adapter.send("to@test.com", "Subject", "Body"))
            .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void 타임아웃_실패() {
        stubFor(post(urlEqualTo("/api/emails"))
            .willReturn(aResponse().withStatus(200).withFixedDelay(10_000)));

        assertThatThrownBy(() -> adapter.send("to@test.com", "Subject", "Body"))
            .isInstanceOf(ExternalApiException.class);
    }
}
```

- MUST NOT: 실제 외부 API에 연결한다. 비결정적 + 비용 발생.

---

## 8. Modulith 구조 검증

```java
class ModulithStructureTest {
    static ApplicationModules modules =
        ApplicationModules.of(BoilerplateApplication.class);

    @Test
    void 모듈_구조_검증() {
        modules.verify();
    }
}
```

**모듈 격리 테스트**:

```java
@ApplicationModuleTest(mode = BootstrapMode.STANDALONE)
class IdentityModuleIsolationTest {
    @MockitoBean NotificationPort notificationPort;

    @Autowired RegisterUserUseCase registerUseCase;

    @Test
    void Identity_모듈_단독_기동() {
        var result = registerUseCase.execute(
            new RegisterUserCommand("격리", "isolated@test.com", "pw"));
        assertThat(result.userId()).isNotNull();
    }
}
```

**BootstrapMode**: `STANDALONE`(해당 모듈만), `DIRECT_DEPENDENCIES`(직접 의존), `ALL_DEPENDENCIES`(전이 의존)

---

## 9. E2E 테스트

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserRegistrationE2ETest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired RestTestClient client;

    @Test
    void 회원가입_성공_201() {
        client.post().uri("/api/identity/users")
            .bodyValue(Map.of("name","E2E","email","e2e@test.com","password","pw"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody().jsonPath("$.userId").isNotEmpty();
    }

    @Test
    void 중복_이메일_409() {
        var body = Map.of("name","A","email","dup@e2e.com","password","p");
        client.post().uri("/api/identity/users").bodyValue(body)
            .exchange().expectStatus().isCreated();
        client.post().uri("/api/identity/users").bodyValue(body)
            .exchange().expectStatus().isEqualTo(409);
    }
}
```

- MUST: E2E는 핵심 경로만 검증한다. HTTP 응답만 검증하며 내부 상태 직접 접근 금지.

---

## 10. ArchUnit 검증

```java
class ArchitectureTest {
    private static final JavaClasses classes =
        new ClassFileImporter().importPackages("io.github.ppzxc.boilerplate");

    @Test
    void domain_Spring_의존_금지() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .check(classes);
    }

    @Test
    void domain_Instant_now_직접_호출_금지() {
        noClasses().that().resideInAPackage("..domain..")
            .should().callMethod(Instant.class, "now")
            .check(classes);
    }

    @Test
    void application_domain만_의존() {
        classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "..application..", "..domain..")
            .check(classes);
    }

    @Test
    void application_Transactional_어노테이션_금지() {
        noClasses().that().resideInAPackage("..application..")
            .should().beAnnotatedWith(
                "org.springframework.transaction.annotation.Transactional")
            .check(classes);
    }
}
```

---

## 10.1. Configuration 테스트

TX 프록시와 EventTranslator가 올바르게 동작하는지 검증.

### TX 프록시 검증

```java
@SpringBootTest
@Testcontainers
class TxProxyIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Autowired private RegisterUserUseCase registerUseCase;
    @Autowired private DSLContext dsl;

    @Test
    void UseCase_실행이_트랜잭션_내에서_완료() {
        var command = new RegisterUserCommand("TX테스트", "tx@test.com", "pw");
        registerUseCase.execute(command);
        var count = dsl.selectCount().from("users")
            .where(field("email").eq("tx@test.com")).fetchOne(0, int.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void UseCase_실패_시_롤백() {
        var cmd1 = new RegisterUserCommand("A", "rollback@test.com", "pw");
        registerUseCase.execute(cmd1);
        assertThatThrownBy(() -> registerUseCase.execute(
            new RegisterUserCommand("B", "rollback@test.com", "pw")));
        var count = dsl.selectCount().from("users")
            .where(field("email").eq("rollback@test.com")).fetchOne(0, int.class);
        assertThat(count).isEqualTo(1);
    }
}
```

### EventTranslator 검증

```java
@SpringBootTest
@Testcontainers
class EventTranslatorTest {
    @Autowired private RegisterUserUseCase registerUseCase;
    @Autowired private DSLContext dsl;

    @Test
    void DomainEvent에서_IntegrationEvent로_변환() {
        registerUseCase.execute(new RegisterUserCommand("이벤트", "evt@test.com", "pw"));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var publications = dsl.selectFrom("event_publication")
                .where(field("event_type").like("%UserRegisteredIntegrationEvent%"))
                .fetch();
            assertThat(publications).isNotEmpty();
        });
    }
}
```

- MUST: Configuration 테스트는 `{bc}-configuration` 모듈의 `src/test/java`에 위치한다.

---

## 11. 모듈별 테스트 파일 위치

| 모듈 | 테스트 파일 | 도구 |
|------|-----------|------|
| `{bc}-domain` | `{Subject}Test.java` | JUnit + AssertJ |
| `{bc}-domain` | `{Subject}PropertyTest.java` | jqwik |
| `{bc}-application` | `{Verb}{Subject}ServiceTest.java` | Mockito |
| `{bc}-application/testFixtures` | `InMemory{Subject}Port.java` | 수동 구현 |
| `{bc}-adapter-input-api` | `{Subject}ControllerTest.java` | @WebMvcTest |
| `{bc}-adapter-input-event` | `{Subject}EventHandlerTest.java` | @ApplicationModuleTest |
| `{bc}-adapter-output-persist` | `{Subject}PersistenceAdapterTest.java` | Testcontainers |
| `{bc}-adapter-output-external` | `{Subject}ApiAdapterTest.java` | WireMock |
| `boilerplate-boot` | `ModulithStructureTest.java` | ApplicationModules |
| `boilerplate-boot` | `{Flow}E2ETest.java` | RestTestClient |

---

## 12. AI 테스트 작성 자기검증 체크리스트

테스트를 생성한 후 반드시 아래 체크리스트를 확인한다.

**Domain — MUST**
- [ ] `create()` 정상 + 이벤트 5필드 검증 (eventId, aggregateId, occurredAt, aggregateVersion)
- [ ] `create()` null 파라미터 → `NullPointerException`
- [ ] 각 행위 메서드 정상 + 이벤트 발행
- [ ] 비정상 상태 전이 → `sealed DomainException` 하위 예외
- [ ] VO 동등성 + Compact Constructor 검증 실패
- [ ] Fixture: `reconstitute()` 사용 (`create()` 직접 사용 금지)
- [ ] 고정 `Instant` 사용 (`Instant.now()` 금지)
- [ ] Spring import 없음

**Application — MUST**
- [ ] Port를 Mockito mock 또는 In-Memory로 대체
- [ ] `Clock.fixed()` 사용
- [ ] 정상 → `savePort.save()` 호출 검증
- [ ] 비정상 → 예외 + `savePort` 미호출 검증
- [ ] Spring Context 금지 — `new`로 Service 직접 생성
- [ ] Spring import 없음

**Adapter Input API — MUST**
- [ ] `@WebMvcTest` — Controller만 로딩
- [ ] UseCase `@MockitoBean`
- [ ] HTTP 상태 코드 + 응답 JSON 검증
- [ ] 유효성 검증 실패 → 400

**Adapter Input Event — MUST**
- [ ] `@ApplicationModuleTest`
- [ ] `Scenario.publish(event)` 사용
- [ ] 이벤트 핸들러 직접 호출 금지

**Adapter Output Persist — MUST**
- [ ] Testcontainers + 실제 PostgreSQL (H2 금지)
- [ ] save → load 왕복
- [ ] Optimistic Lock 충돌 (AD-7)
- [ ] `reconstitute()` 복원 → `pullDomainEvents()` 빈 목록

**Adapter Output External — MUST**
- [ ] WireMock 사용 (실제 외부 API 금지)
- [ ] 성공/실패/타임아웃 3 시나리오

**Modulith — MUST**
- [ ] `ApplicationModules.of().verify()` 검증

**Configuration — SHOULD**
- [ ] TX 프록시: UseCase 실행이 트랜잭션 내에서 완료 (커밋 후 DB 데이터 존재)
- [ ] TX 프록시: UseCase 실패 시 롤백 검증
- [ ] EventTranslator: DomainEvent → IntegrationEvent 변환 검증 (event_publication 테이블)

**Modulith — MUST 보강**
- [ ] `@ApplicationModuleTest(STANDALONE)` 모듈 단독 기동 검증
- [ ] `PublishedEvents`로 Integration Event 발행 검증

**Domain Property-Based — SHOULD**
- [ ] jqwik `@Property` 테스트로 VO 경계값 검증 (유효/무효 입력)

---

## 13. 절대 금지 패턴

```
MUST NOT:
✗ Domain 테스트에서 @SpringBootTest
✗ Application 테스트에서 @Autowired 실제 Port
✗ Adapter 테스트에서 H2 InMemory DB
✗ E2E 테스트에서 내부 상태 직접 검증
✗ Instant.now() 사용 — 고정 Instant 필수
✗ Thread.sleep()으로 비동기 대기 — awaitility 또는 Scenario 사용
✗ create()로 Fixture 생성 후 이벤트 버리기 — reconstitute() 사용
✗ 이벤트 핸들러 직접 메서드 호출 (이벤트 메커니즘 우회)
✗ 실제 외부 API 호출 — WireMock 사용
✗ Controller에서 Domain 객체 직접 반환
```

---

## fallback 지시문

---
> 위 규칙을 현재 상황에 적용하기 어렵거나 규칙 간 충돌이 발생하면,
> 해당 계층의 ADR 파일을 `docs/decisions/` 에서 직접 읽어
> 결정의 배경을 파악한 후 최적의 대안을 제안하라.
