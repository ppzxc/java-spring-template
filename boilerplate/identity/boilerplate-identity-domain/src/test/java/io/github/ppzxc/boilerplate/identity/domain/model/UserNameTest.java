package io.github.ppzxc.boilerplate.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserNameTest {

  @Test
  void 유효한_이름_생성() {
    assertThat(new UserName("홍길동").value()).isEqualTo("홍길동");
  }

  @Test
  void null_예외() {
    assertThatThrownBy(() -> new UserName(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void 빈_문자열_예외() {
    assertThatThrownBy(() -> new UserName("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new UserName("   ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 최대길이_초과_예외() {
    var tooLong = "a".repeat(51);
    assertThatThrownBy(() -> new UserName(tooLong)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 최대길이_정확히_허용() {
    var exactly50 = "a".repeat(50);
    assertThat(new UserName(exactly50).value()).hasSize(50);
  }
}
