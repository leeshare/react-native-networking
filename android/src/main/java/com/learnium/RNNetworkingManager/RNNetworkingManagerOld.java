package com.learnium.RNNetworkingManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.learnium.RNNetworkingManager.download.DownloadInfo;
import com.learnium.RNNetworkingManager.download.DownloadResponseHandler;
import com.learnium.RNNetworkingManager.download.MyDownloadManager;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;
import android.net.Uri;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.lang.Long;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class RNNetworkingManagerOld extends ReactContextBaseJavaModule {

    private String downloadCompleteIntentName = DownloadManager.ACTION_DOWNLOAD_COMPLETE;
    private IntentFilter downloadCompleteIntentFilter = new IntentFilter(downloadCompleteIntentName);
    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle recieving the event
            Long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);

            if(callbacks.containsKey(id)) {
                // Query the state of the download
                DownloadManager downloadManager = (DownloadManager) reactContext.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = downloadManager.query(query);

                // it shouldn't be empty, but just in case
                if (!cursor.moveToFirst()) {
                    Log.e("react-native-networking", "Empty row");
                    return;
                }

                // build the result
                WritableMap result = new WritableNativeMap();
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
                    Log.w("react-native-networking", "Download Failed");
                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reasonString = cursor.getInt(reasonIndex);
                    result.putInt("error", reasonString);
                } else {
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    String downloadedPackageUriString = cursor.getString(uriIndex);
                    WritableMap successResult = new WritableNativeMap();
                    successResult.putString("filePath", downloadedPackageUriString);
                    result.putMap("success", successResult);
                }

                // call the callback
                callbacks.get(id).invoke(result);
            }


            String extraID = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
            long[] references = intent.getLongArrayExtra(extraID);
            for(long reference: references) {
                if (reference == downloadId) {

                }
            }
        }
    };

    ReactApplicationContext reactContext;
    HashMap<Long, Callback> callbacks;

    private static final String METHOD = "method";
    private static final String DESTINATION_DIR = "destinationDir";
    private static final String TO_SHARE_FOLDER = "toShareFolder";
    private static final String SHARE_FOLDER_TYPE = "shareFolderType";

    private long downloadId;
    private String toShareFolder = "no";     //不传 就是不存放到 相册中
    private String shareFolderType = "0";     //如 1 相册/ 2 视频/ 3 音频 / 4 文档等

    // Get an instance of DownloadManager
    DownloadManager downloadManager;

    public RNNetworkingManagerOld(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.callbacks = new HashMap<Long, Callback>();

        // Register the reciever
        reactContext.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
    }

    @ReactMethod
    public void alert(String message) {
        //Toast.makeText(getReactApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getName() {
        return "RNNetworkingManager";
    }


    /*
    1. 下载资源文件包
     */
    @ReactMethod
    public void requestFile(String url,  ReadableMap options, Callback successCallback) {
        Log.w("dm", options.getString("method"));
        if(options.getString(METHOD) == "GET") {

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
        this._downloadFile(url, destinationDir, successCallback);
        // successCallback.invoke(relativeX, relativeY, width, height);

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

    private void _downloadFile(String url, String destinationDir, Callback successCallback) {
        // Get an instance of DownloadManager
        downloadManager = (DownloadManager) this.reactContext.getSystemService(Context.DOWNLOAD_SERVICE);

        // Build a request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        // Download it silently
        request.setVisibleInDownloadsUi(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        //request.setDestinationInExternalFilesDir(context, null, "large.zip");
        request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);  //下载网络需求  手机数据流量、wifi
        request.setAllowedOverRoaming(true);    //设置是否允许漫游网络 建立请求 默认true
        request.allowScanningByMediaScanner();  //为了让下载的文件可以被其他应用扫描到

        String fileName = "temp";
        if(url.split("/").length > 0){
            fileName = url.split("/")[url.split("/").length - 1];
        }

        /*
        if(destinationDir.split("/").length > 0){
            String tempDir = "";
            for(int i = 0; i < destinationDir.split("/").length; i++) {
                if(destinationDir.split("/")[i].isEmpty())
                    continue;
                if(tempDir.isEmpty())
                    tempDir = destinationDir.split("/")[i];
                else
                    tempDir += "/" + destinationDir.split("/")[i];
            }
            Environment.getExternalStoragePublicDirectory(tempDir).mkdir();
        }else {
            Environment.getExternalStoragePublicDirectory(destinationDir).mkdir();
        }
        request.setDestinationInExternalPublicDir("/" + destinationDir + "/", fileName);
        */
        //String dirType = Environment.getExternalStorageState();
        //request.setDestinationInExternalFilesDir(reactContext, destinationDir, fileName);

        if(toShareFolder.equals("yes")){
            String _path = destinationDir + fileName;
            if(shareFolderType.equals("1")){
                //图片下载，如果图片没有后缀，则默认加 .png
                if(_path.indexOf(".") <= 0){
                    _path += ".png";
                }
                request.setDestinationInExternalPublicDir( Environment.DIRECTORY_DCIM, _path);
            }
            else if(shareFolderType.equals("2"))
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, _path);
            else if(shareFolderType.equals("3"))
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, _path);
            else if(shareFolderType.equals("4"))
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, _path);
            else
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, _path);
        }else {
            request.setDestinationInExternalFilesDir(reactContext, destinationDir, fileName);
        }

        // Enqueue the request
        downloadId = downloadManager.enqueue(request);
        //callbacks.put(downloadId, successCallback);

        WritableMap result = new WritableNativeMap();
        result.putString("download_id", Long.toString(downloadId));
        result.putBoolean("success", true);
        successCallback.invoke(result);
    }

    /**
     * 2.1 查询下载进度
     * @param _downloadId
     * @param callback
     */
    @ReactMethod
    public void queryFileInfo2(int _downloadId, Callback callback){
        //DownloadManager manager = (DownloadManager) reactContext.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        if(_downloadId > 0){
            query.setFilterById(_downloadId);
        }else {
            query.setFilterById(downloadId);
        }

        //Cursor cursor = manager.query(query);
        Cursor cursor = downloadManager.query(query);
        if(!cursor.moveToFirst()){
            cursor.close();
            return;
        }

        long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        long downloadedSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        long totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

        String localFilename = null;
        int fileUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
        String fileUri = cursor.getString(fileUriIdx);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (fileUri != null) {
                localFilename = Uri.parse(fileUri).getPath();
            }
        } else {
            //Android 7.0以上的方式：请求获取写入权限，这一步报错
            //过时的方式：DownloadManager.COLUMN_LOCAL_FILENAME
            //int fileNameIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
            //fileName = c.getString(fileNameIdx);
            localFilename = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
        }

        cursor.close();

        /*DownloadManager.STATUS_SUCCESSFUL:   下载成功
        DownloadManager.STATUS_FAILED:       下载失败
        DownloadManager.STATUS_PENDING:      等待下载
        DownloadManager.STATUS_RUNNING:      正在下载
        DownloadManager.STATUS_PAUSED:       下载暂停
        */
        if(status == DownloadManager.STATUS_SUCCESSFUL) {
        }

         /* 特别注意: 查询获取到的 localFilename 才是下载文件真正的保存路径，在创建
         * 请求时设置的保存路径不一定是最终的保存路径，因为当设置的路径已是存在的文件时，
         * 下载器会自动重命名保存路径，例如: .../demo-1.apk, .../demo-2.apk
         */
        //System.out.println("下载成功, 打开文件, 文件路径: " + localFilename);

        WritableMap result = new WritableNativeMap();
        result.putBoolean("is_success", DownloadManager.STATUS_SUCCESSFUL == status);
        result.putString("file_name", localFilename);
        result.putString("download_so_far", Long.toString(downloadedSoFar));
        result.putString("download_total", Long.toString(totalSize));
        result.putString("id", Long.toString(id));
        result.putString("download_id", Long.toString(downloadId));

        callback.invoke(result);
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
            String fileDir = reactContext.getApplicationContext().getExternalFilesDir("").getAbsolutePath();
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
            String fileDir = reactContext.getApplicationContext().getExternalFilesDir("").getAbsolutePath();
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
        OpenFileUtil2.openFile(file, reactContext);
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
