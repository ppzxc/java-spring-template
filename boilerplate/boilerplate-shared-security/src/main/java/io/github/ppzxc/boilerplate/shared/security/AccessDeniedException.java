package io.github.ppzxc.boilerplate.shared.security;

public final class AccessDeniedException extends RuntimeException {

  private final String requiredPermission;

  public AccessDeniedException(String requiredPermission) {
    super("Required permission: " + requiredPermission);
    this.requiredPermission = requiredPermission;
  }

  public String requiredPermission() {
    return requiredPermission;
  }
}
