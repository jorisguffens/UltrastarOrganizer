package be.jorisg.ultrastarorganizer.commands.coverart;

import be.jorisg.ultrastarorganizer.commands.coverart.download.CoverArtDownloadCommand;
import be.jorisg.ultrastarorganizer.commands.coverart.extract.CoverArtExtractCommand;
import picocli.CommandLine;

@CommandLine.Command(
        name = "coverart",
        description = "Parent command for cover art related tools.",
        subcommands = {
                CommandLine.HelpCommand.class,
                CoverArtDownloadCommand.class,
                CoverArtExtractCommand.class
        })
public class CoverArtCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
