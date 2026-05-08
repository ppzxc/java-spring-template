package io.github.ppzxc.boilerplate.shared;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record RequestContext(UUID userId, String tenantId, Set<Permission> permissions) {

  public RequestContext {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions must not be null"));
  }
}
