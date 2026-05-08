package io.github.ppzxc.boilerplate.shared;

public final class RequestScope {

  public static final ScopedValue<RequestContext> CTX = ScopedValue.newInstance();

  private RequestScope() {}
}
