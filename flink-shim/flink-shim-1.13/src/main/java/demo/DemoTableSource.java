package demo;

import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.TableFunctionProvider;

public class DemoTableSource implements LookupTableSource {

    public DemoTableSource(){

    }

    @Override
    public LookupRuntimeProvider getLookupRuntimeProvider(LookupContext context) {
        return TableFunctionProvider.of(new DemoTableFunction());
    }

    @Override
    public DynamicTableSource copy() {
        return new DemoTableSource();
    }

    @Override
    public String asSummaryString() {
        return "demo";
    }
}
