#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(ApngViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(autoPlay, BOOL)
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL)
RCT_EXPORT_VIEW_PROPERTY(loopCount, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString)

@end
