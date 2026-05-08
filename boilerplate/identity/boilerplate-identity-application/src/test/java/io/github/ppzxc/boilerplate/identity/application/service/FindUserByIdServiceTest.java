package io.github.ppzxc.boilerplate.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.ppzxc.boilerplate.identity.application.dto.FindUserByIdQuery;
import io.github.ppzxc.boilerplate.identity.application.dto.UserSummary;
import io.github.ppzxc.boilerplate.identity.application.port.out.UserQueryPort;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindUserByIdServiceTest {

  private final UserQueryPort queryPort = mock(UserQueryPort.class);

  private final FindUserByIdService sut = new FindUserByIdService(queryPort);

  @Test
  void 정상_조회() {
    var userId = UUID.randomUUID().toString();
    var summary =
        new UserSummary(
            userId,
            "홍길동",
            "test@example.com",
            "ACTIVE",
            0L,
            java.time.Instant.parse("2026-01-01T00:00:00Z"),
            java.time.Instant.parse("2026-01-01T00:00:00Z"));
    when(queryPort.findSummaryById(userId)).thenReturn(Optional.of(summary));

    var result = sut.execute(new FindUserByIdQuery(userId));

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.userName()).isEqualTo("홍길동");
  }

  @Test
  void 사용자_없으면_예외() {
    var userId = UUID.randomUUID().toString();
    when(queryPort.findSummaryById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sut.execute(new FindUserByIdQuery(userId)))
        .isInstanceOf(UserException.NotFoundException.class)
        .hasMessageContaining(userId);
  }
}
