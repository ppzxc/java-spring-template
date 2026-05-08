package io.github.ppzxc.boilerplate.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserCommand;
import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserResult;
import io.github.ppzxc.boilerplate.identity.application.port.out.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.out.SaveUserPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RegisterUserServiceTest {

  private final LoadUserPort loadPort = mock(LoadUserPort.class);
  private final SaveUserPort savePort = mock(SaveUserPort.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private final RegisterUserService sut = new RegisterUserService(loadPort, savePort, clock);

  @Test
  void 정상_등록() {
    when(loadPort.existsByEmail(any())).thenReturn(false);
    when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

    RegisterUserResult result =
        sut.execute(new RegisterUserCommand("홍길동", "test@example.com", "hashedPw"));

    assertThat(result.userId()).isNotNull();
    verify(savePort).save(any(User.class));
  }

  @Test
  void 중복_이메일_거부() {
    when(loadPort.existsByEmail(any())).thenReturn(true);

    assertThatThrownBy(
            () -> sut.execute(new RegisterUserCommand("홍길동", "dup@example.com", "hashedPw")))
        .isInstanceOf(UserException.AlreadyExistsException.class)
        .hasMessageContaining("dup@example.com");

    verify(savePort, never()).save(any());
  }

  @Test
  void null_loadPort_생성_실패() {
    assertThatThrownBy(() -> new RegisterUserService(null, savePort, clock))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void null_savePort_생성_실패() {
    assertThatThrownBy(() -> new RegisterUserService(loadPort, null, clock))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void null_clock_생성_실패() {
    assertThatThrownBy(() -> new RegisterUserService(loadPort, savePort, null))
        .isInstanceOf(NullPointerException.class);
  }
}
