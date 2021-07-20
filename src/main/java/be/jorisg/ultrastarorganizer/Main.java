package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.commands.Minimize;
import be.jorisg.ultrastarorganizer.commands.PurgeCaches;
import be.jorisg.ultrastarorganizer.commands.Reformat;
import be.jorisg.ultrastarorganizer.commands.SongList;
import picocli.CommandLine;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if ( args.length < 2 ) {
            System.out.println("Usage: <command> <directory>");
            System.out.println();
            System.out.println("COMMANDS");
            System.out.println("  purgecaches\t\tPurge all cache files inside a library.");
            System.out.println("  reformat\t\tReformat song directories and update info files.");
            System.out.println("  minimize\t\tMinimize library by removing video files and background images.");
            System.out.println("  songlist\t\tGenerate a document with a list of all songs.");
            System.out.println();
            System.out.println("OPTIONS");
            System.out.println("  <directory>\t\tUltrastar library directory.");
            return;
        }

        String cmd = args[0];
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        int exitCode = 0;
        if ( cmd.equalsIgnoreCase("purgecaches") ) {
            exitCode = new CommandLine(new PurgeCaches()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("reformat") ) {
            exitCode = new CommandLine(new Reformat()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("minimize") ) {
            exitCode = new CommandLine(new Minimize()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("songlist") ) {
            exitCode = new CommandLine(new SongList()).execute(cmdArgs);
        }

        System.exit(exitCode);
    }

}
