package io.github.ppzxc.boilerplate.identity.application.port.out;

/** SavePort 구현체가 Optimistic Lock 실패 시 발생시키는 예외 (AD-7). */
public final class OptimisticLockException extends RuntimeException {

  private final String entityId;

  public OptimisticLockException(String entityId) {
    super("Optimistic lock conflict for entity: " + entityId);
    this.entityId = entityId;
  }

  public String entityId() {
    return entityId;
  }
}
