//
//  RNNetworkingManager.m
//  RNNetworking
//
//  Created by Erdem Başeğmez on 18.06.2015.
//  Copyright (c) 2015 Facebook. All rights reserved.
//

#import "RNNetworkingManager.h"
#import "AFHTTPSessionManager.h"
#import "AFURLSessionManager.h"
#import <React/RCTView.h>
#import "UIView+React.h"
#import <React/RCTEventDispatcher.h>

#import "AFHTTPRequestOperation.h"

#ifdef __OBJC__
#import "ZipArchive2.h"
#endif

@implementation RNNetworkingManager

@synthesize bridge = _bridge;

//放在公共变量中
AFHTTPRequestOperation *operation = nil;
long fileExceptReadBytes = 0;
long fileReadBytes = 0;
NSString *fileName=@"";
NSString *fileUrl = @"";

AFURLSessionManager *manager;
NSURLSessionDownloadTask * downloadTask;
NSData *resumData;
NSMutableURLRequest *request;

RCT_EXPORT_MODULE();

//1. 下载文件
RCT_EXPORT_METHOD(requestFile: (NSDictionary *)options){
    NSString *method;
    //  NSDictionary *headers;
    NSDictionary *data;
    NSString *destinationDir = @".ys";
    NSString *URLString;
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    
    if ([options count] != 0) {
        URLString = options[@"url"];
        NSURL *url = [NSURL URLWithString:URLString];
        
        method = options[@"method"];
        //    headers = parameters[@"headers"];
        data = options[@"data"];
        
        destinationDir = options[@"destinationDir"];
        
        NSString * docsdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *dataFilePath = [docsdir stringByAppendingPathComponent:destinationDir];
        
        NSFileManager *fileManager = [NSFileManager defaultManager];
        
        BOOL isDir = NO;
        
        // fileExistsAtPath 判断一个文件或目录是否有效，isDirectory判断是否一个目录
        BOOL existed = [fileManager fileExistsAtPath:dataFilePath isDirectory:&isDir];
        
        if ( !(isDir == YES && existed == YES) ) {
            
            // 在 Document 目录下创建一个 head 目录
            [fileManager createDirectoryAtPath:dataFilePath withIntermediateDirectories:YES attributes:nil error:nil];
        }
        
        if (options[@"token"]) {
            // check body content and find token
            NSString *user = options[@"token"];
            //    NSString *password;
            NSString *host = [url host];
            NSNumber *port = [url port];
            NSString *protocol = [url scheme];
            NSURLProtectionSpace *protectionSpace = [[NSURLProtectionSpace alloc] initWithHost:host port:[port integerValue] protocol:protocol realm:nil authenticationMethod:NSURLAuthenticationMethodHTTPBasic];
            
            NSURLCredential *defaultCredential = [NSURLCredential credentialWithUser:user password:NULL persistence:NSURLCredentialPersistencePermanent];
            
            NSURLCredentialStorage *credentials = [NSURLCredentialStorage sharedCredentialStorage];
            [credentials setDefaultCredential:defaultCredential forProtectionSpace:protectionSpace];
            
            [configuration setURLCredentialStorage: credentials];
        }
        
    } else {
        method = @"GET";
    }
    //method = @"POST";
    
    if(manager == nil){
        manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
    }
    
    
    Boolean isExist = false;
    for(id obj in manager.downloadTasks)
    {
        NSURLSessionDownloadTask *task = (NSURLSessionDownloadTask *)obj;
        
        NSArray *array = [task.currentRequest.URL.absoluteString componentsSeparatedByString:@"?"];
        NSLog(@"task url =    %@", array[0]);
        NSLog(@"post url =    %@", URLString);
        if([array[0] isEqual:URLString]){
            [task resume];
            downloadTask = task;
            fileUrl = URLString;
            isExist = true;
        }else {
            //fileUrl = URLString;
            [task suspend];
        }
    }
    
    /*if(downloadTask != nil){
     if( [fileUrl isEqualToString: URLString] ){
     [downloadTask resume];
     return;
     }
     [downloadTask suspend];
     }*/
    
    if(isExist){
        return;
    }
    
    fileUrl = URLString;
    //request = [[AFJSONRequestSerializer serializer] requestWithMethod:method URLString:URLString parameters:options error:nil];
    request = [[AFJSONRequestSerializer serializer] requestWithMethod:method URLString:URLString parameters:nil error:nil];
    downloadTask = [manager downloadTaskWithRequest:request progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        // find the Documents directory for the app
        NSURL *documentsDirectoryURL = [[NSFileManager defaultManager] URLForDirectory:NSDocumentDirectory inDomain:NSUserDomainMask appropriateForURL:nil create:NO error:nil];
        // set destination to Documents/response.suggestedFilename
        
        NSString* str=[[NSString alloc]initWithFormat:@"%@%@",destinationDir,[response suggestedFilename]];
        fileName=str;
        NSURL* tmp= [documentsDirectoryURL URLByAppendingPathComponent:str];
        return tmp;
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        if (error) {
            [output setObject:[error localizedDescription] forKey:@"error"];
        } else {
            //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"filePath": [filePath lastPathComponent]} forKey:@"success"];
            //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"filePath": [filePath path]} forKey:@"success"];
            NSLog(@"relativePath =   %@", [filePath relativePath]);
            NSLog(@"path =   %@", [filePath path]);
            NSLog(@"absoluteString =   %@", [filePath absoluteString]);
            NSLog(@"lastPathComponent =    %@", [filePath lastPathComponent]);
        }
        //callback(@[output]);
    }];
    [downloadTask resume];
    
    
    operation = [[AFHTTPRequestOperation alloc]initWithRequest:request];
    [operation setDownloadProgressBlock:^(NSUInteger bytesRead, long long totalBytesRead, long long totalBytesExpectedToRead) {
        
        NSString *url = [operation.request.URL absoluteString];
        NSArray *array = [url componentsSeparatedByString:@"?"];
        
        NSLog(@"url =    %@", array[0]);
        
        //[output setObject:@{@"bytesRead": [NSNumber numberWithLong:bytesRead], @"bytesRead": [NSNumber numberWithLong:totalBytesRead], @"totalBytesExpectedToRead": [NSNumber numberWithLong:totalBytesExpectedToRead]} forKey:@"setDownloadProgressBlock"];
        
        fileUrl = array[0];
        fileReadBytes = totalBytesRead;
        fileExceptReadBytes = totalBytesExpectedToRead;
        if(totalBytesRead == totalBytesExpectedToRead){
            [self commonEvent:fileUrl withData:totalBytesRead withData:totalBytesExpectedToRead];
        }
    }];
    [operation setCompletionBlockWithSuccess:^(AFHTTPRequestOperation *operation, id responseObject) {
        
        //success(responseObject);
        [output setValue:@"=======================" forKey:@"setCompletionBlockWithSuccess"];
        
        
    } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
        
        //failure(error);
        [output setValue:error forKey:@"failure"];
        
    }];
    
    [operation start];
    
}

//2. 暂停下载
RCT_EXPORT_METHOD(pauseDownload:
                  (NSString *)URLString){
    [downloadTask suspend];
    
    return;
    NSLog(@"+++++++++++++++++++取消");
    //[self.downloadTask cancel];
    
    //恢复下载的数据!=文件数据，也就是记录开始下载的位置的相关信息，倘若是已下载的文件数据，就会存在内存增大的问题
    [downloadTask cancelByProducingResumeData:^(NSData * _Nullable resumeData) {
        resumData = resumeData;
    }];
}

RCT_EXPORT_METHOD(resumeDownload: (NSString *)URLString){
    if(resumData)
    {
        //downloadTask = [manager downloadTaskWithResumeData:resumData];
    }
}



- (void)commonEvent: (NSString *) url withData: (long) sofar withData: (long) total{
    NSMutableDictionary *dic=[[NSMutableDictionary alloc]init];
    [dic setValue:@"confirm" forKey:@"type"];
    
    NSNumber *_sofar = [NSNumber numberWithLong:sofar];
    NSNumber *_total = [NSNumber numberWithLong:total];
    [dic setValue:[_sofar stringValue] forKey:@"download_sofar"];
    [dic setValue:[_total stringValue] forKey:@"download_total"];
    [dic setValue:@"" forKey:@"file_name"];
    [dic setValue:url forKey:@"file_url"];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.bridge.eventDispatcher sendAppEventWithName:@"confirmEvent" body:dic];
    });
}

//1. 下载文件
/*RCT_EXPORT_METHOD(requestFile:(NSString *)URLString
 parameters:(NSDictionary *)parameters
 callback:(RCTResponseSenderBlock)callback) {
 
 
 fileName=@"";//reset
 fileUrl = URLString;
 NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
 NSURL *url = [NSURL URLWithString:URLString];
 
 NSString *method;
 //  NSDictionary *headers;
 NSDictionary *data;
 NSString *destinationDir = @".ys";
 
 if ([parameters count] != 0) {
 method = parameters[@"method"];
 //    headers = parameters[@"headers"];
 data = parameters[@"data"];
 
 destinationDir = parameters[@"destinationDir"];
 
 NSString * docsdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
 NSString *dataFilePath = [docsdir stringByAppendingPathComponent:destinationDir];
 
 NSFileManager *fileManager = [NSFileManager defaultManager];
 
 BOOL isDir = NO;
 
 // fileExistsAtPath 判断一个文件或目录是否有效，isDirectory判断是否一个目录
 BOOL existed = [fileManager fileExistsAtPath:dataFilePath isDirectory:&isDir];
 
 if ( !(isDir == YES && existed == YES) ) {
 
 // 在 Document 目录下创建一个 head 目录
 [fileManager createDirectoryAtPath:dataFilePath withIntermediateDirectories:YES attributes:nil error:nil];
 }
 
 
 } else {
 method = @"GET";
 }
 
 NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
 
 if (data[@"token"]) {
 // check body content and find token
 NSString *user = data[@"token"];
 //    NSString *password;
 NSString *host = [url host];
 NSNumber *port = [url port];
 NSString *protocol = [url scheme];
 NSURLProtectionSpace *protectionSpace = [[NSURLProtectionSpace alloc] initWithHost:host port:[port integerValue] protocol:protocol realm:nil authenticationMethod:NSURLAuthenticationMethodHTTPBasic];
 
 NSURLCredential *defaultCredential = [NSURLCredential credentialWithUser:user password:NULL persistence:NSURLCredentialPersistencePermanent];
 
 NSURLCredentialStorage *credentials = [NSURLCredentialStorage sharedCredentialStorage];
 [credentials setDefaultCredential:defaultCredential forProtectionSpace:protectionSpace];
 
 [configuration setURLCredentialStorage: credentials];
 }
 
 AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
 
 if ([method isEqualToString:@"GET"]) {
 // request serializer: json
 // response serializer: formdata multipart file
 
 // input: url, parameters
 // request: json: get file
 // response: file
 // output: filepath
 
 //    NSDictionary *parameters = @{};
 NSMutableURLRequest *request = [[AFJSONRequestSerializer serializer] requestWithMethod:method URLString:URLString parameters:parameters error:nil];
 
 downloadTask = [manager downloadTaskWithRequest:request progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
 // find the Documents directory for the app
 NSURL *documentsDirectoryURL = [[NSFileManager defaultManager] URLForDirectory:NSDocumentDirectory inDomain:NSUserDomainMask appropriateForURL:nil create:NO error:nil];
 // set destination to Documents/response.suggestedFilename
 
 NSString* str=[[NSString alloc]initWithFormat:@"%@%@",destinationDir,[response suggestedFilename]];
 fileName=str;
 NSURL* tmp= [documentsDirectoryURL URLByAppendingPathComponent:str];
 return tmp;
 } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
 if (error) {
 [output setObject:[error localizedDescription] forKey:@"error"];
 } else {
 //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"filePath": [filePath lastPathComponent]} forKey:@"success"];
 [output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"filePath": [filePath path]} forKey:@"success"];
 NSLog(@"relativePath =   %@", [filePath relativePath]);
 NSLog(@"path =   %@", [filePath path]);
 NSLog(@"absoluteString =   %@", [filePath absoluteString]);
 NSLog(@"lastPathComponent =    %@", [filePath lastPathComponent]);
 }
 callback(@[output]);
 }];
 [downloadTask resume];
 
 
 operation = [[AFHTTPRequestOperation alloc]initWithRequest:request];
 [operation setDownloadProgressBlock:^(NSUInteger bytesRead, long long totalBytesRead, long long totalBytesExpectedToRead) {
 fileReadBytes = totalBytesRead;
 fileExceptReadBytes = totalBytesExpectedToRead;
 }];
 [operation setCompletionBlockWithSuccess:^(AFHTTPRequestOperation *operation, id responseObject) {
 
 //success(responseObject);
 
 } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
 
 //failure(error);
 
 }];
 
 [operation start];
 
 
 } else if ([method isEqualToString:@"POST"]) {
 // request serializer: formdata multipart file
 // response: response object (json)
 
 // input: url, filepath
 // request: post file
 // response: success or error JSON
 // output: success or error
 
 // multi-part upload task
 [manager setResponseSerializer:[AFJSONResponseSerializer serializer]];
 NSURL *documentsDirectoryURL = [[NSFileManager defaultManager] URLForDirectory:NSDocumentDirectory inDomain:NSUserDomainMask appropriateForURL:nil create:NO error:nil];
 
 NSURL *fileURL = [documentsDirectoryURL URLByAppendingPathComponent:data[@"file"] ];
 
 [manager setResponseSerializer:[AFJSONResponseSerializer serializer]];
 
 NSMutableURLRequest *request = [[AFHTTPRequestSerializer serializer] multipartFormRequestWithMethod:method URLString:URLString parameters:nil constructingBodyWithBlock:^(id<AFMultipartFormData> formData) {
 
 [formData appendPartWithFileURL:fileURL name:@"file" fileName:@"fileName.mp4" mimeType:@"video/mp4" error:nil];
 } error:nil];
 
 NSProgress *progress;
 NSURLSessionUploadTask *uploadTask = [manager uploadTaskWithStreamedRequest:request progress:&progress completionHandler:^(NSURLResponse *response, id responseObject, NSError *error) {
 if (error) {
 [output setObject:[error localizedDescription] forKey:@"error"];
 } else {
 [output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"responseObject": responseObject} forKey:@"success"];
 }
 callback(@[output]);
 }];
 [uploadTask resume];
 }
 }*/

//2. 查询文件下载进度
RCT_EXPORT_METHOD(queryFileInfo:
                  (RCTResponseSenderBlock)callback){
    
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    Boolean isSuccess = fileReadBytes==fileExceptReadBytes && ![fileName isEqualToString:@""];
    
    [output setValue:@(isSuccess) forKey:@"is_success"];
    [output setValue:@"" forKey:@"file_name"];
    //[output setValue:totalBytesRead forKey:@"download_sofar"];
    //[output setValue:totalBytesExpectedToRead forKey:@"download_total"];
    
    //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"responseObject": responseObject} forKey:@"success"];
    [output setObject:@{
                        @"file_name": fileName,
                        @"download_sofar": [NSNumber numberWithLong:fileReadBytes],
                        @"download_total": [NSNumber numberWithLong:(fileExceptReadBytes)],
                        //@"download_total": [NSNumber numberWithLong:(fileReadBytes + fileExceptReadBytes)],
                        @"id": @"",
                        @"download_id": @"",
                        @"file_url": fileUrl,
                        @"is_success": @(isSuccess)
                        } forKey:@"success_ios"];
    
    callback(@[output]);
    
}

//3. 解压一个文件压缩包
RCT_EXPORT_METHOD(unzipFile:(NSString *)zipFile
                  folderPath:(NSString *)folderPath
                  callback:(RCTResponseSenderBlock)callback){
    
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    // 解压
    NSString *zipPath = zipFile;
    NSString *destinationPath = folderPath;
    
    
    NSString * docsdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *dataFilePath = [docsdir stringByAppendingPathComponent:destinationPath];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    BOOL isDir = NO;
    
    // fileExistsAtPath 判断一个文件或目录是否有效，isDirectory判断是否一个目录
    BOOL existed = [fileManager fileExistsAtPath:dataFilePath isDirectory:&isDir];
    
    if ( !(isDir == YES && existed == YES) ) {
        
        // 在 Document 目录下创建一个 head 目录
        [fileManager createDirectoryAtPath:dataFilePath withIntermediateDirectories:YES attributes:nil error:nil];
    }
    zipPath=[[NSString alloc]initWithFormat:@"%@/%@",docsdir,zipPath];
    destinationPath=[[NSString alloc]initWithFormat:@"%@/%@",docsdir,destinationPath];
    if( [SSZipArchive2 unzipFileAtPath:zipPath toDestination:destinationPath] ){
        
        [output setValue:@"true" forKey:@"success"];
        callback(@[output]);
        //
        [fileManager removeItemAtPath:zipPath error:nil];
    }
    
}


//根据文件名，读取
-(NSString *)get_filename:(NSString *)name
{
    NSString *result = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0]
                        stringByAppendingPathComponent:name];
    
    NSLog(@"get_filename: %@", result);
    return result;
}
//根据文件名，读取
-(NSString *)get_filename2:(NSString *)name
{
    NSString *result = [[NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:name];
    
    NSLog(@"get_filename: %@", result);
    return result;
}

//4. 判断文件是否在本机已存在
RCT_EXPORT_METHOD(isFileExist:(NSString *)file
                  callback:(RCTResponseSenderBlock)callback) {
    
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    
    NSFileManager *file_manager = [NSFileManager defaultManager];
    NSString *full_path = [self get_filename:file];
    if( [file_manager fileExistsAtPath:full_path] ){
        /*result.putBoolean("success", true);
         result.putString("full_path", f.getAbsolutePath());*/
        //[output setObject:@{@"full_path": file} forKey:@"success"];
        [output setValue:full_path forKey:@"full_path"];
        [output setValue:@"true" forKey:@"success"];
    }else {
        
        //[output setObject:[error localizedDescription] forKey:@"error"];
        //[output setObject:@{@"文件不存在"} forKey:@"error"];
        full_path = [self get_filename2:file];
        if([file_manager fileExistsAtPath:full_path]){
            [output setValue:full_path forKey:@"full_path"];
            [output setValue:@"true" forKey:@"success"];
        }else {
            
            [output setValue:@"false" forKey:@"success"];
        }
        
    }
    callback(@[output]);
}

-(float)durationWithVideo:(NSURL *)videoUrl{
    NSURL *mp3_url = [[NSURL alloc] initFileURLWithPath:videoUrl];
    //NSDictionary *opts = [NSDictionary dictionaryWithObject:@(NO) forKey:AVURLAssetPreferPreciseDurationAndTimingKey];
    //AVURLAsset *urlAsset = [AVURLAsset URLAssetWithURL:videoUrl options:opts]; // 初始化视频媒体文件
    AVURLAsset *urlAsset = [AVURLAsset URLAssetWithURL:mp3_url options:nil];
    
    float second = 0.0;
    second = urlAsset.duration.value / urlAsset.duration.timescale; // 获取视频总时长,单位秒
    NSLog(@"duration:%f", second);
    
    return second;
}

//4.1 判断音频文件是否在本机已存在 并获得音频时长
RCT_EXPORT_METHOD(isMediaExist:(NSString *)file
                  isNeedDuration:(BOOL)isNeedDuration
                  callback:(RCTResponseSenderBlock)callback) {
    
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    
    NSFileManager *file_manager = [NSFileManager defaultManager];
    NSString *full_path = file;
    if([file_manager fileExistsAtPath:file]){
        [output setValue:full_path forKey:@"full_path"];
        [output setValue:@"true" forKey:@"success"];
    }else {
        full_path = [self get_filename:file];
        if( [file_manager fileExistsAtPath:full_path] ){
            [output setValue:full_path forKey:@"full_path"];
            [output setValue:@"true" forKey:@"success"];
        }else {
            full_path = [self get_filename2:file];
            if([file_manager fileExistsAtPath:full_path]){
                [output setValue:full_path forKey:@"full_path"];
                [output setValue:@"true" forKey:@"success"];
            }else {
                [output setValue:@"false" forKey:@"success"];
            }
        }
    }
    
    double duration = 0;
    if(isNeedDuration && full_path){
        
        duration = [self durationWithVideo:full_path];
        
        [output setValue:@(duration) forKey:@"duration"];
    }
    
    callback(@[output]);
}

//5. 读取文件
RCT_EXPORT_METHOD(readFile:(NSString *)path
                  callback:(RCTResponseSenderBlock) callback) {
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    //NSString *str = [NSString stringWithContentsOfFile:path];
    //UTF-8编码
    NSString *content = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:nil];
    //NSLog(@"%@",content);
    
    [output setValue:content forKey:@"content"];
    
    callback(@[output]);
}


//6. 清空缓存
RCT_EXPORT_METHOD(clearDestinationDir:(NSDictionary *)parameters
                  callback:(RCTResponseSenderBlock)callback) {
    
    fileName=@"";//reset
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    NSString *method;
    //  NSDictionary *headers;
    NSDictionary *data;
    //NSString *destinationDir = @".ys";
    NSString *destinationDir = @"";
    
    if ([parameters count] != 0) {
        
        destinationDir = parameters[@"destinationDir"];
        
        NSString * docsdir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *dataFilePath = [docsdir stringByAppendingPathComponent:destinationDir];
        
        NSFileManager *fileManager = [NSFileManager defaultManager];
        
        BOOL isDir = NO;
        // fileExistsAtPath 判断一个文件或目录是否有效，isDirectory判断是否一个目录
        BOOL existed = [fileManager fileExistsAtPath:dataFilePath isDirectory:&isDir];
        if (isDir == YES && existed == YES) {
            
            [fileManager removeItemAtPath:dataFilePath error:nil];
            
        }
        
        [output setValue:dataFilePath forKey:@"path1"];
        //[output setValue:content forKey:@"path2"];
    }
    
    callback(@[output]);
}

- (UIViewController *)documentInteractionControllerViewControllerForPreview:(UIDocumentInteractionController *)interactionController

{
    return self;
}

//7. 打开文件
RCT_EXPORT_METHOD(openFile:(NSString *)path) {
    //url 为需要调用第三方打开的文件地址
    //NSURL *url = [NSURL fileURLWithPath:_dict[@"path"]];
    NSURL *url = [NSURL fileURLWithPath:path];
    /*_documentInteractionController = [UIDocumentInteractionController
     interactionControllerWithURL:url];
     [_documentInteractionController setDelegate:self];
     
     //[_documentInteractionController presentOpenInMenuFromRect:CGRectZero inView:self.view animated:YES];
     
     [_documentInteractionController presentOpenInMenuFromRect:CGRectZero inView:nil animated:YES];
     */
    
    //[self.class presentViewController:_documentInteractionController animated:YES completion:nil];
    
    
    //self.documentVC = [UIDocumentInteractionController interactionControllerWithURL:url];
    //[self.documentVC setDelegate:self];
    //BOOL canOpen =  [self.documentVC presentOpenInMenuFromRect:CGRectZero inView:self.view animated:YES];
    self.documentInteractionController = [UIDocumentInteractionController interactionControllerWithURL:url];
    
    //CGRect navRect = self.view.navigationBar.frame;
    //navRect.size =CGSizeMake(self.view.width,40.0f);
    //[self.documentInteractionController presentOptionsMenuFromRect:navRectinView:self.view animated:YES];
    
    // 得到当前应用程序的主要窗口
    UIWindow * window = [[UIApplication sharedApplication] keyWindow];
    // 获取窗口的当前显示视图
    UIView *frontView = [[window subviews] objectAtIndex:0];
    
    [self.documentInteractionController setDelegate:self];
    //BOOL canOpen = [self.documentInteractionController presentPreviewAnimated:YES];
    BOOL canOpen =  [self.documentInteractionController presentOpenInMenuFromRect:CGRectMake(200, 700, 10, 10) inView:frontView animated:YES];
    
    // 返回NO说明没有可以打开该文件的爱屁屁, 友情提示一下
    if (canOpen == NO) {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"提示" message:@"没有找到可以打开该文件的应用" delegate:nil cancelButtonTitle:@"确定" otherButtonTitles:nil, nil];
        [alert show];
    }
    
    
}

@end
