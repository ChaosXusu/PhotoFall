package com.itheima.photofall97;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by teacher on 2016/6/2.
 */
public class ImageLoader {

    private static ImageLoader mImageLoader;
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskLruCache;

    private ImageLoader(Context context) {
        //初始化内存缓存
        initMemoryCache();
        //初始化本地缓存
        initDiskCache(context);
    }

    //懒汉可以传参，饿汉不可以
    public static ImageLoader getInstance(Context context) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(context);
        }
        return mImageLoader;
    }

    private void initDiskCache(Context context) {

        File directory = Utils.getDiskCacheDir(context, "thumb");
        int appVersion = Utils.getAppVersion(context);
        int valueCount = 1;
        long maxSize = 32 * 1024 * 1024;//32M

        try {
            mDiskLruCache = DiskLruCache.open(directory, appVersion, valueCount, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromDisk(String url, int width) {
        FileInputStream fis = null;
        FileDescriptor fd = null;
        //因为key会被用作文件名，故进行MD5加密
        String key = Utils.hashKeyForDisk(url);
        try {
//        1.从本地取
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot == null) {
                // 2. 本地也没有，就从网络加载（往本地和内存都各存一份）
                DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                OutputStream outputStream = editor.newOutputStream(0);
                //访问网络
                boolean success = Utils.downloadUrlToStream(url, outputStream);
                if (success) {
                    editor.commit();
                } else {
                    editor.abort();
                }

                snapshot = mDiskLruCache.get(key);
            }

            if (snapshot != null) {
                fis = (FileInputStream) snapshot.getInputStream(0);
                //文件描述符，就是文件的索引
                fd = fis.getFD();
                //解码图片
                Bitmap bitmap = Utils.decodeSampledBitmapFromFileDescriptor(fd, width);
                if (bitmap != null) {
                    //内存里存一份
                    addBitmapToMemory(url, bitmap);
                }
                return bitmap;
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fd == null && fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }


        return null;
    }

    private void initMemoryCache() {
        //获取当前app运行内存的大小，这是系统分配的
        int maxSize = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxSize / 8;

        //告诉内存每一张图片的大小
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //告诉内存每一张图片的大小
                return value.getByteCount();
            }
        };
    }

    //加载图片的方法
    public Bitmap loadImage(String url, int columnWidth) {
//        1. 首先从内存中取
        Bitmap bitmap = getBitmapFromMemory(url);
        if (bitmap == null) {
//        2. 内存里没有，就从本地取（往内存中存）
//        3. 本地也没有，就从网络加载（往本地和内存都各存一份）
            bitmap = getBitmapFromDisk(url, columnWidth);
        }

        return bitmap;
    }

    //从内存中取
    public Bitmap getBitmapFromMemory(String url) {
        if (!TextUtils.isEmpty(url)) {
            Bitmap bitmap = mLruCache.get(url);
            return bitmap;
        }
        return null;
    }

    //往内存中存图片
    private void addBitmapToMemory(String url, Bitmap bitmap) {
        if (!TextUtils.isEmpty(url)) {
            mLruCache.put(url, bitmap);
        }
    }
}
