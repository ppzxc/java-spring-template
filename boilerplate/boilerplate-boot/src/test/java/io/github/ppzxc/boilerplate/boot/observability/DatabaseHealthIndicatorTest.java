package io.github.ppzxc.boilerplate.boot.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseHealthIndicatorTest {

  @Test
  void db_정상_연결_UP() throws Exception {
    var dataSource = mock(DataSource.class);
    var conn = mock(Connection.class);
    var stmt = mock(Statement.class);
    when(dataSource.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);
    when(stmt.execute("SELECT 1")).thenReturn(true);

    var health = new DatabaseHealthIndicator(dataSource).health();

    assertThat(health.getStatus().getCode()).isEqualTo("UP");
  }

  @Test
  void db_연결_실패_DOWN() throws Exception {
    var dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));

    var health = new DatabaseHealthIndicator(dataSource).health();

    assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
  }
}
