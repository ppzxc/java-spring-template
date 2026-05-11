package io.github.ppzxc.boilerplate.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ppzxc.boilerplate.identity.domain.event.DomainEvent;
import io.github.ppzxc.boilerplate.identity.domain.event.UserEvent;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.test.DomainTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserTest extends DomainTestBase {

  private static final UserName NAME = new UserName("홍길동");
  private static final Email EMAIL = new Email("hong@example.com");
  private static final HashedPassword PASSWORD = new HashedPassword("$2a$12$hashed");

  @Test
  void create_정상_생성_및_이벤트_발행() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);

    assertThat(user.id()).isNotNull();
    assertThat(user.userName()).isEqualTo(NAME);
    assertThat(user.email()).isEqualTo(EMAIL);
    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(user.version()).isEqualTo(0L);

    List<DomainEvent> events = user.pullDomainEvents();
    assertThat(events).hasSize(1);
    var event = (UserEvent.UserRegisteredEvent) events.get(0);
    assertThat(event.aggregateId()).isEqualTo(user.id().value());
    assertThat(event.occurredAt()).isEqualTo(NOW);
    assertThat(event.aggregateVersion()).isEqualTo(0L);
    assertThat(event.eventType()).isEqualTo("UserRegisteredEvent");
    assertThat(event.userName()).isEqualTo(NAME.value());
    assertThat(event.email()).isEqualTo(EMAIL.value());
  }

  @Test
  void create_null_userName_예외() {
    assertThatThrownBy(() -> User.create(null, EMAIL, PASSWORD, NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_null_email_예외() {
    assertThatThrownBy(() -> User.create(NAME, null, PASSWORD, NOW))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void create_후_pullDomainEvents_두번째_호출_빈_목록() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();
    assertThat(user.pullDomainEvents()).isEmpty();
  }

  @Test
  void suspend_정상_상태전이_및_이벤트() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();

    user.suspend(LATER);

    assertThat(user.status()).isEqualTo(UserStatus.SUSPENDED);
    var events = user.pullDomainEvents();
    assertThat(events).hasSize(1);
    var event = (UserEvent.UserSuspendedEvent) events.get(0);
    assertThat(event.aggregateId()).isEqualTo(user.id().value());
    assertThat(event.occurredAt()).isEqualTo(LATER);
    assertThat(event.eventType()).isEqualTo("UserSuspendedEvent");
  }

  @Test
  void suspend_이미_정지된_사용자_예외() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();
    user.suspend(LATER);

    assertThatThrownBy(() -> user.suspend(LATER))
        .isInstanceOf(UserException.AlreadySuspendedException.class);
  }

  @Test
  void deactivate_정상_상태전이_및_이벤트() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();

    user.deactivate(LATER);

    assertThat(user.status()).isEqualTo(UserStatus.DEACTIVATED);
    var events = user.pullDomainEvents();
    assertThat(events).hasSize(1);
    var event = (UserEvent.UserDeactivatedEvent) events.get(0);
    assertThat(event.aggregateId()).isEqualTo(user.id().value());
    assertThat(event.eventType()).isEqualTo("UserDeactivatedEvent");
  }

  @Test
  void deactivate_이미_비활성화된_사용자_예외() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();
    user.deactivate(LATER);

    assertThatThrownBy(() -> user.deactivate(LATER))
        .isInstanceOf(UserException.AlreadyDeactivatedException.class);
  }

  @Test
  void suspend_후_deactivate_정상() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();
    user.suspend(LATER);
    user.pullDomainEvents();

    user.deactivate(LATER);

    assertThat(user.status()).isEqualTo(UserStatus.DEACTIVATED);
  }

  @Test
  void assertCanLogin_ACTIVE_상태_정상() {
    var user = User.create(NAME, EMAIL, PASSWORD, NOW);
    user.pullDomainEvents();

    user.assertCanLogin(); // 예외 없이 통과해야 함
  }

  @Test
  void assertCanLogin_SUSPENDED_상태_예외() {
    var user =
        User.reconstitute(
            UserId.generate(), EMAIL, NAME, PASSWORD, NOW, UserStatus.SUSPENDED, NOW, NOW, 1L);

    assertThatThrownBy(() -> user.assertCanLogin())
        .isInstanceOf(UserException.IneligibleStatusException.class);
  }

  @Test
  void assertCanLogin_DEACTIVATED_상태_예외() {
    var user =
        User.reconstitute(
            UserId.generate(), EMAIL, NAME, PASSWORD, NOW, UserStatus.DEACTIVATED, NOW, NOW, 1L);

    assertThatThrownBy(() -> user.assertCanLogin())
        .isInstanceOf(UserException.IneligibleStatusException.class);
  }

  @Test
  void reconstitute_이벤트_미발행() {
    var id = UserId.generate();
    var user = User.reconstitute(id, EMAIL, NAME, PASSWORD, NOW, UserStatus.ACTIVE, NOW, NOW, 5L);

    assertThat(user.id()).isEqualTo(id);
    assertThat(user.version()).isEqualTo(5L);
    assertThat(user.pullDomainEvents()).isEmpty();
  }
}
