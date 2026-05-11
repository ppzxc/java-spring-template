package io.github.ppzxc.boilerplate.boot.observability;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
class DatabaseHealthIndicator implements HealthIndicator {

  private final DataSource dataSource;

  DatabaseHealthIndicator(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Health health() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("SELECT 1");
      return Health.up().build();
    } catch (Exception e) {
      return Health.down(e).build();
    }
  }
}
