package parse;


import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.flink.sql.parser.impl.FlinkSqlParserImpl;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestVisitorTableName {

    static Set<String> testHelper(String sql) throws SqlParseException {
        SqlParser.Config sqlConfig = SqlParser.config()
                .withParserFactory(FlinkSqlParserImpl.FACTORY)
                .withQuoting(Quoting.DOUBLE_QUOTE)
                .withUnquotedCasing(Casing.UNCHANGED)
                .withQuotedCasing(Casing.UNCHANGED);
        VisitorTableName visitorTableName = new VisitorTableName();
        return visitorTableName.visit(SqlParser.create(sql, sqlConfig).parseStmtList());
    }


    @Test
    public void testSimple() throws SqlParseException {
        assert testHelper("select * from abc").equals(new HashSet<>(Collections.singletonList("abc")));
    }


    @Test
    public void testView() throws SqlParseException {

        assert testHelper("create view a as select * from abc").equals(new HashSet<>(Arrays.asList("a", "abc")));
    }
}
