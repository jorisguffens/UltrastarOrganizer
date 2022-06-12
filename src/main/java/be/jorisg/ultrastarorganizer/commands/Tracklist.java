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
import be.jorisg.ultrastarorganizer.tracklist.TracklistType;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.xerces.dom.ParentNode;
import org.w3c.dom.Node;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "songlist",
        description = "Create a document with a list of all songs.")
public class Tracklist implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @CommandLine.Parameters(index = "1", description = "Output file type", type = TracklistType.class)
    private TracklistType type;

    @Override
    public Integer call() throws Exception {
        if ( type == null ) {
            System.out.println("Invalid file type.");
            return 1;
        }

        System.out.println("Looking for songs...");

        List<SongInfo> songs = new ArrayList<>();
        for (File songDir : directory.listFiles()) {
            if (!songDir.isDirectory()) {
                continue;
            }

            try {
                songs.addAll(Utils.getInfoFiles(songDir));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println(songs.size() + " songs will be processed.");

        File outputFile = new File(FileSystems.getDefault().getPath(".").toFile(), "tracklist." + type.name().toLowerCase());
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        type.generator().generate(outputFile, songs);

        System.out.println("Generated " + outputFile.getName());
        return 0;
    }

    private void clear(ParentNode parent) {
        Node childNode;
        while ((childNode = parent.getFirstChild()) != null) {
            parent.removeChild(childNode);
        }
    }
}
