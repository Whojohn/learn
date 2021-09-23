import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestHdfs {

    private static HdfsMain ini() throws InterruptedException, IOException, URISyntaxException {
        System.setProperty("HADOOP_USER_NAME", "hadoop");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        HdfsMain hdfs = new HdfsMain("./resource/");
        return hdfs;
    }


    @Test
    /**
     * 并发修改正在写入的文件
     * 对于正在的文件，改名不会影响正在写入的文件
     */
    public void testRenameWhenWrite() throws Exception {
        HdfsMain hdfs = ini();
        OutputStream outputStream = hdfs.fileSystem.create(new Path("/paralli/test"));
        outputStream.close();
        Thread thread1 = new Thread(new DoAppend("./resource/"));
        Thread thread2 = new Thread(new DoChangeFileName("./resource/"));
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    @Test
    /**
     * 只有一个写入的情况下，多个读
     */
    public void testReadWhenWrite() throws Exception {
        HdfsMain hdfs = ini();
        OutputStream outputStream = hdfs.fileSystem.create(new Path("test1"));
        outputStream.close();

        Thread thread1 = new Thread(new DoAppend("./resource/"));
        Thread thread2 = new Thread(new DoRead("./resource/"));
        Thread thread3 = new Thread(new DoRead("./resource/"));

        thread1.start();
        thread2.start();
        thread3.start();
        thread1.join();
        thread2.join();
        thread3.join();
    }


    @Test
    /**
     * HDFS 并发写入同一文件时，会引发 lease 相关报错，只有一个线程能持有 lease 并正常写入 ，以保护资源。
     */
    public void multiAppend() throws Exception {
        HdfsMain hdfs = ini();
        OutputStream outputStream = hdfs.fileSystem.create(new Path("/paralli/test"));
        outputStream.close();
        Thread thread1 = new Thread(new DoAppend("./resource/"));
        Thread thread2 = new Thread(new DoAppend("./resource/"));
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

    }

    @Test
    /**
     *  HDFS 写入压缩样例
     */
    public void testCompress() throws InterruptedException, IOException, URISyntaxException {
        HdfsMain hdfs = ini();
        String document = new String(Files.readAllBytes(Paths.get("C:\\workbench\\learn\\hdfslearn\\src\\main\\java\\test")), StandardCharsets.UTF_8);
        hdfs.document = document;
        CompressionCodecFactory comfactroy = new CompressionCodecFactory(hdfs.configuration);
        CompressionCodec codec = comfactroy.getCodecByClassName("org.apache.hadoop.io.compress.GzipCodec");
        try {
            OutputStream outputStream = hdfs.fileSystem.create(new Path("/paralli/test3"));
            CompressionOutputStream compressionOutputStream = codec.createOutputStream(outputStream);
            for (int a = 0; a < 100; a++) {
                compressionOutputStream.write(hdfs.document.getBytes());
            }
            // 压缩完成必须调用 finish
            compressionOutputStream.finish();
            hdfs.fileSystem.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     *  HDFS 解压样例
     */
    public void testUnCompress() throws InterruptedException, IOException, URISyntaxException {
        HdfsMain hdfs = ini();
        CompressionCodecFactory comfactroy = new CompressionCodecFactory(hdfs.configuration);
        CompressionCodec codec = comfactroy.getCodecByClassName("org.apache.hadoop.io.compress.GzipCodec");
        CompressionInputStream compressionInputStream = codec.createInputStream(hdfs.fileSystem.open(new Path("/paralli/test")));
        OutputStream out = hdfs.fileSystem.create(new Path("/paralli/uncompress_test"));
        IOUtils.copyBytes(compressionInputStream, out, hdfs.configuration);
        out.flush();
        out.close();
        hdfs.fileSystem.close();
    }


}

