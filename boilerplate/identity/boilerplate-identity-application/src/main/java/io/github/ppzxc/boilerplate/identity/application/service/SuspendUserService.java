package io.github.ppzxc.boilerplate.identity.application.service;

import io.github.ppzxc.boilerplate.identity.application.dto.SuspendUserCommand;
import io.github.ppzxc.boilerplate.identity.application.port.in.SuspendUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.out.LoadUserPort;
import io.github.ppzxc.boilerplate.identity.application.port.out.SaveUserPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import io.github.ppzxc.boilerplate.identity.domain.model.UserId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class SuspendUserService implements SuspendUserUseCase {

  private final LoadUserPort loadPort;
  private final SaveUserPort savePort;
  private final Clock clock;

  public SuspendUserService(LoadUserPort loadPort, SaveUserPort savePort, Clock clock) {
    this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
    this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public void execute(SuspendUserCommand command) {
    var userId = new UserId(UUID.fromString(command.userId()));
    var user =
        loadPort
            .findById(userId)
            .orElseThrow(() -> new UserException.NotFoundException(command.userId()));
    user.suspend(clock.instant());
    savePort.save(user);
  }
}
