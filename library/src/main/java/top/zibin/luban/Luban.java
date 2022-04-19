package top.zibin.luban;

import static top.zibin.luban.kt.Luban.DEFAULT_DISK_CACHE_DIR;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.InputStream;

import top.zibin.luban.kt.MemoryUnit;

public class Luban {

    public static Builder with(Context context) {
        return new Builder(context);
    }

    public static class Builder {

        private final top.zibin.luban.kt.Luban luban;

        Builder(Context context) {
            File dir = new File(context.getCacheDir(), DEFAULT_DISK_CACHE_DIR);
            InternalCompact.INSTANCE.ensureDir(dir);
            luban = new top.zibin.luban.kt.Luban(dir);
        }

        public Builder load(InputStream inputStream) {
            luban.input(inputStream, null, null, null, null, null, null);
            return this;
        }

        public Builder load(Context context, Uri uri) {
            luban.input(null, uri, null, null, null, null, context);
            return this;
        }

        public Builder load(String path) {
            luban.input(null, null, null, path, null, null, null);
            return this;
        }

        public Builder load(File file) {
            luban.input(null, null, file, null, null, null, null);
            return this;
        }

        public Builder ignoreBy(long value) {
            luban.ignoreBy(value, MemoryUnit.KB);
            return this;
        }

        public Builder setTargetDir(String dir) {
            luban.output(null, dir, null);
            return this;
        }

        public Builder setTargetDir(File file) {
            luban.output(file, null, null);
            return this;
        }

        public File get() {
            return luban.get();
        }
    }
}