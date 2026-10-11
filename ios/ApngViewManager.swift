import Foundation
import React
import SDWebImage
import SDWebImageAVIFCoder

@objc(ApngViewManager)
class ApngViewManager: RCTViewManager {
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    override func view() -> UIView! {
        return ApngView(frame: .zero)
    }
}

final class ApngView: SDAnimatedImageView {
    private static var codersRegistered = false

    private var source: NSDictionary?
    private var autoPlay: Bool = true
    private var paused: Bool = false
    private var loopCount: Int = 0

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        if !Self.codersRegistered {
            // Register APNG coder
            SDImageCodersManager.shared.addCoder(SDImageAPNGCoder.shared)
            // Register AVIF coder for animated AVIF support
            SDImageCodersManager.shared.addCoder(SDImageAVIFCoder.shared)
            Self.codersRegistered = true
        }
        contentMode = .scaleAspectFit
        clipsToBounds = true
        autoPlayAnimatedImage = true
    }

    override func removeFromSuperview() {
        super.removeFromSuperview()
        stopAnimating()
        self.image = nil
    }

    @objc func setSource(_ value: NSDictionary?) {
        source = value
        loadImage()
    }

    @objc func setAutoPlay(_ value: Bool) {
        autoPlay = value
        updatePlayback()
    }

    @objc func setPaused(_ value: Bool) {
        paused = value
        updatePlayback()
    }

    @objc func setLoopCount(_ value: NSNumber?) {
        loopCount = value?.intValue ?? 0
        applyLoopCount()
    }

    @objc func setResizeMode(_ value: NSString?) {
        guard let mode = value as String? else {
            contentMode = .scaleAspectFit
            return
        }

        switch mode {
        case "cover":
            contentMode = .scaleAspectFill
        case "stretch":
            contentMode = .scaleToFill
        case "center":
            contentMode = .center
        default:
            contentMode = .scaleAspectFit
        }
    }

    private func loadImage() {
        stopAnimating()
        self.image = nil

        guard let source = source else { return }
        let uri = source["uri"] as? String
        guard let uriString = uri, !uriString.isEmpty else { return }

        // Handle different URI schemes
        if uriString.hasPrefix("http://") || uriString.hasPrefix("https://") {
            loadFromRemoteURL(uriString)
        } else if uriString.hasPrefix("file://") {
            let path = String(uriString.dropFirst(7))
            loadFromFilePath(path)
        } else if uriString.hasPrefix("asset://") || uriString.hasPrefix("asset:/") {
            let assetPath = uriString
                .replacingOccurrences(of: "asset://", with: "")
                .replacingOccurrences(of: "asset:/", with: "")
                .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            loadFromBundleAsset(assetPath)
        } else if isBundledResourceName(uriString) {
            loadFromBundledResource(uriString)
        } else {
            // Treat as file path
            loadFromFilePath(uriString)
        }
    }

    private func isBundledResourceName(_ uri: String) -> Bool {
        // Resource names from React Native bundler don't have slashes or colons
        if uri.contains("/") || uri.contains(":") { return false }
        // Check if it looks like a resource name (alphanumeric with underscores)
        let pattern = "^[a-zA-Z][a-zA-Z0-9_]*$"
        return uri.range(of: pattern, options: .regularExpression) != nil
    }

    private func loadFromRemoteURL(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        
        SDWebImageManager.shared.loadImage(
            with: url,
            options: [.retryFailed],
            progress: nil
        ) { [weak self] (image, data, error, cacheType, finished, imageURL) in
            guard let self = self, finished else { return }
            if let animatedImage = image as? SDAnimatedImage {
                self.image = animatedImage
            } else if let data = data {
                self.image = SDAnimatedImage(data: data)
            }
            self.applyLoopCount()
            self.updatePlayback()
        }
    }

    private func loadFromFilePath(_ path: String) {
        if FileManager.default.fileExists(atPath: path) {
            self.image = SDAnimatedImage(contentsOfFile: path)
        } else if let url = URL(string: "file://\(path)"),
                  let data = try? Data(contentsOf: url) {
            self.image = SDAnimatedImage(data: data)
        }
        applyLoopCount()
        updatePlayback()
    }

    private func loadFromBundleAsset(_ assetPath: String) {
        // Try to find in main bundle - support APNG, PNG, and AVIF
        let extensions = ["apng", "png", "avif"]
        for ext in extensions {
            if let path = Bundle.main.path(forResource: assetPath.replacingOccurrences(of: ".\(ext)", with: ""), ofType: ext) {
                self.image = SDAnimatedImage(contentsOfFile: path)
                applyLoopCount()
                updatePlayback()
                return
            }
        }
        
        // Try direct path in bundle
        let bundlePath = Bundle.main.bundlePath + "/" + assetPath
        if FileManager.default.fileExists(atPath: bundlePath) {
            self.image = SDAnimatedImage(contentsOfFile: bundlePath)
            applyLoopCount()
            updatePlayback()
        }
    }

    private func loadFromBundledResource(_ resourceName: String) {
        // React Native bundles assets with names like "src_assets_test_apng"
        // Try to find matching file in bundle
        let searchKey = resourceName.lowercased().replacingOccurrences(of: "_", with: "")
        
        if let resourcePath = Bundle.main.resourcePath {
            let fileManager = FileManager.default
            if let files = try? fileManager.contentsOfDirectory(atPath: resourcePath) {
                for file in files {
                    // Support APNG, PNG, and AVIF files
                    if file.hasSuffix(".apng") || file.hasSuffix(".png") || file.hasSuffix(".avif") {
                        let normalized = file
                            .replacingOccurrences(of: ".apng", with: "")
                            .replacingOccurrences(of: ".png", with: "")
                            .replacingOccurrences(of: ".avif", with: "")
                            .replacingOccurrences(of: "-", with: "")
                            .replacingOccurrences(of: "_", with: "")
                            .lowercased()
                        
                        if normalized.contains(searchKey) || searchKey.contains(normalized) {
                            let fullPath = resourcePath + "/" + file
                            self.image = SDAnimatedImage(contentsOfFile: fullPath)
                            applyLoopCount()
                            updatePlayback()
                            return
                        }
                    }
                }
            }
        }
        
        // Fallback: try as direct resource name
        if let path = Bundle.main.path(forResource: resourceName, ofType: "apng") {
            self.image = SDAnimatedImage(contentsOfFile: path)
        } else if let path = Bundle.main.path(forResource: resourceName, ofType: "png") {
            self.image = SDAnimatedImage(contentsOfFile: path)
        } else if let path = Bundle.main.path(forResource: resourceName, ofType: "avif") {
            self.image = SDAnimatedImage(contentsOfFile: path)
        }
        applyLoopCount()
        updatePlayback()
    }

    private func applyLoopCount() {
        if loopCount > 0 {
            animationRepeatCount = loopCount
        } else {
            animationRepeatCount = 0
        }
    }

    private func updatePlayback() {
        autoPlayAnimatedImage = autoPlay
        if paused || !autoPlay {
            stopAnimating()
        } else {
            startAnimating()
        }
    }
}
