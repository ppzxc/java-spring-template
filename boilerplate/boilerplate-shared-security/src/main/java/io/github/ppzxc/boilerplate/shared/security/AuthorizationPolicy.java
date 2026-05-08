package io.github.ppzxc.boilerplate.shared.security;

public interface AuthorizationPolicy {

  void requirePermission(String resourceScope);
}
