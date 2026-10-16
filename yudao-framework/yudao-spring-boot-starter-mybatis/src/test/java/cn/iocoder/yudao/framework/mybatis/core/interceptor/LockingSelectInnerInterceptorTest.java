package cn.iocoder.yudao.framework.mybatis.core.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockingSelectInnerInterceptorTest {

    private final TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(new TenantLineHandler() {
        @Override
        public Expression getTenantId() {
            return new LongValue(1);
        }
    });

    @Test
    void orderedLockRetainsTenantAndLimit() {
        String sql = tenant.parserSingle("SELECT id FROM hrm_salary_option WHERE code = ? ORDER BY id DESC LIMIT 1 FOR UPDATE", null);
        String fixed = LockingSelectInnerInterceptor.rewrite(sql);
        assertEquals("SELECT id FROM hrm_salary_option WHERE code = ? AND tenant_id = 1 ORDER BY id DESC LIMIT 1 FOR UPDATE", fixed);
        assertEquals(fixed, LockingSelectInnerInterceptor.rewrite(fixed));
    }

    @Test
    void catalogLockRetainsOrder() {
        String sql = tenant.parserSingle("SELECT id FROM hrm_salary_option ORDER BY id ASC FOR UPDATE", null);
        assertEquals("SELECT id FROM hrm_salary_option WHERE tenant_id = 1 ORDER BY id ASC FOR UPDATE", LockingSelectInnerInterceptor.rewrite(sql));
    }

    @Test
    void nestedLocksAndSkipLockedStayInTheirOwnSelect() {
        String fixed = LockingSelectInnerInterceptor.rewrite("SELECT id FROM (SELECT id FROM t ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED) x ORDER BY id LIMIT 2 FOR UPDATE NOWAIT");
        assertEquals("SELECT id FROM (SELECT id FROM t ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED) x ORDER BY id LIMIT 2 FOR UPDATE NOWAIT", fixed);
    }

    @Test
    void literalAndParametersArePreserved() {
        String sql = "SELECT 'FOR UPDATE ORDER BY secret' AS label, id FROM t WHERE name = ? ORDER BY id FOR UPDATE";
        assertEquals(sql, LockingSelectInnerInterceptor.rewrite(sql));
        assertEquals("SELECT id FROM t WHERE name = ?", LockingSelectInnerInterceptor.rewrite("SELECT id FROM t WHERE name = ?"));
    }
}
