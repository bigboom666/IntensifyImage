package me.kareluo.intensify.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by felix on 16/5/18.
 */
//IntensifyImageCache是按照精度，缓存了不同的ImageCache，它的键值是精度，值是ImageCache
class IntensifyImageCache extends IntensifyCache<Integer, IntensifyImageCache.ImageCache, Void> {

    private int mSubMaxSize;
    private Rect mOriginalRect;
    private BitmapRegionDecoder mRegionDecoder;

    private int BLOCK_SIZE = 300;

    //maxSize:最多几个imageCache缓存对象
    //subMaxSize:imageCache缓存的的像素数量
    public IntensifyImageCache(
            int maxSize, int subMaxSize, int blockSize, BitmapRegionDecoder bitmapRegionDecoder) {
        super(maxSize);
        BLOCK_SIZE = blockSize;
        mSubMaxSize = subMaxSize;
        mRegionDecoder = bitmapRegionDecoder;
        if (mRegionDecoder == null) {
            throw new IllegalArgumentException("BitmapRegionDecoder is null.");
        }

        //原始图片宽高
        mOriginalRect = new Rect(0, 0, mRegionDecoder.getWidth(), mRegionDecoder.getHeight());
    }

    @Override
    protected void entryRemoved(boolean evicted, Integer key, ImageCache oldValue, ImageCache newValue) {
        if (oldValue != null) oldValue.evictAll();
    }

    //创建key对应的数据
    @Override
    protected ImageCache create(Integer key) {
        return new ImageCache(mSubMaxSize, key);
    }




//加载指定坐标对应的Bitmap对象，以300大小的块和原始图像的交集
    public class ImageCache extends IntensifyCache<Point, Bitmap, Integer> {

        public ImageCache(int maxSize, Integer level) {
            super(maxSize, level);
        }

        //还他娘是个递归
        @Override
        protected Bitmap alternative(Point key, Integer level) {
            if (!this.level.equals(level)) {
                Bitmap bitmap = justGet(key);
                if (bitmap != null) {
                    return bitmap;
                }
            }
            if (level > 1) {
                ImageCache imageCache = IntensifyImageCache.this.justGet(level >> 1);
                if (imageCache != null) {
                    return imageCache.alternative(key, level >> 1);
                }
            }
            return null;
        }

        @Override
        protected void entryRemoved(boolean evicted, Point key, Bitmap oldValue, Bitmap newValue) {
            if (oldValue != null) oldValue.recycle();
        }

        @Override
        protected Bitmap create(Point key) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = level;
            Rect rect = blockRect(key.x, key.y, BLOCK_SIZE * level);
            if (rect.intersect(mOriginalRect)) {
                return mRegionDecoder.decodeRegion(rect, options);
            }
            return null;
        }

        @Override
        protected int sizeOf(Point key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }
    }

    public static Rect blockRect(int x, int y, int size) {
        return new Rect(x * size, y * size, (x + 1) * size, (y + 1) * size);
    }
}
