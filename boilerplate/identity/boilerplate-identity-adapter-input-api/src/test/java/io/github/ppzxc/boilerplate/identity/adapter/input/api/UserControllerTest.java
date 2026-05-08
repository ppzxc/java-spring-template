package io.github.ppzxc.boilerplate.identity.adapter.input.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.ppzxc.boilerplate.identity.application.dto.FindUserByIdQuery;
import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserResult;
import io.github.ppzxc.boilerplate.identity.application.dto.UserSummary;
import io.github.ppzxc.boilerplate.identity.application.port.in.DeactivateUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.FindUserByIdUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.RegisterUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.SuspendUserUseCase;
import io.github.ppzxc.boilerplate.identity.domain.exception.UserException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  private static final String API_VERSION = "2026-05-08";
  private static final String BASE_URL = "/api/identity/users";

  @Autowired MockMvc mockMvc;

  @MockitoBean RegisterUserUseCase registerUserUseCase;
  @MockitoBean FindUserByIdUseCase findUserByIdUseCase;
  @MockitoBean SuspendUserUseCase suspendUserUseCase;
  @MockitoBean DeactivateUserUseCase deactivateUserUseCase;

  @Test
  void 회원가입_201_Location_ETag() throws Exception {
    var userId = UUID.randomUUID().toString();
    var now = Instant.parse("2026-01-01T00:00:00Z");
    when(registerUserUseCase.execute(any()))
        .thenReturn(
            new RegisterUserResult(userId, "홍길동", "test@example.com", "ACTIVE", 0L, now, now));

    mockMvc
        .perform(
            post(BASE_URL)
                .header("Api-Version", API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userName":"홍길동","email":"test@example.com","password":"secure_pw"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(header().string("ETag", "v0"))
        .andExpect(header().exists("Request-Id"))
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.state").value("ACTIVE"))
        .andExpect(jsonPath("$.etag").value("v0"))
        .andExpect(jsonPath("$.userName").value("홍길동"));
  }

  @Test
  void 회원가입_422_검증실패_problemJson() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .header("Api-Version", API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userName\":\"\",\"email\":\"invalid-email\",\"password\":\"\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details").isArray());
  }

  @Test
  void 회원가입_409_이메일중복_problemJson() throws Exception {
    when(registerUserUseCase.execute(any()))
        .thenThrow(new UserException.AlreadyExistsException("test@example.com"));

    mockMvc
        .perform(
            post(BASE_URL)
                .header("Api-Version", API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userName":"홍길동","email":"test@example.com","password":"pw"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
  }

  @Test
  void 회원가입_400_ApiVersion헤더누락() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userName":"홍길동","email":"test@example.com","password":"pw"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("API_VERSION_REQUIRED"));
  }

  @Test
  void 단건조회_200_ETag() throws Exception {
    var userId = UUID.randomUUID().toString();
    var now = Instant.parse("2026-01-01T00:00:00Z");
    when(findUserByIdUseCase.execute(any(FindUserByIdQuery.class)))
        .thenReturn(new UserSummary(userId, "홍길동", "test@example.com", "ACTIVE", 2L, now, now));

    mockMvc
        .perform(get(BASE_URL + "/{id}", userId).header("Api-Version", API_VERSION))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(header().exists("Request-Id"))
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.state").value("ACTIVE"))
        .andExpect(jsonPath("$.etag").value("v2"));
  }

  @Test
  void 단건조회_404_존재하지않음() throws Exception {
    var userId = UUID.randomUUID().toString();
    when(findUserByIdUseCase.execute(any())).thenThrow(new UserException.NotFoundException(userId));

    mockMvc
        .perform(get(BASE_URL + "/{id}", userId).header("Api-Version", API_VERSION))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  void 정지_204_IfMatch있음() throws Exception {
    var userId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post(BASE_URL + "/{id}:suspend", userId)
                .header("Api-Version", API_VERSION)
                .header("If-Match", "v1"))
        .andExpect(status().isNoContent());
  }

  @Test
  void 정지_412_IfMatch없음() throws Exception {
    var userId = UUID.randomUUID().toString();

    mockMvc
        .perform(post(BASE_URL + "/{id}:suspend", userId).header("Api-Version", API_VERSION))
        .andExpect(status().isPreconditionFailed())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("ETAG_MISMATCH"));
  }

  @Test
  void 탈퇴_204_IfMatch있음() throws Exception {
    var userId = UUID.randomUUID().toString();

    mockMvc
        .perform(
            post(BASE_URL + "/{id}:deactivate", userId)
                .header("Api-Version", API_VERSION)
                .header("If-Match", "v1"))
        .andExpect(status().isNoContent());
  }

  @Test
  void 탈퇴_이미정지된사용자_422() throws Exception {
    var userId = UUID.randomUUID().toString();
    doThrow(new UserException.AlreadyDeactivatedException(userId))
        .when(deactivateUserUseCase)
        .execute(any());

    mockMvc
        .perform(
            post(BASE_URL + "/{id}:deactivate", userId)
                .header("Api-Version", API_VERSION)
                .header("If-Match", "v1"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("USER_ALREADY_DEACTIVATED"));
  }
}
