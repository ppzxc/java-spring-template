package io.github.ppzxc.boilerplate.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ppzxc.boilerplate.identity.application.dto.LoginCommand;
import io.github.ppzxc.boilerplate.identity.application.port.output.IssueTokenPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.PasswordEncoderPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.SaveRefreshTokenPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.AccessToken;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.RefreshToken;
import io.github.ppzxc.boilerplate.identity.domain.model.TokenSet;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

  private LoadUserPort loadUserPort;
  private PasswordEncoderPort passwordEncoderPort;
  private IssueTokenPort issueTokenPort;
  private SaveRefreshTokenPort saveRefreshTokenPort;
  private LoginService loginService;

  @BeforeEach
  void setUp() {
    loadUserPort = mock(LoadUserPort.class);
    passwordEncoderPort = mock(PasswordEncoderPort.class);
    issueTokenPort = mock(IssueTokenPort.class);
    saveRefreshTokenPort = mock(SaveRefreshTokenPort.class);
    loginService =
        new LoginService(loadUserPort, passwordEncoderPort, issueTokenPort, saveRefreshTokenPort);
  }

  @Test
  @DisplayName("성공: 올바른 정보로 로그인하면 토큰을 반환한다")
  void login_success() {
    // given
    var email = "test@example.com";
    var password = "password123";
    var command = new LoginCommand(email, password);
    var user =
        User.create(
            new UserName("test"), new Email(email), new HashedPassword("hashed"), Instant.now());

    when(loadUserPort.findByEmail(new Email(email))).thenReturn(Optional.of(user));
    when(passwordEncoderPort.matches(password, user.hashedPassword())).thenReturn(true);

    var tokenSet =
        new TokenSet(
            new AccessToken("access"),
            new RefreshToken("refresh", Instant.now().plusSeconds(3600)));
    when(issueTokenPort.issue(user)).thenReturn(tokenSet);

    // when
    var result = loginService.execute(command);

    // then
    assertThat(result.accessToken()).isEqualTo("access");
    assertThat(result.refreshToken()).isEqualTo("refresh");
    verify(saveRefreshTokenPort).save(user.id(), tokenSet.refreshToken());
  }

  @Test
  @DisplayName("실패: 비밀번호가 일치하지 않으면 예외가 발생한다")
  void login_fail_invalid_password() {
    // given
    var email = "test@example.com";
    var password = "wrong-password";
    var command = new LoginCommand(email, password);
    var user =
        User.create(
            new UserName("test"), new Email(email), new HashedPassword("hashed"), Instant.now());

    when(loadUserPort.findByEmail(new Email(email))).thenReturn(Optional.of(user));
    when(passwordEncoderPort.matches(password, user.hashedPassword())).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> loginService.execute(command))
        .isInstanceOf(UserException.InvalidCredentialException.class);
  }
}
