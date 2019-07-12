'use strict'

//import { NativeModules } from 'react-native';
//module.exports = NativeModules.RNNetworkingManager;

import {
	Platform,
	NativeModules,
	NativeAppEventEmitter
} from 'react-native';

let RNModule = NativeModules.RNNetworkingManager;

export default {
  //1
	init(options){
		let opt = {
		    onReturn(){},
			...options
		};
		let fnConf = {
			confirm: opt.onReturn,
		};
		RNModule.requestFile(opt);
		this.listener && this.listener.remove();
    this.listener = NativeAppEventEmitter.addListener('confirmEvent', event => {
      fnConf[event['type']](
        event['download_sofar'],
        event['download_total'],
        event['file_name'],
        event['file_url']
      );
    });
	},
  //2
	pauseDownload(url){
		RNModule.pauseDownload(url);
	},
  //1 暂时 ios 调用
  requestFile(url, options, callback) {
    RNModule.requestFile(url, options, callback);
  },
  //2 暂时 ios 调用
  queryFileInfo2(_downloadId, callback){
    RNModule.queryFileInfo2(_downloadId, callback);
  },
  //2 暂时 ios 调用
  queryFileInfo(callback){
    RNModule.queryFileInfo(callback);
  },
	//2019-07-11 还原了原来的旧下载方法
	//原因是新的下载方法，下载的图片，在android手机相册中确东西，导致相册识别不了
	androidDownloadPicToAlbum(url, options, callback){
		RNModule.requestPic(url, options, callback);
	},


  //3
  unzipFile(zipFile, folderPath, callback){
    RNModule.unzipFile(zipFile, folderPath, callback);
  },
  //4
  isFileExist(file, callback){
    RNModule.isFileExist(file, callback);
  },
  //4.2
  isFilePublicExist(file, fileType, callback){
    RNModule.isFilePublicExist(file, fileType, callback);
  },
  //4.1
  isMediaExist(file, isNeedDuration, callback){
    RNModule.isMediaExist(file, isNeedDuration, callback);
  },
  //5
  readFile(path, callback) {
    RNModule.readFile(path, callback);
  },
  //6
  clearDestinationDir(options, callback) {
    RNModule.clearDestinationDir(options, callback);
  },
  //7
  openFile(path) {
    RNModule.openFile(path);
  }


}
