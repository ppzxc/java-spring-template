package io.github.ppzxc.boilerplate.identity.adapter.input.api;

import io.github.ppzxc.boilerplate.identity.adapter.input.api.dto.UserDetailResponse;
import io.github.ppzxc.boilerplate.identity.adapter.input.api.dto.UserRegistrationRequest;
import io.github.ppzxc.boilerplate.identity.application.dto.DeactivateUserCommand;
import io.github.ppzxc.boilerplate.identity.application.dto.FindUserByIdQuery;
import io.github.ppzxc.boilerplate.identity.application.dto.RegisterUserCommand;
import io.github.ppzxc.boilerplate.identity.application.dto.SuspendUserCommand;
import io.github.ppzxc.boilerplate.identity.application.port.in.DeactivateUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.FindUserByIdUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.RegisterUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.SuspendUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.out.OptimisticLockException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Identity BC REST Controller (AD-1 — Input Port에만 의존). */
@RestController
@RequestMapping("/api/identity/users")
public class UserController {

  private final RegisterUserUseCase registerUserUseCase;
  private final FindUserByIdUseCase findUserByIdUseCase;
  private final SuspendUserUseCase suspendUserUseCase;
  private final DeactivateUserUseCase deactivateUserUseCase;

  public UserController(
      RegisterUserUseCase registerUserUseCase,
      FindUserByIdUseCase findUserByIdUseCase,
      SuspendUserUseCase suspendUserUseCase,
      DeactivateUserUseCase deactivateUserUseCase) {
    this.registerUserUseCase = registerUserUseCase;
    this.findUserByIdUseCase = findUserByIdUseCase;
    this.suspendUserUseCase = suspendUserUseCase;
    this.deactivateUserUseCase = deactivateUserUseCase;
  }

  @PostMapping
  public ResponseEntity<UserDetailResponse> register(
      @Valid @RequestBody UserRegistrationRequest request) {
    var result =
        registerUserUseCase.execute(
            new RegisterUserCommand(request.userName(), request.email(), request.password()));
    var response = UserDetailResponse.from(result);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).header("ETag", response.etag()).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDetailResponse> findById(@PathVariable String id) {
    var summary = findUserByIdUseCase.execute(new FindUserByIdQuery(id));
    var response = UserDetailResponse.from(summary);
    return ResponseEntity.ok().header("ETag", response.etag()).body(response);
  }

  @PostMapping("/{id}:suspend")
  public ResponseEntity<Void> suspend(
      @PathVariable String id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch) {
    requireIfMatch(id, ifMatch);
    suspendUserUseCase.execute(new SuspendUserCommand(id));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}:deactivate")
  public ResponseEntity<Void> deactivate(
      @PathVariable String id,
      @RequestHeader(value = "If-Match", required = false) String ifMatch) {
    requireIfMatch(id, ifMatch);
    deactivateUserUseCase.execute(new DeactivateUserCommand(id));
    return ResponseEntity.noContent().build();
  }

  private void requireIfMatch(String entityId, String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
      throw new OptimisticLockException(entityId);
    }
  }
}
