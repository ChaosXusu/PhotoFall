package com.itheima.photofall97;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by teacher on 2016/6/2.
 */
public class MyScrollView extends ScrollView implements View.OnTouchListener {

    private static final int PAGE_SIZE = 20;//每一次加载的数量
    private int page = 0;//当前加载的页面
    //是否是第一次加载
    private boolean isOnceLoad;
    private final ImageLoader mImageLoader;
    //每一列的列宽
    private int columnWidth;
    //三列
    private LinearLayout firstColumn;
    private LinearLayout secondColumn;
    private LinearLayout thirdColumn;
    //三列的列高
    private int firstHeight;
    private int secondHeight;
    private int thirdHeight;

    //记录scrollY移动的临时变量
    private int scrollYChange;
    //也就是屏幕的高度
    private int scrollViewHeight;
    //scrollView的直接子布局
    private View child;

    private Set<LoadImageTask> taskCollections;
    private List<ImageView> imageList;

    //要在布局中使用此控件
    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mImageLoader = ImageLoader.getInstance(context);

        taskCollections = new HashSet<>();
        imageList = new ArrayList<>();

        setOnTouchListener(this);

    }

    //要知道每一列的宽度，必须在onmeasure方法之后
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!isOnceLoad) {
            firstColumn = (LinearLayout) findViewById(R.id.first_column);
            secondColumn = (LinearLayout) findViewById(R.id.second_column);
            thirdColumn = (LinearLayout) findViewById(R.id.third_column);

            columnWidth = firstColumn.getWidth();

            scrollViewHeight = getHeight();

            child = getChildAt(0);

            isOnceLoad = true;
            //初始化加载
            loadMoreImages();

        }
    }

    //加载图片
    private void loadMoreImages() {
        //分页加载
        int startIndex = page * PAGE_SIZE;//0 * 20 = 0
        int endIndex = (page + 1) * PAGE_SIZE;//1 * 20 = 20
        //对加载起始位置进行判断
        if (startIndex < Images.imageUrls.length) {
            Toast.makeText(getContext(), "loading...", Toast.LENGTH_SHORT).show();
            //对加载结束位置进行判断
            if (endIndex > Images.imageUrls.length) {
                endIndex = Images.imageUrls.length;
            }

            for (int i = startIndex; i < endIndex; i++) {
                String url = Images.imageUrls[i];
                //访问网络，开启一个异步线程asycTask
                LoadImageTask loadImageTask = new LoadImageTask();
                //将当前任务加入到集合中
                taskCollections.add(loadImageTask);
                loadImageTask.execute(url);

            }


            page++;
        } else {
            Toast.makeText(getContext(), "no more pic...", Toast.LENGTH_SHORT).show();
        }
    }



    class LoadImageTask extends AsyncTask<String,Void,Bitmap>{
        String url;
        ImageView mImageView;

        public LoadImageTask(ImageView imageView) {
            mImageView = imageView;
        }

        public LoadImageTask() {
        }

        //在子线程中执行的
        @Override
        protected Bitmap doInBackground(String... params) {
            //加载图片
            url = params[0];
            Bitmap bitmap = mImageLoader.loadImage(url, columnWidth);
            return bitmap;
        }
        //在主线程中，但是在子线程之后
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //展示图片
            if(bitmap != null){
                if (mImageView != null){
                    mImageView.setImageBitmap(bitmap);
                }else{
                    //为了精度更准确，图片重新计算高度
                    double ratio = bitmap.getWidth() / (columnWidth * 1.0);
                    int imageHeight = (int) (bitmap.getHeight() / ratio);

                    //将图片往最短的一列进行添加
                    ImageView imageView = new ImageView(getContext());
                    imageView.setImageBitmap(bitmap);

                    //设置参数
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(columnWidth,imageHeight);
                    imageView.setLayoutParams(params);
                    imageView.setPadding(5,5,5,5);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                    imageView.setTag(R.string.image_url,url);
                    //把图片添加到集合中
                    imageList.add(imageView);

                    findShortestColumn(imageHeight,imageView).addView(imageView);
                    //任务结束时移除
                    taskCollections.remove(this);

                }

            }
        }


    }

    //找到最短一列的方法
    private LinearLayout findShortestColumn(int imageHeight,ImageView imageView) {
        if (firstHeight <= secondHeight){
            if (firstHeight <= thirdHeight){
                //图片的头部坐标
                imageView.setTag(R.string.image_top,firstHeight);
                firstHeight += imageHeight;
                //图片的底部坐标
                imageView.setTag(R.string.image_bottom,firstHeight);
                return firstColumn;
            }else{
                imageView.setTag(R.string.image_top,thirdHeight);
                thirdHeight += imageHeight;
                imageView.setTag(R.string.image_bottom,thirdHeight);
                return thirdColumn;
            }
        }else{
            if (secondHeight <= thirdHeight){
                imageView.setTag(R.string.image_top,secondHeight);
                secondHeight += imageHeight;
                imageView.setTag(R.string.image_bottom,secondHeight);
                return secondColumn;
            }else{
                imageView.setTag(R.string.image_top,thirdHeight);
                thirdHeight += imageHeight;
                imageView.setTag(R.string.image_bottom,thirdHeight);
                return thirdColumn;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    //先于onTouchEvent方法执行的，如果返回true，scrollview就不能滚动
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //1. 手指抬起的时候
        if (event.getAction() == MotionEvent.ACTION_UP){
            //判断是否滚动到底部,因为惯性，使用递归
            Message message = mHander.obtainMessage();
            message.obj = this;
            mHander.sendMessageDelayed(message,5);

        }
        return false;
    }

    //是因为scrollView滚动事件在此方法中，为了防止冲突
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    public Handler mHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            MyScrollView myScrollView = (MyScrollView) msg.obj;
            int scrollY = myScrollView.getScrollY();

            if(scrollYChange == scrollY){
                //已经停止
                //判断是否滚动到底部
                //为了性能考虑，要确保所有加载任务都已结束，再进行加载更多
                if(scrollY + scrollViewHeight >= child.getHeight() && taskCollections.size() == 0){

                    myScrollView.loadMoreImages();

                }

                // 判断图片是否屏幕内
                myScrollView.checkVisibility();
            }else{
                //还没停止
                scrollYChange = scrollY;
                //递归
                Message message = mHander.obtainMessage();
                message.obj = myScrollView;
                mHander.sendMessageDelayed(message,5);
            }
        }

    };

    public void checkVisibility(){
        //遍历所有的图片
        for (int i = 0; i < imageList.size() ; i++) {
            ImageView imageView = imageList.get(i);
            //进行坐标判断
            int top = (int) imageView.getTag(R.string.image_top);
            int bottom = (int) imageView.getTag(R.string.image_bottom);
            if(bottom > getScrollY() && top < getScrollY() + scrollViewHeight){
                //当前图片在屏幕内
                //从内存中先取
                String url = (String) imageView.getTag(R.string.image_url);
                Bitmap bitmap = mImageLoader.getBitmapFromMemory(url);
                if (bitmap != null){
                    imageView.setImageBitmap(bitmap);
                }else{
                    //开启线程
                    LoadImageTask loadImageTask = new LoadImageTask(imageView);
                    loadImageTask.execute(url);
                }

            }else{
                //不在屏幕内，设置一张默认的图片
                imageView.setImageResource(R.drawable.empty_photo);

            }

        }

    }

}
