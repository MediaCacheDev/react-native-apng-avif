//
//  RNApngAvifViewComponentView.mm
//  Fabric component view for ApngAvifView (new architecture)
//

#ifdef RCT_NEW_ARCH_ENABLED

#import <UIKit/UIKit.h>
#import <SDWebImage/SDWebImage.h>

#import <React/RCTComponent.h>
#import <React/RCTConversions.h>
#import <React/RCTViewComponentView.h>
#import <React/RCTViewManager.h>

#import <react/renderer/components/RNApngAvifSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNApngAvifSpec/EventEmitters.h>
#import <react/renderer/components/RNApngAvifSpec/Props.h>
#import <react/renderer/components/RNApngAvifSpec/RCTComponentViewHelpers.h>

// The Swift view. The header's own module imports are skipped in Objective-C++, hence the UIKit,
// SDWebImage and React imports above.
#if __has_include(<react_native_apng_avif/react_native_apng_avif-Swift.h>)
#import <react_native_apng_avif/react_native_apng_avif-Swift.h>
#else
#import "react_native_apng_avif-Swift.h"
#endif

using namespace facebook::react;

static const std::shared_ptr<const RNApngAvifViewProps> &RNApngAvifViewDefaultProps()
{
  static const auto defaultProps = std::make_shared<const RNApngAvifViewProps>();
  return defaultProps;
}

@interface RNApngAvifViewComponentView : RCTViewComponentView
@end

@implementation RNApngAvifViewComponentView {
  ApngAvifView *_imageView;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<RNApngAvifViewComponentDescriptor>();
}

// A view holds its decoded animation, so each mount gets a new one.
+ (BOOL)shouldBeRecycled
{
  return NO;
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    _props = RNApngAvifViewDefaultProps();
    [self createImageView];
  }
  return self;
}

- (void)createImageView
{
  _imageView = [[ApngAvifView alloc] initWithFrame:self.bounds];
  [self forwardEvents];
  self.contentView = _imageView;
}

// Older React Native versions ignore shouldBeRecycled and recycle every component view.
- (void)prepareForRecycle
{
  [super prepareForRecycle];
  [self createImageView];
  _props = RNApngAvifViewDefaultProps();
}

// Only the props that changed, as the old architecture sets them. The view's defaults match the
// spec's.
- (void)updateProps:(const Props::Shared &)props oldProps:(const Props::Shared &)oldProps
{
  const auto &previous = *std::static_pointer_cast<const RNApngAvifViewProps>(_props);
  const auto &next = *std::static_pointer_cast<const RNApngAvifViewProps>(props);

  if (previous.autoPlay != next.autoPlay) [_imageView setAutoPlay:next.autoPlay];
  if (previous.paused != next.paused) [_imageView setPaused:next.paused];
  if (previous.loopCount != next.loopCount) [_imageView setLoopCount:@(next.loopCount)];
  if (previous.resizeMode != next.resizeMode) {
    [_imageView setResizeMode:RCTNSStringFromString(toString(next.resizeMode))];
  }
  if (previous.source.uri != next.source.uri || previous.source.cacheKey != next.source.cacheKey) {
    [_imageView setSource:[self sourceDictionary:next.source]];
  }

  [super updateProps:props oldProps:oldProps];
}

- (nullable NSDictionary *)sourceDictionary:(const RNApngAvifViewSourceStruct &)source
{
  if (source.uri.empty()) return nil;
  NSMutableDictionary *dictionary = [NSMutableDictionary dictionaryWithObject:RCTNSStringFromString(source.uri)
                                                                       forKey:@"uri"];
  if (!source.cacheKey.empty()) dictionary[@"cacheKey"] = RCTNSStringFromString(source.cacheKey);
  return dictionary;
}

- (std::shared_ptr<const RNApngAvifViewEventEmitter>)imageEventEmitter
{
  return std::static_pointer_cast<const RNApngAvifViewEventEmitter>(_eventEmitter);
}

- (void)forwardEvents
{
  __weak RNApngAvifViewComponentView *weakSelf = self;
  _imageView.onLoad = ^(NSDictionary *body) {
    auto emitter = [weakSelf imageEventEmitter];
    if (emitter) emitter->onLoad({.width = [body[@"width"] doubleValue], .height = [body[@"height"] doubleValue]});
  };
  _imageView.onError = ^(NSDictionary *body) {
    auto emitter = [weakSelf imageEventEmitter];
    if (emitter) emitter->onError({.message = RCTStringFromNSString(body[@"message"] ?: @"")});
  };
}

@end

// Older React Native versions look the class up by this name instead of codegenConfig.ios.componentProvider.
extern "C" Class<RCTComponentViewProtocol> RNApngAvifViewCls(void)
{
  return RNApngAvifViewComponentView.class;
}

#endif
