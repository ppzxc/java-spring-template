package io.github.ppzxc.boilerplate.shared;

public final class ScopedValueAuthorizationPolicy implements AuthorizationPolicy {

  @Override
  public void requirePermission(String resourceScope) {
    var perm = new Permission(resourceScope);
    if (!RequestScope.CTX.isBound()) {
      throw new AccessDeniedException(resourceScope);
    }
    if (!RequestScope.CTX.get().permissions().contains(perm)) {
      throw new AccessDeniedException(resourceScope);
    }
  }
}
