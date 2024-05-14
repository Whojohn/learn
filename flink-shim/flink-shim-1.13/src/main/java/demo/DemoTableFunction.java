package demo;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.functions.TableFunction;

public class DemoTableFunction extends TableFunction<RowData> {
    public void eval(Object... keys) {
        collect(new GenericRowData(3));
    }
}
