package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "purgecaches",
        description = "Remove all cache files from ultrastar inside a library.")
public class PurgeCaches implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @Override
    public Integer call() throws Exception {
        for (File songDir : directory.listFiles()) {
            if (!songDir.isDirectory()) {
                continue;
            }

            boolean cleaned = false;

            // delete cache files
            List<File> cacheFiles = Utils.getFilesByExtensions(songDir, "db", "sco", "ini");
            for (File file : cacheFiles) {
                file.delete();
                cleaned = true;
            }

            // delete wdmc directory
            File wdmc = new File(songDir, ".wdmc");
            if (wdmc.exists()) {
                FileUtils.deleteDirectory(wdmc);
                cleaned = true;
            }

            if (cleaned) {
                System.out.println("Cleaned up " + songDir.getName());
            }
        }

        return 0;
    }
}
