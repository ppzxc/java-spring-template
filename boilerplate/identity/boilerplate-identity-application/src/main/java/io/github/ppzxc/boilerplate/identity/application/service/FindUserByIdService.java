package io.github.ppzxc.boilerplate.identity.application.service;

import io.github.ppzxc.boilerplate.identity.application.dto.FindUserByIdQuery;
import io.github.ppzxc.boilerplate.identity.application.dto.UserSummary;
import io.github.ppzxc.boilerplate.identity.application.port.in.FindUserByIdUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.out.UserQueryPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import java.util.Objects;

public class FindUserByIdService implements FindUserByIdUseCase {

  private final UserQueryPort queryPort;

  public FindUserByIdService(UserQueryPort queryPort) {
    this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
  }

  @Override
  public UserSummary execute(FindUserByIdQuery query) {
    return queryPort
        .findSummaryById(query.userId())
        .orElseThrow(() -> new UserException.NotFoundException(query.userId()));
  }
}
