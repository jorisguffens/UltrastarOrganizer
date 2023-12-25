package be.jorisg.ultrastarorganizer.commands.media;

import be.jorisg.ultrastarorganizer.commands.media.download.MediaDownloadCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "media",
        description = "Parent command for media tools.",
        subcommands = {
                CommandLine.HelpCommand.class,
                MediaDownloadCommand.class
        })
public class MediaCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

}
