require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'react-native-apng-avif'
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  s.authors      = { 'Daniel Kasprowicz' => 'danielkpl2@gmail.com' }
  s.platforms    = { :ios => '13.4' }
  s.source       = { :git => 'https://github.com/danielkpl2/react-native-apng-avif.git', :tag => "v#{s.version}" }

  s.source_files = 'ios/**/*.{h,m,mm,swift}'
  s.swift_version = '5.7'

  s.dependency 'React-Core'
  s.dependency 'SDWebImage', '~> 5.21'
  s.dependency 'SDWebImageAVIFCoder', '~> 0.11'
  # Decode-only dav1d backend. libavif's default subspec is libaom, which bundles an encoder too
  # and decodes more slowly.
  s.dependency 'libavif/libdav1d'
end
