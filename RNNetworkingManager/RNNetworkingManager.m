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

#import "AFHTTPRequestOperation.h"

#ifdef __OBJC__
#import "ZipArchive.h"
#endif

@implementation RNNetworkingManager

@synthesize bridge = _bridge;

//放在公共变量中
AFHTTPRequestOperation *operation = nil;
long fileExceptReadBytes = 0;
long fileReadBytes = 0;
NSString *fileName=@"";

RCT_EXPORT_MODULE();


//1. 下载文件
RCT_EXPORT_METHOD(requestFile:(NSString *)URLString
                  parameters:(NSDictionary *)parameters
                  callback:(RCTResponseSenderBlock)callback) {
    
    
    fileName=@"";//reset
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
      
        NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:request progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
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
}

//2. 查询文件下载进度
RCT_EXPORT_METHOD(queryFileInfo:
                  (RCTResponseSenderBlock)callback){
    
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    /*if(request != nil){
        AFHTTPRequestOperation *operation = [[AFHTTPRequestOperation alloc]initWithRequest:request];
        //[operation setOutputStream:[NSOutputStream outputStreamToFileAtPath:savedPath append:NO]];
        [operation setDownloadProgressBlock:^(NSUInteger bytesRead, long long totalBytesRead, long long totalBytesExpectedToRead) {
            float p = (float)totalBytesRead / totalBytesExpectedToRead;
            //progress(p);
            
            Boolean isSuccess = totalBytesRead >= totalBytesExpectedToRead;
            
            [output setValue:@(isSuccess) forKey:@"is_success"];
            [output setValue:@"" forKey:@"file_name"];
            //[output setValue:totalBytesRead forKey:@"download_so_far"];
            //[output setValue:totalBytesExpectedToRead forKey:@"download_total"];
            
            //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"responseObject": responseObject} forKey:@"success"];
            [output setObject:@{@"file_name": @"",
                                @"download_so_far": [NSNumber numberWithLong:totalBytesRead],
                                @"download_total": [NSNumber numberWithLong:totalBytesExpectedToRead],
                                @"id": @"",
                                @"download_id": @""
                                } forKey:@"success_ios"];
          
          
            callback(@[output]);
            
        }];
      [operation setCompletionBlockWithSuccess:^(AFHTTPRequestOperation *operation, id responseObject) {
        
        //success(responseObject);
        
      } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
        
        //failure(error);
        
      }];
      
      [operation start];
    }*/
  
  /*if(operation != nil){
    [operation setDownloadProgressBlock:^(NSUInteger bytesRead, long long totalBytesRead, long long totalBytesExpectedToRead) {
      float p = (float)totalBytesRead / totalBytesExpectedToRead;
      
      Boolean isSuccess = totalBytesRead >= totalBytesExpectedToRead;
      
      [output setValue:@(isSuccess) forKey:@"is_success"];
      [output setValue:@"" forKey:@"file_name"];
      [output setObject:@{@"file_name": @"",
                          @"download_so_far": [NSNumber numberWithLong:totalBytesRead],
                          @"download_total": [NSNumber numberWithLong:totalBytesExpectedToRead],
                          @"id": @"",
                          @"download_id": @""
                          } forKey:@"success_ios"];
      
      
      callback(@[output]);
      
    }];
   }
   */
    
    //float p = (float)fileReadBytes / (fileReadBytes + fileExceptReadBytes);
    
    //Boolean isSuccess = fileExceptReadBytes <= 0;
  
  Boolean isSuccess = fileReadBytes==fileExceptReadBytes && ![fileName isEqualToString:@""];
  
    [output setValue:@(isSuccess) forKey:@"is_success"];
    [output setValue:@"" forKey:@"file_name"];
    //[output setValue:totalBytesRead forKey:@"download_so_far"];
    //[output setValue:totalBytesExpectedToRead forKey:@"download_total"];
    
    //[output setObject:@{@"response": [NSNumber numberWithInteger:[(NSHTTPURLResponse *)response statusCode]], @"responseObject": responseObject} forKey:@"success"];
    [output setObject:@{@"file_name": fileName,//@""
                        @"download_so_far": [NSNumber numberWithLong:fileReadBytes],
                        //@"download_total": [NSNumber numberWithLong:(fileReadBytes + fileExceptReadBytes)],
                        @"download_total": [NSNumber numberWithLong:(fileExceptReadBytes)],
                        @"id": @"",
                        @"download_id": @"",
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
    if( [SSZipArchive unzipFileAtPath:zipPath toDestination:destinationPath] ){
        
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

//5. 读取文件
RCT_EXPORT_METHOD(readFile:(NSString *)path
                  callback:(RCTResponseSenderBlock) callback) {
    NSMutableDictionary *output = [[NSMutableDictionary alloc] init];
    
    //NSString *str = [NSString stringWithContentsOfFile:path];
    //UTF-8编码
    NSString *content = [NSString stringWithContentsOfFile:path encoding:NSUTF8StringEncoding error:nil];
    NSLog(@"%@",content);
    
    [output setValue:content forKey:@"content"];
    
    callback(@[output]);
}



@end
