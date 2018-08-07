
#import "RNFlic.h"

@implementation RNFlic {
    bool hasListeners;
}

+ (BOOL) requiresMainQueueSetup {
    return YES;
}

NSString *eventNamespace = @"FLIC";
NSString *appID = @"";
NSString *appSecret = @"";

- (instancetype) init {
    self = [super init];

    if (self) {
        [SCLFlicManager configureWithDelegate:self defaultButtonDelegate:self appID:appID appSecret:appSecret backgroundExecution:YES];
    }

	return self;
}

- (void) startObserving {
    hasListeners = YES;
}

- (void) stopObserving {
    hasListeners = NO;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents {
    return @[eventNamespace];
}

- (void) sendEventMessage:(NSDictionary *)body {
    NSLog(@"FLIC EVENT %@", body);
    
    if (hasListeners) {
        [self sendEventWithName:eventNamespace body: body];
    }
}

RCT_EXPORT_METHOD(getKnownButtons:(NSString *)name) {
    [self sendEventMessage: @{
        @"event": @"GET_KNOWN_BUTTONS",
        @"buttons": @([SCLFlicManager sharedManager].knownButtons.count)
    }];
}

RCT_EXPORT_METHOD(makeCall:(NSString *)number) {
    // Not supported in iOS.
}

RCT_EXPORT_METHOD(searchButtons:(NSString *)name) {
    [[SCLFlicManager sharedManager] startScan];
    [self sendEventMessage: @{
        @"event": @"SEARCH_BUTTON_START"
    }];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(15.0f * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self sendEventMessage: @{
            @"event": @"SEARCH_BUTTON_TIMEOUT"
        }];
        [[SCLFlicManager sharedManager] stopScan];
    });
}

// -- SCLFlicManagerDelegate --

- (void) flicManager:(SCLFlicManager *)manager didDiscoverButton:(SCLFlicButton *)button withRSSI:(NSNumber *)RSSI; {
    [[SCLFlicManager sharedManager] stopScan];
    [button connect];
    [self sendEventMessage: @{
        @"event": @"BUTTON_READY",
        @"rssi": RSSI
    }];
}

- (void) flicManagerDidRestoreState:(SCLFlicManager *)manager; {
    [self sendEventMessage: @{
        @"event": @"MANAGER_RESTORED"
    }];
}

- (void) flicManager:(SCLFlicManager *)manager didChangeBluetoothState:(SCLFlicManagerBluetoothState)state; {
    [self sendEventMessage: @{
        @"event": @"BLUETOOTH_SWITCHED_STATE",
        @"state": @(state)
    }];
}

- (void) flicManager:(SCLFlicManager *)manager didForgetButton:(NSUUID *)buttonIdentifier error:(NSError *)error; {
    [self sendEventMessage: @{
        @"event": @"BUTTON_PRESSED",
        @"buttonId": buttonIdentifier,
        @"error": error
    }];
}

// -- SCLFlicButtonDelegate --

- (void) flicButtonDidConnect:(SCLFlicButton *)button {
    [button connect];
    [self sendEventMessage: @{
        @"event": @"BUTTON_CONNECTED"
    }];
}

- (void) flicButtonIsReady:(SCLFlicButton *)button {
    [button connect];
    [self sendEventMessage: @{
        @"event": @"BUTTON_READY"
    }];
}

- (void) flicButton:(SCLFlicButton *)button didDisconnectWithError:(NSError *)error {
    [self sendEventMessage: @{
        @"event": @"BUTTON_DISCONNECTED",
        @"error": error
    }];
}

- (void) flicButton:(SCLFlicButton *)button didReceiveButtonClick:(BOOL)queued age:(NSInteger)age {
    [self sendEventMessage: @{
        @"event": @"BUTTON_PRESSED",
        @"age": @(age)
    }];
}

@end
  
