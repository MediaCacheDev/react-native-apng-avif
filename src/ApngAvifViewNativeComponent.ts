import type { HostComponent, ViewProps } from 'react-native';
import type {
  DirectEventHandler,
  Double,
  Int32,
  WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';

type LoadEvent = Readonly<{
  width: Double;
  height: Double;
}>;

type ErrorEvent = Readonly<{
  message: string;
}>;

export interface NativeProps extends ViewProps {
  source?: Readonly<{
    uri?: string;
    cacheKey?: string;
  }> | null;
  autoPlay?: WithDefault<boolean, true>;
  paused?: WithDefault<boolean, false>;
  /** 0 plays forever. */
  loopCount?: WithDefault<Int32, 0>;
  resizeMode?: WithDefault<'contain' | 'cover' | 'stretch' | 'center', 'contain'>;
  onLoad?: DirectEventHandler<LoadEvent>;
  onError?: DirectEventHandler<ErrorEvent>;
}

export default codegenNativeComponent<NativeProps>('RNApngAvifView') as HostComponent<NativeProps>;
