
fork from:		https://github.com/eduedix/react-native-networking

(2019-06-11)
  修改下载到相册还是使用 Environment.DIRECTORY_DCIM ，而不是 Environment.DIRECTORY_PICTURES 。

I changed some function in android and ios.

(2019-05-27)
  修改android端支持断点续传，抛弃原来系统提供的 DownloadManager 。
  修改回调 为 DeviceEventManagerModule.RCTDeviceEventEmitter 
  修改js调用方法 requestFile
  增加js调用方法 pauseDownload

	 

(2017-11-08)
包含以下几个方法：

    1. 下载资源文件包  requestFile
    
    2. 查询下载进度   queryFileInfo
    
    3. 解压一个文件夹压缩包 unzipFile
    
    4. 判读文件或目录是否存在 isFileExist
    
    4.1 判断音频文件是否存在时，顺便得到音频文件时长  isMediaExist
    
    
    5. 读取文件，返回字符串 readFile
    
    6. 清除目标目录   clearDestinationDir
