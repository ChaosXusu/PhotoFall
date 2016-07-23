# PhotoFall

## 瀑布流原理

* 一.布局
1. 不知道每一列有多少张图片
2. 只知道每一列的展示宽度，也不知道每一张图片的高度
3. 要在布局中动态添加imageView
4. SrollView 包裹listview时候，因为listview的高度由子布局决定，所以每添加一个item，就得不停测量自身

	* 1. ScrollView 包裹三个linearlayout
	* 2. 每个linearlayout动态添加imageview
* 二.加载图片的策略

1. 分页加载
	1. 初始化加载第一波，一是为了防止内存溢出，二是为了加载速度
	2. 拉到底部时，再次进行加载
2. 图片加载
	1. 图片压缩
		1. 压缩比 = 图片的实际宽度 / 展示宽度
		2. 图片的展示高度 = 图片的实际高度 / 压缩比
	2. 图片缓存（三级缓存）
  3. 下拉加载更多（事件分发）
	1. 手指抬起的时候,滑动停止的判断方式（用递归）
	2. 是否滚动到底部
	    getscrollY + ScrollViewHeight >= ScrollView直接子布局的高度
	3. getscrollY:ScrollView 竖直方法滚动的距离
	4. 屏幕的高度：就是ScrollView的高度
	5. 图片墙的高度：就是ScrollView直接子布局的高度
	
* 三.做一些优化
  1. 滑动停止时，才加载图片
  2. 不在屏幕内，显示一张默认图片
	1. 在屏幕上方：图片底部坐标 < scrollY
	2. 在屏幕下方：图片头部坐标 > scrolly + 屏幕的高度
	
	## 图片缓存
* 三级缓存（准确的说，客户端只有二级缓存）
1. 内存：加载速度快
	1. SDK3.0之前，内存缓存用的是软引用（弱引用、强引用），当内存不足的时候，垃圾回收器会优先回收软引用指定的对象
	List<SoftReference<Bitmap>> list;
	2. SDK3.0之后，Google推荐使用Lrucache（least recently used）最近最少使用策略
	
2. 本地：数据持久化，节省流量，加载速度较快
	1. DiskLruCache 硬盘缓存策略，谷歌推荐 
	2. /SDCacrd/android/data/包名 这个路径是安卓系统认定的app外部存储路径，会随着app删除而删除
	3. data/data/包名 内部存储路径
	
3. 网络

* 加载图片的策略
	1. 首先从内存中取
	2. 内存里没有，就从本地取（往内存中存）
	3. 本地也没有，就从网络加载（往本地和内存都各存一份）


1. AsyncTask线程之间传值是handler
class LoadImageTask extends AsyncTask<String,Void,Bitmap>{

        //在主线程中，但是在子线程之前
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //在子线程中执行的
        @Override
        protected Bitmap doInBackground(String... params) {
            publishProgress();
            return null;
        }
        //在主线程中，但是在子线程之后
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            
        }
        
        //刷新进度的方法，publishProgress()调用
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }
