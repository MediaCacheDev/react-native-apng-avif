#import <React/RCTViewManager.h>

@interface RCT_EXTERN_REMAP_MODULE(RNApngAvifView, ApngAvifViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(source, NSDictionary)
RCT_EXPORT_VIEW_PROPERTY(autoPlay, BOOL)
RCT_EXPORT_VIEW_PROPERTY(paused, BOOL)
RCT_EXPORT_VIEW_PROPERTY(loopCount, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString)
RCT_EXPORT_VIEW_PROPERTY(onLoad, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onError, RCTDirectEventBlock)

@end
