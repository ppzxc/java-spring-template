package io.github.ppzxc.boilerplate.identity.application.service;

import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserCommand;
import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserResult;
import io.github.ppzxc.boilerplate.identity.application.port.in.RegisterUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.out.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.out.SaveUserPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.HashedPassword;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserName;
import java.time.Clock;
import java.util.Objects;

public class RegisterUserService implements RegisterUserUseCase {

  private final LoadUserPort loadPort;
  private final SaveUserPort savePort;
  private final Clock clock;

  public RegisterUserService(LoadUserPort loadPort, SaveUserPort savePort, Clock clock) {
    this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
    this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public RegisterUserResult execute(RegisterUserCommand command) {
    var email = new Email(command.email());
    if (loadPort.existsByEmail(email)) {
      throw new UserException.AlreadyExistsException(command.email());
    }
    var userName = new UserName(command.userName());
    var password = new HashedPassword(command.hashedPassword());
    var user = User.create(userName, email, password, clock.instant());
    var saved = savePort.save(user);
    return new RegisterUserResult(
        saved.id().value().toString(),
        saved.userName().value(),
        saved.email().value(),
        saved.status().name(),
        saved.version(),
        saved.createdAt(),
        saved.updatedAt());
  }
}
