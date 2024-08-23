package jvav.io;

public class File extends java.io.File {
    private final String path;
    public File(String path) {
        super(path);
        this.path = path;
    }

    @Override
    public String getAbsolutePath() {
        return path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
