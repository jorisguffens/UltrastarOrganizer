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

package be.jorisg.ultrastarorganizer.entity;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class SongFiles {

    private File mp3;
    private File video;
    private File background;
    private File cover;

    public SongFiles(File mp3, File video, File background, File cover) {
        this.mp3 = mp3;
        this.video = video;
        this.background = background;
        this.cover = cover;
    }

    public File getMP3() {
        return mp3;
    }

    public File getVideo() {
        return video;
    }

    public File getBackground() {
        return background;
    }

    public File getCover() {
        return cover;
    }

    public void setMP3(File mp3) {
        this.mp3 = mp3;
    }

    public void setVideo(File video) {
        this.video = video;
    }

    public void setBackground(File background) {
        this.background = background;
    }

    public void setCover(File cover) {
        this.cover = cover;
    }

    public void renameAll(String name) {
        if ( mp3 != null ) {
            setMP3(rename(mp3, name));
        }
        if ( video != null ) {
            setVideo(rename(video, name));
        }
        if ( background != null ) {
            setBackground(rename(background, name + " [BG]"));
        }
        if ( cover != null ) {
            setCover(rename(cover, name + " [CO]"));
        }
    }

    private File rename(File file, String name) {
        String ext = FilenameUtils.getExtension(file.getName());
        File dest = new File(file.getParent(), name + "." + ext);
        file.renameTo(dest);
        return dest;
    }

    public void apply(SongInfo info) {
        info.setMP3(mp3);
        info.setVideo(video);
        info.setBackground(background);
        info.setCover(cover);
    }
}
