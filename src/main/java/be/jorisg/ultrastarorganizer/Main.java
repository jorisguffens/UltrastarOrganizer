/*
 * This file is part of Ultrastar Organizer, licensed under the MIT License.
 *
 * Copyright (c) Joris Guffens
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package be.jorisg.ultrastarorganizer;

import be.jorisg.ultrastarorganizer.commands.*;
import picocli.CommandLine;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        String path = Main.class.getClassLoader()
                .getResource("logging.properties").getFile();
        System.setProperty("java.util.logging.config.file", path);

        if ( args.length < 2 ) {
            sendHelp();
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
        else if ( cmd.equalsIgnoreCase("tracklist") ) {
            exitCode = new CommandLine(new Tracklist()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("automatch") ) {
            exitCode = new CommandLine(new Automatch()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("extractcovers") ) {
            exitCode = new CommandLine(new ExtractCovers()).execute(cmdArgs);
        }
        else if ( cmd.equalsIgnoreCase("downloadcovers") ) {
            exitCode = new CommandLine(new DownloadCovers()).execute(cmdArgs);
        }
        else {
            sendHelp();
        }

        System.exit(exitCode);
    }

    private static void sendHelp() {
        System.out.println("Usage: <command> <directory>");
        System.out.println();
        System.out.println("COMMANDS");
        System.out.println("  purgecaches\t\tPurge all cache files inside a library.");
        System.out.println("  reformat\t\tReformat song directories and update info files.");
        System.out.println("  minimize\t\tMinimize library by removing video files and background images.");
        System.out.println("  tracklist\t\tGenerate a document with a list of all songs.");
        System.out.println("  automatch\t\tMatch mp3 files with info files in the same root directory.");
        System.out.println("  extractcovers\t\tExtract cover files and bundle them into a single directory.");
        System.out.println("  downloadcovers\t\tDownload missing cover files.");
        System.out.println();
        System.out.println("OPTIONS");
        System.out.println("  <directory>\t\tUltrastar library directory.");
        System.out.println("  <size>\t\textractcovers image resize.");
        System.out.println("  <type>\t\ttracklist export type (csv, odt).");
    }

}
