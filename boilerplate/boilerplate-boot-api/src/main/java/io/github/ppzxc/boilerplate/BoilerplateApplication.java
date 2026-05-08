package io.github.ppzxc.boilerplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/** Spring Boot API 애플리케이션 진입점. */
@Modulithic(systemName = "Boilerplate", sharedModules = "shared")
@SpringBootApplication
public class BoilerplateApplication {

  public static void main(String[] args) {
    SpringApplication.run(BoilerplateApplication.class, args);
  }
}
