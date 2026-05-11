package io.github.ppzxc.boilerplate.shared.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultTest {

  @Test
  void success_생성_및_타입_확인() {
    Result<String, Integer> result = Result.success("ok");

    assertThat(result).isInstanceOf(Result.Success.class);
    String value =
        switch (result) {
          case Result.Success<String, Integer>(var v) -> v;
          case Result.Failure<String, Integer>(var e) -> null;
        };
    assertThat(value).isEqualTo("ok");
  }

  @Test
  void failure_생성_및_타입_확인() {
    Result<String, Integer> result = Result.failure(42);

    assertThat(result).isInstanceOf(Result.Failure.class);
    Integer error =
        switch (result) {
          case Result.Success<String, Integer>(var v) -> null;
          case Result.Failure<String, Integer>(var e) -> e;
        };
    assertThat(error).isEqualTo(42);
  }

  @Test
  void map_Success_변환() {
    Result<Integer, String> result = Result.<String, String>success("hello").map(String::length);

    assertThat(result).isInstanceOf(Result.Success.class);
    int value = result.fold(v -> v, e -> -1);
    assertThat(value).isEqualTo(5);
  }

  @Test
  void map_Failure_무시() {
    Result<Integer, String> result = Result.<String, String>failure("err").map(String::length);

    assertThat(result).isInstanceOf(Result.Failure.class);
    String value = result.fold(Object::toString, e -> e);
    assertThat(value).isEqualTo("err");
  }

  @Test
  void flatMap_Success_체인() {
    Result<Integer, String> result =
        Result.<String, String>success("hello").flatMap(s -> Result.success(s.length()));

    assertThat(result).isInstanceOf(Result.Success.class);
    int value = result.fold(v -> v, e -> -1);
    assertThat(value).isEqualTo(5);
  }

  @Test
  void flatMap_Failure_단락() {
    Result<Integer, String> result =
        Result.<String, String>failure("err").flatMap(s -> Result.success(s.length()));

    assertThat(result).isInstanceOf(Result.Failure.class);
    String value = result.fold(Object::toString, e -> e);
    assertThat(value).isEqualTo("err");
  }

  @Test
  void mapError_Success_무시() {
    Result<String, Integer> result = Result.<String, String>success("ok").mapError(String::length);

    assertThat(result).isInstanceOf(Result.Success.class);
    String value = result.fold(v -> v, Object::toString);
    assertThat(value).isEqualTo("ok");
  }

  @Test
  void mapError_Failure_변환() {
    Result<String, Integer> result = Result.<String, String>failure("err").mapError(String::length);

    assertThat(result).isInstanceOf(Result.Failure.class);
    int value = result.fold(v -> -1, e -> e);
    assertThat(value).isEqualTo(3);
  }

  @Test
  void fold_Success_경로() {
    Result<String, Integer> result = Result.success("value");

    String output = result.fold(v -> "S:" + v, e -> "F:" + e);

    assertThat(output).isEqualTo("S:value");
  }

  @Test
  void fold_Failure_경로() {
    Result<String, Integer> result = Result.failure(99);

    String output = result.fold(v -> "S:" + v, e -> "F:" + e);

    assertThat(output).isEqualTo("F:99");
  }

  @Test
  void onSuccess_사이드이펙트_Success() {
    List<String> captured = new ArrayList<>();
    Result<String, Integer> result = Result.success("hello");

    result.onSuccess(captured::add);

    assertThat(captured).containsExactly("hello");
  }

  @Test
  void onSuccess_사이드이펙트_Failure_무시() {
    List<String> captured = new ArrayList<>();
    Result<String, Integer> result = Result.failure(1);

    result.onSuccess(captured::add);

    assertThat(captured).isEmpty();
  }

  @Test
  void onFailure_사이드이펙트_Failure() {
    List<Integer> captured = new ArrayList<>();
    Result<String, Integer> result = Result.failure(42);

    result.onFailure(captured::add);

    assertThat(captured).containsExactly(42);
  }

  @Test
  void onFailure_사이드이펙트_Success_무시() {
    List<Integer> captured = new ArrayList<>();
    Result<String, Integer> result = Result.success("ok");

    result.onFailure(captured::add);

    assertThat(captured).isEmpty();
  }
}
