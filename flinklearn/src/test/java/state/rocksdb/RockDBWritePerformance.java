package state.rocksdb;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.contrib.streaming.state.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rocksdb.*;
import utils.TestHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import static org.apache.flink.contrib.streaming.state.RocksDBConfigurableOptions.WRITE_BATCH_SIZE;

public class RockDBWritePerformance {

    private final String unCompressRandomData = "/../randomfile";

    @BeforeClass
    public static void ensureRocksDbNativeLibraryLoaded() throws IOException, URISyntaxException {
        NativeLibraryLoader.getInstance().loadLibrary(new TestHelper().getTmpFile());
    }

    @Test
    public void initailFile() throws Exception {
        try (TestHelper helper = new TestHelper() {{
            this.setClean(false);
        }}

        ) {
            ConcurrentLinkedQueue<String> temp = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < 2000000; i++) {
                if (i % 100000 == 0) {
                    IntStream.range(0, 100000).parallel().forEach(e -> temp.add(("value:" + RandomStringUtils.random(2048,
                            true, true))));
                    FileUtils.writeLines(new File(helper.getTmpFile() + unCompressRandomData),
                            Lists.newArrayList(temp.iterator()), true);
                    temp.clear();
                }
            }
        }
    }

    @Test
    public void basicTest() throws Exception {
        long start = System.currentTimeMillis();
        // 传入空数组默认会创建一个 cf , 这里测试代码相当于使用默认 cf
        ArrayList<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>(1);
        try (
                RocksDBResourceContainer container = new RocksDBResourceContainer(PredefinedOptions.DEFAULT,
                        new MyOptions());
                WriteOptions options = container.getWriteOptions();

                TestHelper helper = new TestHelper() {{
                    this.setClean(true);
                }};
                RocksDB db =
                        RocksDBOperationUtils.openDB(
                                helper.getTmpFile(),
                                Collections.emptyList(),
                                columnFamilyHandles,
                                container.getColumnOptions(),
                                container.getDbOptions());
                ColumnFamilyHandle handle = columnFamilyHandles.remove(0);
                RocksDBWriteBatchWrapper writeBatchWrapper = new RocksDBWriteBatchWrapper(db, options,
                        WRITE_BATCH_SIZE.defaultValue().getBytes());
                BufferedReader bf =
                        new BufferedReader(new FileReader(new File(helper.getTmpFile() + unCompressRandomData)))
        ) {
            long i = 0;
            String line = bf.readLine();
            while (line != null) {
                i += 1;
                for (int nested = 0; nested < 10; nested++) {
                    writeBatchWrapper.put(handle, ("key" + i * nested).getBytes(), line.getBytes());
                }
                line = bf.readLine();
            }
            writeBatchWrapper.flush();
        }
        System.out.println("cost time   :" + (System.currentTimeMillis() - start));
    }


    class MyOptions implements RocksDBOptionsFactory {
        @Override
        public DBOptions createDBOptions(DBOptions currentOptions, Collection<AutoCloseable> handlesToClose) {
            currentOptions.setInfoLogLevel(InfoLogLevel.DEBUG_LEVEL);
            currentOptions.setIncreaseParallelism(6);
            currentOptions.setMaxBackgroundFlushes(3);
//            currentOptions.setAllowConcurrentMemtableWrite(true);
//            currentOptions.setEnableWriteThreadAdaptiveYield(true);
            return currentOptions;
        }

        @Override
        public ColumnFamilyOptions createColumnOptions(ColumnFamilyOptions currentOptions,
                                                       Collection<AutoCloseable> handlesToClose) {
            List<CompressionType> compressionTypeList =
                    new ArrayList<>();
            try (final Options options = new Options()) {
                for (int i = 0; i < options.numLevels(); i++) {
                    if (i < 2) {
                        compressionTypeList.add(CompressionType.NO_COMPRESSION);
                    } else {
                        compressionTypeList.add(CompressionType.LZ4_COMPRESSION);
                    }
                }

            }
            currentOptions.setMinWriteBufferNumberToMerge(3).setMaxWriteBufferNumber(4);

            currentOptions = currentOptions.setCompressionPerLevel(compressionTypeList);
            return currentOptions;
        }

        @Override
        public RocksDBNativeMetricOptions createNativeMetricsOptions(RocksDBNativeMetricOptions nativeMetricOptions) {
            return RocksDBOptionsFactory.super.createNativeMetricsOptions(nativeMetricOptions);
        }
    }

}




