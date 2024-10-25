#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE (TextReader, NSObject)

RCT_EXTERN_METHOD(read
                  : (NSString *)imgPath withOptions
                  : (NSDictionary *)options withResolver
                  : (RCTPromiseResolveBlock)resolve withRejecter
                  : (RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
