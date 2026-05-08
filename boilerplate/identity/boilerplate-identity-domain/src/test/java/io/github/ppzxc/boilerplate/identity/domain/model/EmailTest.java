package io.github.ppzxc.boilerplate.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

  @Test
  void 유효한_이메일_생성() {
    var email = new Email("user@example.com");
    assertThat(email.value()).isEqualTo("user@example.com");
  }

  @Test
  void null_이메일_예외() {
    assertThatThrownBy(() -> new Email(null)).isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid", "no-at", "@nodomain", "user@", " @ "})
  void 잘못된_형식_이메일_예외(String invalid) {
    assertThatThrownBy(() -> new Email(invalid)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 동등성_검증() {
    assertThat(new Email("a@b.com")).isEqualTo(new Email("a@b.com"));
    assertThat(new Email("a@b.com")).isNotEqualTo(new Email("c@d.com"));
  }
}
