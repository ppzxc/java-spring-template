package io.github.ppzxc.boilerplate.boot.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ppzxc.boilerplate.shared.AccessDeniedException;
import io.github.ppzxc.boilerplate.shared.Permission;
import io.github.ppzxc.boilerplate.shared.RequestContext;
import io.github.ppzxc.boilerplate.shared.RequestScope;
import io.github.ppzxc.boilerplate.shared.ScopedValueAuthorizationPolicy;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScopedValueAuthorizationPolicyTest {

  private final ScopedValueAuthorizationPolicy policy = new ScopedValueAuthorizationPolicy();

  @Test
  void 권한_있으면_예외_없음() {
    var ctx = new RequestContext(UUID.randomUUID(), "tenant", Set.of(new Permission("user:read")));
    ScopedValue.where(RequestScope.CTX, ctx)
        .run(
            () ->
                assertThatCode(() -> policy.requirePermission("user:read"))
                    .doesNotThrowAnyException());
  }

  @Test
  void 권한_미매칭_AccessDeniedException() {
    var ctx = new RequestContext(UUID.randomUUID(), "tenant", Set.of(new Permission("user:read")));
    ScopedValue.where(RequestScope.CTX, ctx)
        .run(
            () ->
                assertThatThrownBy(() -> policy.requirePermission("user:write"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("user:write"));
  }

  @Test
  void CTX_미바인딩_AccessDeniedException() {
    assertThatThrownBy(() -> policy.requirePermission("user:read"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 빈_permissions_AccessDeniedException() {
    var ctx = new RequestContext(UUID.randomUUID(), "tenant", Set.of());
    ScopedValue.where(RequestScope.CTX, ctx)
        .run(
            () ->
                assertThatThrownBy(() -> policy.requirePermission("user:read"))
                    .isInstanceOf(AccessDeniedException.class));
  }
}
