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
        File dest = new File(file.getParent(), name + "" + ext);
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
