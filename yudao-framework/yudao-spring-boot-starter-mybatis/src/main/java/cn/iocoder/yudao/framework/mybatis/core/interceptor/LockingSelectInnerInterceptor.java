package cn.iocoder.yudao.framework.mybatis.core.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.ForMode;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.regex.Pattern;

/**
 * JSqlParser 4.9 emits row locks before ORDER BY/LIMIT, which MySQL rejects.
 * Run after SQL-changing plugins and serialize each SELECT's lock at its end.
 * The AST visitor keeps nested locks, bound parameters and tenant predicates intact.
 */
public class LockingSelectInnerInterceptor implements InnerInterceptor {

    private static final Pattern ROW_LOCK = Pattern.compile("\\bFOR\\s+(UPDATE|SHARE)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        PluginUtils.mpBoundSql(boundSql).sql(rewrite(boundSql.getSql()));
    }

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer timeout) {
        PluginUtils.MPBoundSql boundSql = PluginUtils.mpStatementHandler(statementHandler).mPBoundSql();
        boundSql.sql(rewrite(boundSql.sql()));
    }

    public static String rewrite(String sql) {
        if (!ROW_LOCK.matcher(sql).find()) {
            return sql;
        }
        Statement statement;
        try {
            statement = JsqlParserGlobal.parse(sql);
        } catch (net.sf.jsqlparser.JSQLParserException e) {
            throw new IllegalStateException("Cannot serialize locking SQL", e);
        }
        StringBuilder buffer = new StringBuilder();
        ExpressionDeParser expression = new ExpressionDeParser();
        SelectDeParser select = new SelectDeParser(expression, buffer) {
            @Override
            public void visit(PlainSelect plain) {
                ForMode mode = plain.getForMode();
                if (mode == null) {
                    super.visit(plain);
                    return;
                }
                // Only suppress this SELECT's lock; nested SELECTs use the same visitor.
                plain.setForMode(null);
                try {
                    super.visit(plain);
                } finally {
                    plain.setForMode(mode);
                }
                buffer.append(" FOR ").append(mode.getValue());
                if (plain.getForUpdateTable() != null) {
                    buffer.append(" OF ").append(plain.getForUpdateTable());
                }
                if (plain.getWait() != null) {
                    buffer.append(plain.getWait());
                }
                if (plain.isNoWait()) {
                    buffer.append(" NOWAIT");
                } else if (plain.isSkipLocked()) {
                    buffer.append(" SKIP LOCKED");
                }
            }
        };
        expression.setSelectVisitor(select);
        expression.setBuffer(buffer);
        statement.accept(new StatementDeParser(expression, select, buffer));
        return buffer.toString();
    }
}
