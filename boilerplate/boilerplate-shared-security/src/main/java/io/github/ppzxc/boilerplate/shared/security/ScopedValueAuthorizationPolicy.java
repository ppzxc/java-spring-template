package io.github.ppzxc.boilerplate.shared.security;

public final class ScopedValueAuthorizationPolicy implements AuthorizationPolicy {

  @Override
  public void requirePermission(String resourceScope) {
    var perm = new Permission(resourceScope);
    var ctx = RequestScope.CTX.orElse(null);
    if (ctx == null || !ctx.permissions().contains(perm)) {
      throw new AccessDeniedException(resourceScope);
    }
  }
}
