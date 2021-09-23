import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

public class HdfsMain {
    FileSystem fileSystem;
    AtomicInteger judge = new AtomicInteger(-1);


    String document = "this is a test/n";
    Configuration configuration;

    String testFileDirName = "/paralli/";
    String renameFileDirName = "/paralli2/";
    String testFileName = "test";
    String renameFileName = "test2";

    String testFilePath = testFileDirName + testFileName;
    String renameFilePath = renameFileDirName + renameFileName;

    /**
     * 初始化 FileSystem 实例
     *
     * @param resourcePath core-site hdfs-site 文件存放路径
     * @throws URISyntaxException   路径异常
     * @throws IOException          io异常
     * @throws InterruptedException 中断异常
     */
    public HdfsMain(String resourcePath) throws URISyntaxException, IOException, InterruptedException {
        this.configuration = new Configuration() {{
            addResource(resourcePath);
        }};
        this.fileSystem = FileSystem.get(configuration);
    }


}

class DoAppend extends HdfsMain implements Runnable {


    /**
     * @param resourcePath
     */
    public DoAppend(String resourcePath) throws URISyntaxException, IOException, InterruptedException {
        super(resourcePath);
    }


    @Override
    /**
     * hdfs 在指定文件中追加数据
     */
    public void run() {
        OutputStream outputStream = null;
        try {
            outputStream = this.fileSystem.append(new Path(this.testFilePath));
            for (int a = 0; a < 10000; a++) {
                this.judge.addAndGet(1);
                Thread.sleep(4);
                outputStream.write(this.document.getBytes());
            }
            outputStream.flush();
            outputStream.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class DoRead extends HdfsMain implements Runnable {
    /**
     * @param resourcePath
     */
    public DoRead(String resourcePath) throws URISyntaxException, IOException, InterruptedException {
        super(resourcePath);
    }

    @Override
    public void run() {
        try {
            if (this.judge.get() <= 0) Thread.sleep(5000);
            InputStream inputStream = this.fileSystem.open(new Path(this.testFilePath));
            for (int a = 0; a < 10; a++) {
                Thread.sleep(1000);
                BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream));
                String temp = bf.readLine();
                if (temp != null) System.out.println(temp.length());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class DoChangeFileName extends HdfsMain implements Runnable {

    /**
     * @param resourcePath
     */
    public DoChangeFileName(String resourcePath) throws URISyntaxException, IOException, InterruptedException {
        super(resourcePath);
    }

    @Override
    public void run() {
        try {
            for (int a = 0; a < 10; a++) {
                if (this.judge.get() <= 0) Thread.sleep(3000);
                this.fileSystem.rename(new Path(this.testFileDirName), new Path(this.renameFileDirName));
                this.fileSystem.rename(new Path(this.renameFileDirName + this.testFileName), new Path(this.renameFileDirName + this.renameFileName));
                Thread.sleep(2000);
                this.fileSystem.rename(new Path(this.renameFileDirName + this.renameFileName), new Path(this.testFileDirName + this.testFileName));
                this.fileSystem.rename(new Path(this.renameFileDirName), new Path(this.testFileDirName));
            }

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }
}
