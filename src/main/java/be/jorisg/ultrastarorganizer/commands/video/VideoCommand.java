package be.jorisg.ultrastarorganizer.commands.video;

import be.jorisg.ultrastarorganizer.commands.video.download.VideoDownloadCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "video",
        description = "Parent command for video tools.",
        subcommands = {
                CommandLine.HelpCommand.class,
                VideoDownloadCommand.class
        })
public class VideoCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
