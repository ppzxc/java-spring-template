package io.github.ppzxc.boilerplate.identity.application.service;

import io.github.ppzxc.boilerplate.identity.application.dto.LoginCommand;
import io.github.ppzxc.boilerplate.identity.application.dto.TokenResponse;
import io.github.ppzxc.boilerplate.identity.application.port.input.LoginUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.output.IssueTokenPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.PasswordEncoderPort;
import io.github.ppzxc.boilerplate.identity.application.port.output.SaveRefreshTokenPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import java.time.Clock;
import java.time.Duration;

/** Login Application Service. */
public class LoginService implements LoginUseCase {

  private final LoadUserPort loadUserPort;
  private final PasswordEncoderPort passwordEncoderPort;
  private final IssueTokenPort issueTokenPort;
  private final SaveRefreshTokenPort saveRefreshTokenPort;
  private final Clock clock;

  public LoginService(
      LoadUserPort loadUserPort,
      PasswordEncoderPort passwordEncoderPort,
      IssueTokenPort issueTokenPort,
      SaveRefreshTokenPort saveRefreshTokenPort,
      Clock clock) {
    this.loadUserPort = loadUserPort;
    this.passwordEncoderPort = passwordEncoderPort;
    this.issueTokenPort = issueTokenPort;
    this.saveRefreshTokenPort = saveRefreshTokenPort;
    this.clock = clock;
  }

  @Override
  public TokenResponse execute(LoginCommand command) {
    var user =
        loadUserPort
            .findByEmail(new Email(command.email()))
            .orElseThrow(UserException.InvalidCredentialException::new);

    if (!passwordEncoderPort.matches(command.password(), user.hashedPassword())) {
      throw new UserException.InvalidCredentialException();
    }

    user.assertCanLogin();

    var tokenSet = issueTokenPort.issue(user);
    saveRefreshTokenPort.save(user.id(), tokenSet.refreshToken());

    var expiresIn =
        Duration.between(clock.instant(), tokenSet.accessToken().expiresAt()).toSeconds();

    return new TokenResponse(
        tokenSet.accessToken().value(), tokenSet.refreshToken().value(), expiresIn);
  }
}
