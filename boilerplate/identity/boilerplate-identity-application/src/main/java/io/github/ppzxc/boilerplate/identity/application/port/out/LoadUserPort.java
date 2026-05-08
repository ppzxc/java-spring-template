package io.github.ppzxc.boilerplate.identity.application.port.out;

import io.github.ppzxc.boilerplate.identity.domain.model.Email;
import io.github.ppzxc.boilerplate.identity.domain.model.User;
import io.github.ppzxc.boilerplate.identity.domain.model.UserId;
import java.util.Optional;

public interface LoadUserPort {

  Optional<User> findById(UserId id);

  boolean existsByEmail(Email email);
}
