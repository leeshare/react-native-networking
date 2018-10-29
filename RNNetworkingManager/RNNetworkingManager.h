//
//  RNNetworkingManager.h
//  RNNetworking
//
//  Created by Erdem Başeğmez on 18.06.2015.
//  Copyright (c) 2015 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "RCTBridgeModule.h"
#import "AVFoundation/AVFoundation.h"

//UIViewController
//
@interface RNNetworkingManager : NSObject <RCTBridgeModule>

@end

@interface RNNetworkingManager ()<UIDocumentInteractionControllerDelegate>

@property (nonatomic, strong) UIDocumentInteractionController *documentInteractionController;

@end
