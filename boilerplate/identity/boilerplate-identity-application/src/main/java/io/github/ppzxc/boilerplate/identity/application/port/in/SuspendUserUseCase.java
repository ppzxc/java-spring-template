package io.github.ppzxc.boilerplate.identity.application.port.in;

import io.github.ppzxc.boilerplate.identity.application.dto.SuspendUserCommand;

public interface SuspendUserUseCase {

  void execute(SuspendUserCommand command);
}
