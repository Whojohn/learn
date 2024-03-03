package parse;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlInOperator;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.flink.sql.parser.ddl.SqlCreateTable;
import org.apache.flink.sql.parser.ddl.SqlCreateView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Inspire by project as follow:
 * <a href="https://github.com/Qihoo360/Quicksql/blob/master/core/src/main/java/com/qihoo/qsql/plan/TableNameCollector.java">...</a>
 *
 * <p>fix those problem</>
 * 1. support flink sql dialect
 * 2. fix view fetch problem
 */
public class VisitorTableName extends SqlBasicVisitor<Set<String>> {

    private final Set<String> tableNames = new HashSet<>();

    private Boolean collect = false;

    @Override
    public Set<String> visit(SqlLiteral literal) {
        return tableNames;
    }

    private void wrapperCollector(Consumer<SqlCall> consumer, SqlCall sqlcall) {
        boolean temp = this.collect;
        this.collect = true;
        consumer.accept(sqlcall);
        this.collect = temp;
    }

    @Override
    public Set<String> visit(SqlCall sqlcall) {
        if (sqlcall instanceof SqlInsert) {
            SqlInsert sqlInsert = (SqlInsert) sqlcall;
            wrapperCollector(e -> (sqlInsert).getTargetTable().accept(this), sqlcall);
            (sqlInsert).getSource().accept(this);
        } else if (sqlcall instanceof SqlTableRef) {
            this.visit(sqlcall);
        } else if (sqlcall instanceof SqlCreateView) {
            SqlCreateView sqlCreateView = (SqlCreateView) sqlcall;
            wrapperCollector(e -> (sqlCreateView).getViewName().accept(this), sqlcall);
            (sqlCreateView).getQuery().accept(this);
        } else if (sqlcall instanceof SqlCreateTable) {
            wrapperCollector(e -> ((SqlCreateTable) sqlcall).getTableName().accept(this), sqlcall);
        } else if (sqlcall instanceof SqlSelect) {
            boolean temp = this.collect;
            this.collect = false;
            ((SqlSelect) sqlcall).getSelectList().accept(this);
            if (((SqlSelect) sqlcall).getFrom() != null) {
                this.collect = true;
                ((SqlSelect) sqlcall).getFrom().accept(this);

                this.collect = false;
                if (((SqlSelect) sqlcall).getWhere() != null) {
                    ((SqlBasicCall) ((SqlSelect) sqlcall).getWhere()).getOperandList().forEach(e -> e.accept(this));

                }
            }
            this.collect = temp;
        } else if (sqlcall instanceof SqlCase) {
            ((SqlCase) sqlcall).getWhenOperands().accept(this);
            ((SqlCase) sqlcall).getThenOperands().accept(this);
            ((SqlCase) sqlcall).getElseOperand().accept(this);
        } else if (sqlcall instanceof SqlWithItem) {
            ((SqlWithItem) sqlcall).name.accept(this);
            ((SqlWithItem) sqlcall).query.accept(this);
        } else if (sqlcall instanceof SqlWith) {
            ((SqlWith) sqlcall).withList.accept(this);
            ((SqlWith) sqlcall).body.accept(this);
        } else if (sqlcall instanceof SqlJoin) {
            ((SqlJoin) sqlcall).getLeft().accept(this);
            ((SqlJoin) sqlcall).getRight().accept(this);
        } else if (sqlcall instanceof SqlBasicCall) {
            visitBasiccall((SqlBasicCall) sqlcall);
        } else if (sqlcall instanceof SqlOrderBy) {
            ((SqlOrderBy) sqlcall).query.accept(this);
        } else if (sqlcall instanceof SqlSnapshot) {
            ((SqlSnapshot) sqlcall).getTableRef().accept(this);
        } else {
            sqlcall.getOperator().acceptCall(this, sqlcall);
        }
        return tableNames;
    }

    @Override
    public Set<String> visit(SqlNodeList sqlNodeList) {
        sqlNodeList.forEach(e -> e.accept(this));
        return tableNames;
    }

    public Set<String> visit(SqlTableRef sqlTableRef) {
        sqlTableRef.getOperandList().forEach(e -> e.accept(this));
        return tableNames;
    }

    @Override
    public Set<String> visit(SqlIdentifier sqlIdentifier) {
        if (this.collect) {
            tableNames.add(sqlIdentifier.toString());
        }
        return tableNames;
    }

    @Override
    public Set<String> visit(SqlDataTypeSpec sqlDataTypeSpec) {
        return tableNames;
    }

    @Override
    public Set<String> visit(SqlDynamicParam sqlDynamicParam) {
        return tableNames;
    }

    @Override
    public Set<String> visit(SqlIntervalQualifier sqlIntervalqualifier) {
        return tableNames;
    }

    private void visitBasiccall(SqlBasicCall sqlcall) {
        if (sqlcall.getOperator() instanceof SqlInOperator) {
            sqlcall.operands[1].accept(this);
        } else if (sqlcall.getOperator() instanceof SqlAsOperator) {
            (sqlcall).operands[0].accept(this);
            return;
        }
        Arrays.stream((sqlcall).operands).forEach(e -> e.accept(this));
    }
}


