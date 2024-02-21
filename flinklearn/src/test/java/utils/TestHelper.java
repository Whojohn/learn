package utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHelper implements AutoCloseable {
    private String path;
    private Boolean clean = true;

    public String getTmpFile() throws URISyntaxException {
        Path filePath =
                Paths.get(TestHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve("../." +
                        "./tmp");
        File file = new File(filePath.toString());
        if (!file.exists()) {
            file.mkdir();
        }
        this.path = file.getAbsolutePath();
        return this.path;
    }

    @Override
    public void close() throws Exception {
        if (clean) {
            File file = new File(this.path);
            FileUtils.deleteDirectory(file);
        }
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }
}
