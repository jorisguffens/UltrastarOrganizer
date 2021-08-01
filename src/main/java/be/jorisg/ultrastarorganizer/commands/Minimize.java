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
import be.jorisg.ultrastarorganizer.exceptions.LibraryException;
import be.jorisg.ultrastarorganizer.utils.Utils;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import org.apache.commons.io.FilenameUtils;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "minimize",
        description = "Minimize library size by removing video files and background covers.")
public class Minimize implements Callable<Integer> {

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

    private void process(File songDir) throws LibraryException {
        List<SongInfo> infoFiles = Utils.getInfoFiles(songDir);
        SongInfo main = Utils.getMainInfoFile(infoFiles);

        String filename = main.getFileName();
        boolean hasAudio = main.getMP3() != null;

        // get video files
        File mainVideo = main.getVideo();
        List<File> videoFiles = Utils.getFilesByExtensions(songDir, "mp4", "avi", "mkv", "flv", "mov", "mpg", "m4v", "divx");
        if ( mainVideo == null && !videoFiles.isEmpty() ) {
            mainVideo = videoFiles.get(0);
        }

        // convert video to mp3
        if ( !hasAudio && mainVideo != null ) {
            File target = new File(songDir, filename + ".mp3");
            convertVideoToMP3(mainVideo, target);
        }

        // delete video files
        for ( File vf : videoFiles ) {
            vf.delete();
        }

        // get image files
        List<File> imageFiles = Utils.getFilesByExtensions(songDir, "jpg", "png", "jpeg").stream()
                .filter(Utils::validateImage).collect(Collectors.toList());

        // delete all image files that are not the cover
        for ( File f : imageFiles ) {
            if ( f.equals(main.getCover()) ) {
                continue;
            }

            // convert background to cover if cover is missing but background is not
            if ( main.getCover() == null && f.equals(main.getBackground()) ) {
                String ext = FilenameUtils.getExtension(f.getName());
                File dest = new File(songDir, main.getFileName() + " [CO]." + ext);
                f.renameTo(dest);

                main.setCover(dest);
                main.setBackground(null);
                main.save();
                continue;
            }

            f.delete();
        }
    }

    private void convertVideoToMP3(File source, File target) {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(192000);
        audio.setChannels(2);
        audio.setSamplingRate(44100);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("mp3");
        attrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder();
        try {
            encoder.encode(source, target, attrs);
        } catch (IllegalArgumentException | EncoderException e) {
            e.printStackTrace();
        }
    }

}
