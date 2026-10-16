package cn.iocoder.yudao.framework.mybatis.core.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Run against an isolated MySQL schema, never against business data. */
@EnabledIfEnvironmentVariable(named = "HRM_MYSQL_TEST_URL", matches = ".+")
class LockingSelectMySqlTest {
    @Test
    void tenantOrderedQueriesExecuteAndHoldRowLocks() throws Exception {
        String url = System.getenv("HRM_MYSQL_TEST_URL");
        String user = System.getenv().getOrDefault("HRM_MYSQL_TEST_USER", "root");
        String password = System.getenv().getOrDefault("HRM_MYSQL_TEST_PASSWORD", "");
        String table = "lock_regression_" + UUID.randomUUID().toString().replace("-", "");
        TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override public Expression getTenantId() { return new LongValue(1); }
        });
        try (Connection first = DriverManager.getConnection(url, user, password);
             Connection second = DriverManager.getConnection(url, user, password)) {
            assertTrue(first.getCatalog().startsWith("hrm_"), "Requires isolated hrm_ schema");
            try (Statement setup = first.createStatement()) {
                setup.execute("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, amount INT NOT NULL) ENGINE=InnoDB");
            }
            try {
                try (Statement setup = first.createStatement()) {
                    setup.execute("INSERT INTO " + table + " VALUES (1,1,10),(2,2,20)");
                }
                first.setAutoCommit(false);
                String original = "SELECT id FROM " + table + " ORDER BY id DESC LIMIT 1 FOR UPDATE";
                String sql = LockingSelectInnerInterceptor.rewrite(tenant.parserSingle(original, null));
                try (Statement query = first.createStatement(); ResultSet result = query.executeQuery(sql)) {
                    assertTrue(result.next());
                    assertEquals(1, result.getLong(1));
                    assertFalse(result.next());
                }
                try (Statement concurrent = second.createStatement()) {
                    concurrent.execute("SET SESSION innodb_lock_wait_timeout=1");
                    // The row returned through the tenant pipeline must stay locked.
                    SQLException blocked = assertThrows(SQLException.class,
                            () -> concurrent.executeUpdate("UPDATE " + table + " SET amount=11 WHERE id=1"));
                    assertEquals(1205, blocked.getErrorCode());
                }
                first.rollback();
                try (Statement query = first.createStatement(); ResultSet result = query.executeQuery(
                        LockingSelectInnerInterceptor.rewrite(tenant.parserSingle("SELECT id FROM " + table + " ORDER BY id ASC FOR UPDATE", null)))) {
                    assertTrue(result.next());
                    assertEquals(1, result.getLong(1));
                    assertFalse(result.next());
                }
                first.rollback();
            } finally {
                first.rollback();
                first.setAutoCommit(true);
                try (Statement cleanup = first.createStatement()) { cleanup.execute("DROP TABLE " + table); }
            }
        }
    }
}
