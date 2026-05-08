package io.github.ppzxc.boilerplate.notification.configuration;

import io.github.ppzxc.boilerplate.notification.adapter.output.persist.NotificationPersistenceAdapter;
import io.github.ppzxc.boilerplate.notification.application.port.in.SendUserRegisteredNotificationUseCase;
import io.github.ppzxc.boilerplate.notification.application.service.SendUserRegisteredNotificationService;
import java.time.Clock;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
class NotificationBeanConfiguration {

  @Bean
  Clock notificationClock() {
    return Clock.systemUTC();
  }

  // --- AI_ANCHOR: ADD_NEW_USECASE_BEANS_HERE ---

  @Bean
  SendUserRegisteredNotificationUseCase sendUserRegisteredNotificationUseCase(
      NotificationPersistenceAdapter adapter,
      Clock notificationClock,
      PlatformTransactionManager txManager) {
    var service = new SendUserRegisteredNotificationService(adapter, notificationClock);
    return createTxProxy(service, SendUserRegisteredNotificationUseCase.class, txManager);
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
