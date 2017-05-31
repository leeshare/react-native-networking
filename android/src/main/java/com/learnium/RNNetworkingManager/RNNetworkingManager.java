package com.learnium.RNNetworkingManager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Callback;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class RNNetworkingManager extends ReactContextBaseJavaModule {

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
        }
    };

    ReactApplicationContext reactContext;
    HashMap<Long, Callback> callbacks;

    private static final String METHOD = "method";
    private static final String DESTINATION_DIR = "destinationDir";

    private long downloadId;

    public RNNetworkingManager(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.callbacks = new HashMap<Long, Callback>();

        // Register the reciever
        reactContext.registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);
    }

    @Override
    public String getName() {
        return "RNNetworkingManager";
    }

    /*
    下载资源文件包
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
        this._downloadFile(url, destinationDir, successCallback);
        // successCallback.invoke(relativeX, relativeY, width, height);
    }

    private void _downloadFile(String url, String destinationDir, Callback successCallback) {
        // Get an instance of DownloadManager
        DownloadManager downloadManager = (DownloadManager) this.reactContext.getSystemService(Context.DOWNLOAD_SERVICE);

        // Build a request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        // Download it silently
        request.setVisibleInDownloadsUi(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        //request.setDestinationInExternalFilesDir(context, null, "large.zip");

        String fileName = "temp";
        if(url.split("/").length > 0){
            fileName = url.split("/")[url.split("/").length - 1];
        }

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

        // Enqueue the request
        downloadId = downloadManager.enqueue(request);
        callbacks.put(downloadId, successCallback);
    }

    /*
    查询下载进度
     */
    @ReactMethod
    public void queryFileInfo(Callback callback){
        DownloadManager manager = (DownloadManager) reactContext.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        Cursor cursor = manager.query(query);
        if(!cursor.moveToFirst()){
            cursor.close();
            return;
        }

        long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        String localFilename = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
        long downloadedSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        long totalSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

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

    /**
     * 给定根目录，返回一个相对路径所对应的实际文件名.
     *
     * @param baseDir
     *            指定根目录
     * @param absFileName
     *            相对路径名，来自于ZipEntry中的name
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

        WritableMap result = new WritableNativeMap();
        result.putBoolean("success", true);
        callback.invoke(result);
    }

}