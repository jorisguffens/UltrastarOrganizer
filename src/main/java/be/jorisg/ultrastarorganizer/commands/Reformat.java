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

package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.entity.SongNoteCollection;
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "reformat",
        description = "Reformat song directory and update info files")
public class Reformat implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @Override
    public Integer call() {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File songDir = files[i];
            if ( !songDir.isDirectory() ) {
                continue;
            }

            try {
                System.out.println("Processing directory " + (i + 1) + " of " +
                        files.length + ": " + songDir.getName() + "");
                process(songDir);
            } catch (LibraryException ex) {
                System.out.println(ex.getMessage());
                if ( !songDir.getName().startsWith("[ERROR]") ) {
                    songDir.renameTo(new File(directory, "[ERROR] - " + songDir.getName()));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return 0;
    }

    public void process(File songDir) throws LibraryException, IOException {
        List<SongInfo> infoFiles = Utils.getInfoFiles(songDir);
        SongInfo main = Utils.getMainInfoFile(infoFiles);

        // find missing mp3 file
        if ( main.getMP3() == null ) {
            List<File> audioFiles = Utils.getFilesByExtensions(songDir, "mp3").stream()
                    .filter(Utils::validateMP3).collect(Collectors.toList());
            if ( audioFiles.size() > 1 ) {
                throw new LibraryException("Can't select correct audio file.");
            }

            if ( !audioFiles.isEmpty() ) {
                main.setMP3(audioFiles.get(0));
            }
        }

        // find missing video file
        if ( main.getVideo() == null ) {
            List<File> videoFiles = Utils.getFilesByExtensions(songDir, Utils.VIDEO_EXT);
            if ( !videoFiles.isEmpty() ) {
                main.setVideo(videoFiles.get(0));
            }
        }

        // find missing cover file
        if ( main.getCover() == null ) {
            List<File> imageFiles = Utils.getFilesByExtensions(songDir, Utils.IMAGE_EXT).stream()
                    .filter(Utils::validateImage).collect(Collectors.toList());

            for ( File f : imageFiles ) {
                if ( main.getBackground() != null && f.equals(main.getBackground())) {
                    continue;
                }

                main.setCover(f);
                break;
            }
        }

        // find missing background file
        if ( main.getBackground() == null ) {
            List<File> imageFiles = Utils.getFilesByExtensions(songDir, Utils.IMAGE_EXT).stream()
                    .filter(Utils::validateImage).collect(Collectors.toList());

            for ( File f : imageFiles ) {
                if ( f.equals(main.getCover()) ) {
                    continue;
                }

                main.setBackground(f);
                break;
            }
        }

        // fix negative lyrics start
        SongNoteCollection snc = new SongNoteCollection(main.getNotes());
        int beat = snc.getNotes().get(0).getBeat();
        if ( beat < 0 ) {
            snc.shift(Math.abs(beat));
            main.setNotes(snc.getLines());
        }

        // update all info files
        String filename = main.getBaseFileName();
        for ( SongInfo si : infoFiles ) {

            // set correct filenames in info files
            si.setMP3(rename(main.getMP3(), filename));
            si.setVideo(rename(main.getVideo(), filename));
            si.setCover(rename(main.getCover(), filename + " [CO]"));
            si.setBackground(rename(main.getBackground(), filename + " [BG]"));

            // save info files
            si.save();

            String fname = si.getBaseFileName();
            if ( si.isDuet() && !fname.toLowerCase().contains("duet") ) {
                fname += " (Duet)";
            }

            File target = new File(songDir, fname + ".txt");

            int i = 1;
            while (target.exists() && !si.getFile().equals(target)) {
                target = new File(songDir, fname + " (" + i + ").txt");
                i++;
            }
            si.renameTo(target);
        }

        // update directory filename
        if ( !songDir.getName().equals(filename) ) {
            renameDirectory(songDir, new File(songDir.getParent(), filename));
        }
    }

    private File rename(File file, String name) {
        if ( file == null ) {
            return null;
        }

        String ext = FilenameUtils.getExtension(file.getName());
        File dest = new File(file.getParent(), name + "." + ext);
        file.renameTo(dest);
        return dest;
    }

    private void renameDirectory(File source, File dest) throws IOException {
        File tmp = new File(dest.getParent(), "[TMP] " + dest.getName());
        FileUtils.copyDirectory(source, tmp);
        FileUtils.deleteDirectory(source);
        FileUtils.copyDirectory(tmp, dest);
        FileUtils.deleteDirectory(tmp);
    }
}
