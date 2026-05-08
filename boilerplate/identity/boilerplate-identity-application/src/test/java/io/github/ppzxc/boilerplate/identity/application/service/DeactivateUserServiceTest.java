package io.github.ppzxc.boilerplate.identity.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ppzxc.boilerplate.identity.application.dto.DeactivateUserCommand;
import io.github.ppzxc.boilerplate.identity.application.port.out.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.out.SaveUserPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserId;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import io.github.ppzxc.boilerplate.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeactivateUserServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private final LoadUserPort loadPort = mock(LoadUserPort.class);
  private final SaveUserPort savePort = mock(SaveUserPort.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  private final DeactivateUserService sut = new DeactivateUserService(loadPort, savePort, clock);

  @Test
  void 정상_비활성화() {
    var user =
        User.reconstitute(
            new UserId(UUID.randomUUID()),
            new Email("test@example.com"),
            new UserName("홍길동"),
            new HashedPassword("hashed"),
            NOW,
            UserStatus.ACTIVE,
            NOW,
            NOW,
            0L);
    when(loadPort.findById(any())).thenReturn(Optional.of(user));
    when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    sut.execute(new DeactivateUserCommand(UUID.randomUUID().toString()));

    verify(savePort).save(user);
  }

  @Test
  void 사용자_없으면_예외() {
    when(loadPort.findById(any())).thenReturn(Optional.empty());

    var id = UUID.randomUUID().toString();
    assertThatThrownBy(() -> sut.execute(new DeactivateUserCommand(id)))
        .isInstanceOf(UserException.NotFoundException.class);

    verify(savePort, never()).save(any());
  }
}
