package io.github.ppzxc.boilerplate.identity.configuration;

import io.github.ppzxc.boilerplate.identity.adapter.output.persist.UserPersistenceAdapter;
import io.github.ppzxc.boilerplate.identity.adapter.output.persist.UserQueryAdapter;
import io.github.ppzxc.boilerplate.identity.application.port.in.DeactivateUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.FindUserByIdUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.RegisterUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.port.in.SuspendUserUseCase;
import io.github.ppzxc.boilerplate.identity.application.service.DeactivateUserService;
import io.github.ppzxc.boilerplate.identity.application.service.FindUserByIdService;
import io.github.ppzxc.boilerplate.identity.application.service.RegisterUserService;
import io.github.ppzxc.boilerplate.identity.application.service.SuspendUserService;
import java.time.Clock;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
class IdentityBeanConfiguration {

  @Bean
  @Primary
  Clock identityClock() {
    return Clock.systemUTC();
  }

  // --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---

  @Bean
  RegisterUserUseCase registerUserUseCase(
      UserPersistenceAdapter adapter, Clock identityClock, PlatformTransactionManager txManager) {
    var service = new RegisterUserService(adapter, adapter, identityClock);
    return createTxProxy(service, RegisterUserUseCase.class, txManager);
  }

  @Bean
  FindUserByIdUseCase findUserByIdUseCase(
      UserQueryAdapter queryAdapter, PlatformTransactionManager txManager) {
    var service = new FindUserByIdService(queryAdapter);
    return createTxProxy(service, FindUserByIdUseCase.class, txManager);
  }

  @Bean
  SuspendUserUseCase suspendUserUseCase(
      UserPersistenceAdapter adapter, Clock identityClock, PlatformTransactionManager txManager) {
    var service = new SuspendUserService(adapter, adapter, identityClock);
    return createTxProxy(service, SuspendUserUseCase.class, txManager);
  }

  @Bean
  DeactivateUserUseCase deactivateUserUseCase(
      UserPersistenceAdapter adapter, Clock identityClock, PlatformTransactionManager txManager) {
    var service = new DeactivateUserService(adapter, adapter, identityClock);
    return createTxProxy(service, DeactivateUserUseCase.class, txManager);
  }

  private <T> T createTxProxy(Object target, Class<T> iface, PlatformTransactionManager txManager) {
    var source = new MatchAlwaysTransactionAttributeSource();
    var interceptor = new TransactionInterceptor(txManager, source);
    var factory = new ProxyFactory(target);
    factory.addInterface(iface);
    factory.addAdvice(interceptor);
    return iface.cast(factory.getProxy());
  }
}
