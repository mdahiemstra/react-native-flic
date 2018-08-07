
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <React/RCTEventEmitter.h>
#import <fliclib/fliclib.h>

@interface RNFlic : RCTEventEmitter <SCLFlicManagerDelegate, SCLFlicButtonDelegate, RCTBridgeModule>

@end
  