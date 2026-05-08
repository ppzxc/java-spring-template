package io.github.ppzxc.boilerplate.shared;

public interface AuthorizationPolicy {

  void requirePermission(String resourceScope);
}
