require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-apng-player"
  s.version      = package['version']
  s.summary      = package['description']
  s.homepage     = "https://github.com/user/react-native-apng-player"
  s.license      = package['license']
  s.authors      = { "Author" => "author@example.com" }
  s.platforms    = { :ios => "13.0" }
  s.source       = { :git => "https://github.com/user/react-native-apng-player.git", :tag => "v#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.swift_version = "5.0"

  s.dependency "React-Core"
  s.dependency "SDWebImage"
  s.dependency "SDWebImageAVIFCoder"
end
