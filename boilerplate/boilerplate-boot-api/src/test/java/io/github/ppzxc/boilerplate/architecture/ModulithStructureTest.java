package io.github.ppzxc.boilerplate.architecture;

import io.github.ppzxc.boilerplate.BoilerplateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

  static final ApplicationModules modules = ApplicationModules.of(BoilerplateApplication.class);

  @Test
  void 모듈_구조_검증() {
    modules.verify();
  }
}
