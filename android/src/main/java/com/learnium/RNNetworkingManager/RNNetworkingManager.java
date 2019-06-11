package com.learnium.RNNetworkingManager;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.learnium.RNNetworkingManager.download.DownloadInfo;
import com.learnium.RNNetworkingManager.download.DownloadResponseHandler;
import com.learnium.RNNetworkingManager.download.MyDownloadManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RNNetworkingManager extends ReactContextBaseJavaModule {

    String LogName = "RNNetworkingManager";
    private static final String CONFIRM_EVENT_NAME = "confirmEvent";
    private static final String EVENT_KEY_CONFIRM = "confirm";

    private static final String METHOD = "method";
    private static final String URL = "url";
    private static final String DESTINATION_DIR = "destinationDir";
    private static final String TO_SHARE_FOLDER = "toShareFolder";
    private static final String SHARE_FOLDER_TYPE = "shareFolderType";
    private static final String IS_STOP = "is_stop";

    private long downloadId;
    private String url = "";
    private String toShareFolder = "no";     //不传 就是不存放到 相册中
    private String shareFolderType = "0";     //如 1 相册/ 2 视频/ 3 音频 / 4 文档等

    //ReactApplicationContext reactContext;
    //static Activity activity;
    private Context mContext;

    public RNNetworkingManager(ReactApplicationContext reactContext) {
        super(reactContext);
        //this.reactContext = reactContext;
        mContext = (Context)reactContext;

        //默认最大请求数是1
        //MyDownloadManager.getInstance((Context)reactContext).setMaxRequests(2);

        // Register the reciever
        //reactContext.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);

    }

    @ReactMethod
    public void alert(String message) {
        //Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getName() {
        return "RNNetworkingManager";
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(DownloadInfo info){
        //txt.setText(txt.getText().toString()+"\n"+info.getUrl()+"---progress:"+info.getProgress()+"---total:"+info.getTotal());
    }

    /**
     * 此方法的作用是，支持原始多次回调js
     * @param reactContext
     * @param eventName
     * @param params
     */
    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void commonEvent(String eventKey, Integer type, Long sofar, Long total, String fileName, String url) {
        WritableMap map = Arguments.createMap();
        map.putString("type", eventKey);

        map.putString("download_sofar", Long.toString(sofar));
        map.putString("download_total", Long.toString(total));
        map.putString("file_name", fileName);
        map.putString("file_url", url);
        sendEvent(getReactApplicationContext(), CONFIRM_EVENT_NAME, map);
    }

    /*
    1. 下载资源文件包
     */
    @ReactMethod
    public void requestFile(ReadableMap options) {
        Log.w("dm", options.getString("method"));

        //activity = getCurrentActivity();
        //if(activity != null){
        //    mContext = activity.getApplicationContext();
        //}

        if(options.getString(METHOD) == "GET") {

        }

        if(options.hasKey(URL)){
            url = options.getString(URL);
        }
        String destinationDir = ".ys";
        if(options.hasKey(DESTINATION_DIR)){
            destinationDir = options.getString(DESTINATION_DIR);
        }
        if(options.hasKey(TO_SHARE_FOLDER)){
            toShareFolder = options.getString(TO_SHARE_FOLDER);
        }
        if(options.hasKey(SHARE_FOLDER_TYPE)){
            shareFolderType = options.getString(SHARE_FOLDER_TYPE);
        }

        //EventBus.getDefault().register(this);
        this.newDownloadFile(url, destinationDir);

    }

    //循环删除文件
    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteDirWihtFile(file); // 递规的方式删除文件夹
        }
        dir.delete();// 删除目录本身
    }

    private void newDownloadFile(String url, String destinationDir){
        String fileName = "temp";
        if(url.split("/").length > 0){
            fileName = url.split("/")[url.split("/").length - 1];
        }
        String _path = getDiskFileDir(mContext) + fileName;
        if(toShareFolder.equals("yes")){
            _path = "/" + destinationDir + fileName;
            if(shareFolderType.equals("1")){
                //图片下载，如果图片没有后缀，则默认加 .png
                if(_path.indexOf(".") <= 0){
                    _path += ".png";
                }
                //request.setDestinationInExternalPublicDir( Environment.DIRECTORY_DCIM, _path);
                _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + _path;
                //_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + _path;
            }
            else if(shareFolderType.equals("2")) {
                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, _path);
                _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + _path;
            }
            else if(shareFolderType.equals("3")) {
                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, _path);
                _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath() + _path;
            }
            else if(shareFolderType.equals("4")) {
                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, _path);
                _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + _path;
            }
            else {
                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, _path);
                _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + _path;
            }
        }else {
            //request.setDestinationInExternalFilesDir(reactContext, destinationDir, fileName);
            _path = getDiskFileDir(mContext) + fileName;
        }
        //String dir = getDiskFileDir(mContext) + fileName;
        String dir = _path;
        MyDownloadManager.getInstance(mContext).download(url, dir, new DownloadResponseHandler() {
            @Override
            public void onFinish(File download_file) {

            }

            @Override
            public void onProgress(long currentBytes, long totalBytes, DownloadInfo info) {
                //Log.e(LogName, currentBytes + "----" + totalBytes);
                //progressBar1.setMax((int) totalBytes);
                //progressBar1.setProgress((int) currentBytes);

                commonEvent(EVENT_KEY_CONFIRM, 2, currentBytes, totalBytes, info.getFileName(), info.getUrl());
            }

            @Override
            public void onFailure(String error_msg) {

            }

            @Override
            public void onPause(DownloadInfo info) {

            }

            @Override
            public void onCancle(DownloadInfo info) {

            }
        });
    }
    @ReactMethod
    public void pauseDownload(String url){
        //Context context = (Context)this.reactContext;
        /*if(options.hasKey(URL)){
            String url = options.getString(URL);
            MyDownloadManager.getInstance(mContext).pause(url);
        }*/

        Log.e(LogName, "pause=====" + url);
        MyDownloadManager.getInstance(mContext).pause(url);
    }




    /**
     * 2.1 查询下载进度
     * @param _downloadId
     * @param callback
     */
    @ReactMethod
    public void queryFileInfo2(int _downloadId, Callback callback){

    }

    /*
    2. 查询下载进度
     */
    @ReactMethod
    public void queryFileInfo(Callback callback){
        queryFileInfo2(0, callback);
    }

    /**
     * 给定根目录，返回一个相对路径所对应的实际文件名.
     *
     * @param baseDir 指定根目录
     * @param absFileName 相对路径名，来自于ZipEntry中的name
     * @return java.io.File 实际的文件
     */
    private static File getRealFileName(String baseDir, String absFileName) {
        //LogUtils.i(TAG, "baseDir=" + baseDir + "------absFileName=" + absFileName);
        absFileName = absFileName.replace("\\", "/");
        //LogUtils.i(TAG, "absFileName=" + absFileName);
        String[] dirs = absFileName.split("/");
        //LogUtils.i(TAG, "dirs=" + dirs);
        File ret = new File(baseDir);
        String substr = null;
        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                substr = dirs[i];
                ret = new File(ret, substr);
            }

            if (!ret.exists())
                ret.mkdirs();
            substr = dirs[dirs.length - 1];
            ret = new File(ret, substr);
            return ret;
        } else {
            ret = new File(ret, absFileName);
        }
        return ret;
    }

    /*
    3. 解压一个 文件夹压缩包
     */
    @ReactMethod
    public void unzipFile(String zipFile, String folderPath, Callback callback){
        ZipFile zfile = null;
        try {
            // 转码为GBK格式，支持中文
            zfile = new ZipFile(zipFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        File temp = new File(folderPath);
        if(!temp.exists()){
            temp.mkdir();
        }
        Enumeration zList = zfile.entries();
        ZipEntry ze = null;
        byte[] buf = new byte[1024];
        while (zList.hasMoreElements()) {
            ze = (ZipEntry) zList.nextElement();
            // 列举的压缩文件里面的各个文件，判断是否为目录
            if (ze.isDirectory()) {
                String dirstr = folderPath + ze.getName();
                //LogUtils.i(TAG, "dirstr=" + dirstr);
                dirstr.trim();
                File f = new File(dirstr);
                f.mkdir();
                continue;
            }
            OutputStream os = null;
            FileOutputStream fos = null;
            // ze.getName()会返回 script/start.script这样的，是为了返回实体的File
            File realFile = getRealFileName(folderPath, ze.getName());
            try {
                fos = new FileOutputStream(realFile);
            } catch (FileNotFoundException e) {
                //LogUtils.e(TAG, e.getMessage());
                return;
            }
            os = new BufferedOutputStream(fos);
            InputStream is = null;
            try {
                is = new BufferedInputStream(zfile.getInputStream(ze));
            } catch (IOException e) {
                //LogUtils.e(TAG, e.getMessage());
                return;
            }
            int readLen = 0;
            // 进行一些内容复制操作
            try {
                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                    os.write(buf, 0, readLen);
                }
            } catch (IOException e) {
                //LogUtils.e(TAG, e.getMessage());
                return;
            }
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                //LogUtils.e(TAG, e.getMessage());
                return;
            }
        }
        try {
            zfile.close();
        } catch (IOException e) {
            //LogUtils.e(TAG, e.getMessage());
            return;
        }

        File f = new File(zipFile);
        f.delete();

        WritableMap result = new WritableNativeMap();
        result.putBoolean("success", true);
        callback.invoke(result);
    }

    /*
    4. 判断文件或目录是否存在
     */
    @ReactMethod
    public void isFileExist(String file, Callback callback){
        WritableMap result = new WritableNativeMap();
        File f = new File(file);
        if(f.exists()){
            result.putBoolean("success", true);
            result.putString("full_path", f.getAbsolutePath());
        }else {
            String fileDir = mContext.getApplicationContext().getExternalFilesDir("").getAbsolutePath();
            File f2 = new File(fileDir + "/" + file);

            if(f2.exists()){
                result.putBoolean("success", true);
                result.putString("full_path", f2.getAbsolutePath());
            }else {
                result.putBoolean("success", false);
                result.putString("fileDir", fileDir);
                result.putString("full_path", f2.getAbsolutePath());
            }
        }
        callback.invoke(result);
    }
    /*
    4.2 判断文件或目录是否存在
     */
    @ReactMethod
    public void isFilePublicExist(String file, String fileType, Callback callback){
        WritableMap result = new WritableNativeMap();
        File f = new File(file);
        if(f.exists()){
            result.putBoolean("success", true);
            result.putString("full_path", f.getAbsolutePath());
        }else {
            String fileDir = "";
            if(fileType.equals("picture"))
                fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) .getAbsolutePath();
                //fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) .getAbsolutePath();
            else if(fileType.equals("video"))
                fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) .getAbsolutePath();
            else if(fileType.equals("audio"))
                fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) .getAbsolutePath();
            else if(fileType.equals("document"))
                fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) .getAbsolutePath();
            else
                fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) .getAbsolutePath();

            File f2 = new File(fileDir + "/" + file);

            if(f2.exists()){
                result.putBoolean("success", true);
                result.putString("full_path", f2.getAbsolutePath());
            }else {
                result.putBoolean("success", false);
                result.putString("fileDir", fileDir);
                result.putString("full_path", f2.getAbsolutePath());
            }
        }
        callback.invoke(result);
    }
    /*
    4.1. 判断文件或目录是否存在
    并获得音频时长
    */
    @ReactMethod
    public void isMediaExist(String file, Boolean isNeedDuration, Callback callback){
        WritableMap result = new WritableNativeMap();
        File f = new File(file);
        String path = "";
        if(f.exists()){
            path = f.getAbsolutePath();
            result.putBoolean("success", true);
            result.putString("full_path", path);
        }else {
            String fileDir = mContext.getApplicationContext().getExternalFilesDir("").getAbsolutePath();
            File f2 = new File(fileDir + "/" + file);
            if(f2.exists()){
                path = f2.getAbsolutePath();
                result.putBoolean("success", true);
                result.putString("full_path", path);
            }else {
                result.putBoolean("success", false);
            }
        }
        double duration = 0;
        if(isNeedDuration && path != "") {
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(path);
                player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            duration = player.getDuration();
            player.release();
            result.putDouble("duration", duration);
        }
        callback.invoke(result);
    }

    /*
    5. 读文件，返回字符串
     */
    @ReactMethod
    public void readFile(String path, Callback callback) {
        File file = new File(path);
        BufferedReader reader = null;
        String laststr = "";
        try {
            // System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                System.out.println("line " + line + ": " + tempString);
                laststr = laststr + tempString;
                ++line;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }

        WritableMap result = new WritableNativeMap();
        result.putString("content", laststr);
        callback.invoke(result);
    }


    /*
    6. 清除目标目录 clear destination files
    */
    @ReactMethod
    public void clearDestinationDir(ReadableMap options, Callback callback) {
        //String destinationDir = ".ys";
        String destinationDir = "";
        if(options.hasKey(DESTINATION_DIR)){
            destinationDir = options.getString(DESTINATION_DIR);
        }
        //DownloadManager downloadManager = (DownloadManager) this.reactContext.getSystemService(Context.DOWNLOAD_SERVICE);

        //String dirType = Environment.getExternalStorageState();
        //request.setDestinationInExternalFilesDir(reactContext, destinationDir, fileName);
        File f = getReactApplicationContext().getExternalFilesDir(null);
        String dirPath = f.getAbsolutePath();
        File coursePath = new File(dirPath + "/" + destinationDir);
        deleteDirWihtFile(coursePath);

        WritableMap result = new WritableNativeMap();
        result.putString("path1", dirPath);
        result.putString("path2", coursePath.getAbsolutePath());
        callback.invoke(result);
    }

    /*
    7. 打开文件
    2018-10-27
    */
    @ReactMethod
    public void openFile(String path) {
        //OpenFileUtil.openFile(path);

        File file = new File(path);
        if (!file.exists())
            return;
        OpenFileUtil2.openFile(file, mContext);
    }

    /**
     * 获取缓存文件目录
     *
     * @param context
     * @return
     */
    public static String getDiskFileDir(Context context) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalFilesDir(null).getPath();
        } else {
            cachePath = context.getFilesDir().getPath();
        }
        return cachePath;
    }

    //--------------------------------------------- 2019-05-24 ----------------------------------
    //DownloadManager 不支持手动控制 续传，每一次暂停后，再下载都要重新去下载，所以抛弃掉



    //--------------------------------------------- end -----------------------------------------
}
