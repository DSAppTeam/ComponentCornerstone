package com.plugin.component.utils;


import com.android.annotations.NonNull;
import com.android.ddmlib.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <pre>
 *     author: Blankj
 *     blog  : http://blankj.com
 *     time  : 2017/05/24
 *     desc  : utils about disk cache
 * </pre>
 */
public final class CacheDiskUtils {

    private static final long DEFAULT_MAX_SIZE = Long.MAX_VALUE;
    private static final int DEFAULT_MAX_COUNT = Integer.MAX_VALUE;
    private static final String CACHE_PREFIX = "cdu_";
    private static final String TYPE_BYTE = "by_";
    private static final String TYPE_STRING = "st_";
    private static final String TYPE_JSON_OBJECT = "jo_";
    private static final String TYPE_JSON_ARRAY = "ja_";
    private static final String TYPE_BITMAP = "bi_";
    private static final String TYPE_DRAWABLE = "dr_";
    private static final String TYPE_PARCELABLE = "pa_";
    private static final String TYPE_SERIALIZABLE = "se_";

    private static final Map<String, CacheDiskUtils> CACHE_MAP = new HashMap<>();

    private final String mCacheKey;
    private final File mCacheDir;
    private final long mMaxSize;
    private final int mMaxCount;
    private DiskCacheManager mDiskCacheManager;


    /**
     * Return the single {@link CacheDiskUtils} instance.
     * <p>cache size: unlimited</p>
     * <p>cache count: unlimited</p>
     *
     * @param cacheDir The directory of cache.
     * @return the single {@link CacheDiskUtils} instance
     */
    public static CacheDiskUtils getInstance(@NonNull final File cacheDir) {
        return getInstance(cacheDir, DEFAULT_MAX_SIZE, DEFAULT_MAX_COUNT);
    }

    /**
     * Return the single {@link CacheDiskUtils} instance.
     *
     * @param cacheDir The directory of cache.
     * @param maxSize  The max size of cache, in bytes.
     * @param maxCount The max count of cache.
     * @return the single {@link CacheDiskUtils} instance
     */
    public static CacheDiskUtils getInstance(@NonNull final File cacheDir,
                                             final long maxSize,
                                             final int maxCount) {
        final String cacheKey = cacheDir.getAbsoluteFile() + "_" + maxSize + "_" + maxCount;
        CacheDiskUtils cache = CACHE_MAP.get(cacheKey);
        if (cache == null) {
            synchronized (CacheDiskUtils.class) {
                cache = CACHE_MAP.get(cacheKey);
                if (cache == null) {
                    cache = new CacheDiskUtils(cacheKey, cacheDir, maxSize, maxCount);
                    CACHE_MAP.put(cacheKey, cache);
                }
            }
        }
        return cache;
    }

    private CacheDiskUtils(final String cacheKey,
                           final File cacheDir,
                           final long maxSize,
                           final int maxCount) {
        mCacheKey = cacheKey;
        mCacheDir = cacheDir;
        mMaxSize = maxSize;
        mMaxCount = maxCount;
    }

    private DiskCacheManager getDiskCacheManager() {
        if (mCacheDir.exists()) {
            if (mDiskCacheManager == null) {
                mDiskCacheManager = new DiskCacheManager(mCacheDir, mMaxSize, mMaxCount);
            }
        } else {
            if (mCacheDir.mkdirs()) {
                mDiskCacheManager = new DiskCacheManager(mCacheDir, mMaxSize, mMaxCount);
            } else {
                Log.e("CacheDiskUtils", "can't make dirs in " + mCacheDir.getAbsolutePath());
            }
        }
        return mDiskCacheManager;
    }

    @Override
    public String toString() {
        return mCacheKey + "@" + Integer.toHexString(hashCode());
    }

    ///////////////////////////////////////////////////////////////////////////
    // about bytes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Put bytes in cache.
     *
     * @param key   The key of cache.
     * @param value The value of cache.
     */
    public void put(@NonNull final String key, final byte[] value) {
        put(key, value, -1);
    }

    /**
     * Put bytes in cache.
     *
     * @param key      The key of cache.
     * @param value    The value of cache.
     * @param saveTime The save time of cache, in seconds.
     */
    public void put(@NonNull final String key, final byte[] value, final int saveTime) {
        realPutBytes(TYPE_BYTE + key, value, saveTime);
    }

    private void realPutBytes(final String key, byte[] value, int saveTime) {
        if (value == null) return;
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return;
        if (saveTime >= 0) value = DiskCacheHelper.newByteArrayWithTime(saveTime, value);
        File file = diskCacheManager.getFileBeforePut(key);
        writeFileFromBytes(file, value);
        diskCacheManager.updateModify(file);
        diskCacheManager.put(file);
    }


    /**
     * Return the bytes in cache.
     *
     * @param key The key of cache.
     * @return the bytes if cache exists or null otherwise
     */
    public byte[] getBytes(@NonNull final String key) {
        return getBytes(key, null);
    }

    /**
     * Return the bytes in cache.
     *
     * @param key          The key of cache.
     * @param defaultValue The default value if the cache doesn't exist.
     * @return the bytes if cache exists or defaultValue otherwise
     */
    public byte[] getBytes(@NonNull final String key, final byte[] defaultValue) {
        return realGetBytes(TYPE_BYTE + key, defaultValue);
    }

    private byte[] realGetBytes(@NonNull final String key) {
        return realGetBytes(key, null);
    }

    private byte[] realGetBytes(@NonNull final String key, final byte[] defaultValue) {
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return defaultValue;
        final File file = diskCacheManager.getFileIfExists(key);
        if (file == null) return defaultValue;
        byte[] data = readFile2Bytes(file);
        if (DiskCacheHelper.isDue(data)) {
            diskCacheManager.removeByKey(key);
            return defaultValue;
        }
        diskCacheManager.updateModify(file);
        return DiskCacheHelper.getDataWithoutDueTime(data);
    }

    ///////////////////////////////////////////////////////////////////////////
    // about String
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Put string value in cache.
     *
     * @param key   The key of cache.
     * @param value The value of cache.
     */
    public void put(@NonNull final String key, final String value) {
        put(key, value, -1);
    }

    /**
     * Put string value in cache.
     *
     * @param key      The key of cache.
     * @param value    The value of cache.
     * @param saveTime The save time of cache, in seconds.
     */
    public void put(@NonNull final String key, final String value, final int saveTime) {
        realPutBytes(TYPE_STRING + key, string2Bytes(value), saveTime);
    }

    /**
     * Return the string value in cache.
     *
     * @param key The key of cache.
     * @return the string value if cache exists or null otherwise
     */
    public String getString(@NonNull final String key) {
        return getString(key, null);
    }

    /**
     * Return the string value in cache.
     *
     * @param key          The key of cache.
     * @param defaultValue The default value if the cache doesn't exist.
     * @return the string value if cache exists or defaultValue otherwise
     */
    public String getString(@NonNull final String key, final String defaultValue) {
        byte[] bytes = realGetBytes(TYPE_STRING + key);
        if (bytes == null) return defaultValue;
        return bytes2String(bytes);
    }


    /**
     * Put serializable in cache.
     *
     * @param key   The key of cache.
     * @param value The value of cache.
     */
    public void put(@NonNull final String key, final Serializable value) {
        put(key, value, -1);
    }

    /**
     * Put serializable in cache.
     *
     * @param key      The key of cache.
     * @param value    The value of cache.
     * @param saveTime The save time of cache, in seconds.
     */
    public void put(@NonNull final String key, final Serializable value, final int saveTime) {
        realPutBytes(TYPE_SERIALIZABLE + key, serializable2Bytes(value), saveTime);
    }

    /**
     * Return the serializable in cache.
     *
     * @param key The key of cache.
     * @return the bitmap if cache exists or null otherwise
     */
    public Object getSerializable(@NonNull final String key) {
        return getSerializable(key, null);
    }

    /**
     * Return the serializable in cache.
     *
     * @param key          The key of cache.
     * @param defaultValue The default value if the cache doesn't exist.
     * @return the bitmap if cache exists or defaultValue otherwise
     */
    public Object getSerializable(@NonNull final String key, final Object defaultValue) {
        byte[] bytes = realGetBytes(TYPE_SERIALIZABLE + key);
        if (bytes == null) return defaultValue;
        return bytes2Object(bytes);
    }

    /**
     * Return the size of cache, in bytes.
     *
     * @return the size of cache, in bytes
     */
    public long getCacheSize() {
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return 0;
        return diskCacheManager.getCacheSize();
    }

    /**
     * Return the count of cache.
     *
     * @return the count of cache
     */
    public int getCacheCount() {
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return 0;
        return diskCacheManager.getCacheCount();
    }

    /**
     * Remove the cache by key.
     *
     * @param key The key of cache.
     * @return {@code true}: success<br>{@code false}: fail
     */
    public boolean remove(@NonNull final String key) {
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return true;
        return diskCacheManager.removeByKey(TYPE_BYTE + key)
                && diskCacheManager.removeByKey(TYPE_STRING + key)
                && diskCacheManager.removeByKey(TYPE_JSON_OBJECT + key)
                && diskCacheManager.removeByKey(TYPE_JSON_ARRAY + key)
                && diskCacheManager.removeByKey(TYPE_BITMAP + key)
                && diskCacheManager.removeByKey(TYPE_DRAWABLE + key)
                && diskCacheManager.removeByKey(TYPE_PARCELABLE + key)
                && diskCacheManager.removeByKey(TYPE_SERIALIZABLE + key);
    }

    /**
     * Clear all of the cache.
     *
     * @return {@code true}: success<br>{@code false}: fail
     */
    public boolean clear() {
        DiskCacheManager diskCacheManager = getDiskCacheManager();
        if (diskCacheManager == null) return true;
        return diskCacheManager.clear();
    }

    private static final class DiskCacheManager {
        private final AtomicLong cacheSize;
        private final AtomicInteger cacheCount;
        private final long sizeLimit;
        private final int countLimit;
        private final Map<File, Long> lastUsageDates
                = Collections.synchronizedMap(new HashMap<File, Long>());
        private final File cacheDir;
        private final Thread mThread;

        private DiskCacheManager(final File cacheDir, final long sizeLimit, final int countLimit) {
            this.cacheDir = cacheDir;
            this.sizeLimit = sizeLimit;
            this.countLimit = countLimit;
            cacheSize = new AtomicLong();
            cacheCount = new AtomicInteger();
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int size = 0;
                    int count = 0;
                    final File[] cachedFiles = cacheDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.startsWith(CACHE_PREFIX);
                        }
                    });
                    if (cachedFiles != null) {
                        for (File cachedFile : cachedFiles) {
                            size += cachedFile.length();
                            count += 1;
                            lastUsageDates.put(cachedFile, cachedFile.lastModified());
                        }
                        cacheSize.getAndAdd(size);
                        cacheCount.getAndAdd(count);
                    }
                }
            });
            mThread.start();
        }

        private long getCacheSize() {
            wait2InitOk();
            return cacheSize.get();
        }

        private int getCacheCount() {
            wait2InitOk();
            return cacheCount.get();
        }

        private File getFileBeforePut(final String key) {
            wait2InitOk();
            File file = new File(cacheDir, getCacheNameByKey(key));
            if (file.exists()) {
                cacheCount.addAndGet(-1);
                cacheSize.addAndGet(-file.length());
            }
            return file;
        }

        private void wait2InitOk() {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private File getFileIfExists(final String key) {
            File file = new File(cacheDir, getCacheNameByKey(key));
            if (!file.exists()) return null;
            return file;
        }

        private String getCacheNameByKey(final String key) {
            return CACHE_PREFIX + key.substring(0, 3) + key.substring(3).hashCode();
        }

        private void put(final File file) {
            cacheCount.addAndGet(1);
            cacheSize.addAndGet(file.length());
            while (cacheCount.get() > countLimit || cacheSize.get() > sizeLimit) {
                cacheSize.addAndGet(-removeOldest());
                cacheCount.addAndGet(-1);
            }
        }

        private void updateModify(final File file) {
            Long millis = System.currentTimeMillis();
            file.setLastModified(millis);
            lastUsageDates.put(file, millis);
        }

        private boolean removeByKey(final String key) {
            File file = getFileIfExists(key);
            if (file == null) return true;
            if (!file.delete()) return false;
            cacheSize.addAndGet(-file.length());
            cacheCount.addAndGet(-1);
            lastUsageDates.remove(file);
            return true;
        }

        private boolean clear() {
            File[] files = cacheDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(CACHE_PREFIX);
                }
            });
            if (files == null || files.length <= 0) return true;
            boolean flag = true;
            for (File file : files) {
                if (!file.delete()) {
                    flag = false;
                    continue;
                }
                cacheSize.addAndGet(-file.length());
                cacheCount.addAndGet(-1);
                lastUsageDates.remove(file);
            }
            if (flag) {
                lastUsageDates.clear();
                cacheSize.set(0);
                cacheCount.set(0);
            }
            return flag;
        }

        /**
         * Remove the oldest files.
         *
         * @return the size of oldest files, in bytes
         */
        private long removeOldest() {
            if (lastUsageDates.isEmpty()) return 0;
            Long oldestUsage = Long.MAX_VALUE;
            File oldestFile = null;
            Set<Map.Entry<File, Long>> entries = lastUsageDates.entrySet();
            synchronized (lastUsageDates) {
                for (Map.Entry<File, Long> entry : entries) {
                    Long lastValueUsage = entry.getValue();
                    if (lastValueUsage < oldestUsage) {
                        oldestUsage = lastValueUsage;
                        oldestFile = entry.getKey();
                    }
                }
            }
            if (oldestFile == null) return 0;
            long fileSize = oldestFile.length();
            if (oldestFile.delete()) {
                lastUsageDates.remove(oldestFile);
                return fileSize;
            }
            return 0;
        }
    }

    private static final class DiskCacheHelper {

        static final int TIME_INFO_LEN = 14;

        private static byte[] newByteArrayWithTime(final int second, final byte[] data) {
            byte[] time = createDueTime(second).getBytes();
            byte[] content = new byte[time.length + data.length];
            System.arraycopy(time, 0, content, 0, time.length);
            System.arraycopy(data, 0, content, time.length, data.length);
            return content;
        }

        /**
         * Return the string of due time.
         *
         * @param seconds The seconds.
         * @return the string of due time
         */
        private static String createDueTime(final int seconds) {
            return String.format(
                    Locale.getDefault(), "_$%010d$_",
                    System.currentTimeMillis() / 1000 + seconds
            );
        }

        private static boolean isDue(final byte[] data) {
            long millis = getDueTime(data);
            return millis != -1 && System.currentTimeMillis() > millis;
        }

        private static long getDueTime(final byte[] data) {
            if (hasTimeInfo(data)) {
                String millis = new String(copyOfRange(data, 2, 12));
                try {
                    return Long.parseLong(millis) * 1000;
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
            return -1;
        }

        private static byte[] getDataWithoutDueTime(final byte[] data) {
            if (hasTimeInfo(data)) {
                return copyOfRange(data, TIME_INFO_LEN, data.length);
            }
            return data;
        }

        private static byte[] copyOfRange(final byte[] original, final int from, final int to) {
            int newLength = to - from;
            if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
            byte[] copy = new byte[newLength];
            System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
            return copy;
        }

        private static boolean hasTimeInfo(final byte[] data) {
            return data != null
                    && data.length >= TIME_INFO_LEN
                    && data[0] == '_'
                    && data[1] == '$'
                    && data[12] == '$'
                    && data[13] == '_';
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // other utils methods
    ///////////////////////////////////////////////////////////////////////////

    private static byte[] string2Bytes(final String string) {
        if (string == null) return null;
        return string.getBytes();
    }

    private static String bytes2String(final byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes);
    }

    private static byte[] serializable2Bytes(final Serializable serializable) {
        if (serializable == null) return null;
        ByteArrayOutputStream baos;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos = new ByteArrayOutputStream());
            oos.writeObject(serializable);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Object bytes2Object(final byte[] bytes) {
        if (bytes == null) return null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void writeFileFromBytes(final File file, final byte[] bytes) {
        FileOutputStream outputStream = null;
        FileChannel fc = null;
        try {
            outputStream = new FileOutputStream(file, false);
            fc = outputStream.getChannel();
            fc.write(ByteBuffer.wrap(bytes));
            fc.force(true);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fc != null) {
                    fc.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] readFile2Bytes(final File file) {
        RandomAccessFile accessFile = null;
        FileChannel fc = null;
        FileInputStream inputStream = null;
        try {
            accessFile = new RandomAccessFile(file, "r");
            fc = accessFile.getChannel();
            int size = (int) fc.size();
            byte[] data = new byte[size];
            fc.read(ByteBuffer.wrap(data));
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fc != null) {
                    fc.close();
                }
                if (accessFile != null) {
                    accessFile.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
