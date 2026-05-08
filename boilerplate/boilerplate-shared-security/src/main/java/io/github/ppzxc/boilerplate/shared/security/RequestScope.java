package io.github.ppzxc.boilerplate.shared.security;

public final class RequestScope {

  public static final ScopedValue<RequestContext> CTX = ScopedValue.newInstance();

  private RequestScope() {}
}
