import Foundation
import React
import SDWebImage
import SDWebImageAVIFCoder

/// Plays an animated APNG or AVIF.
///
/// Accepted sources: `http(s)://` (SDWebImage, cached under `cacheKey` when given), `file://` or an
/// absolute path, and `asset://path`, resolved against the main bundle's resource directory. A
/// release-build `require()` already arrives as a `file://` URL into the bundle.
final class ApngAvifView: SDAnimatedImageView {
    private static let registerCoders: Void = {
        SDImageCodersManager.shared.addCoder(SDImageAPNGCoder.shared)
        SDImageCodersManager.shared.addCoder(SDImageAVIFCoder.shared)
    }()

    @objc var onLoad: RCTDirectEventBlock?
    @objc var onError: RCTDirectEventBlock?

    private var source: NSDictionary?
    private var autoPlay = true
    private var paused = false
    private var loopCount = 0
    // Bumped on every source change; a local load that finishes under an older value is dropped.
    private var generation = 0

    override init(frame: CGRect) {
        super.init(frame: frame)
        _ = Self.registerCoders
        contentMode = .scaleAspectFit
        clipsToBounds = true
        shouldCustomLoopCount = true
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    @objc func setSource(_ value: NSDictionary?) {
        if let value, let current = source, value.isEqual(current) { return }
        source = value
        load()
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
        loopCount = max(0, value?.intValue ?? 0)
        // 0 means forever, which also overrides a file that asks to play only once.
        animationRepeatCount = loopCount
    }

    @objc func setResizeMode(_ value: NSString?) {
        switch value as String? {
        case "cover": contentMode = .scaleAspectFill
        case "stretch": contentMode = .scaleToFill
        case "center": contentMode = .center
        default: contentMode = .scaleAspectFit
        }
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        updatePlayback()
    }

    private func load() {
        generation += 1
        let token = generation
        sd_cancelCurrentImageLoad()
        image = nil

        guard let uri = source?["uri"] as? String, !uri.isEmpty else { return }
        let cacheKey = source?["cacheKey"] as? String

        if uri.hasPrefix("http://") || uri.hasPrefix("https://") {
            loadRemote(uri, cacheKey: cacheKey)
        } else if let path = localPath(for: uri) {
            loadLocal(path, token: token)
        } else {
            emitError("Unsupported source: \(uri)")
        }
    }

    private func localPath(for uri: String) -> String? {
        if uri.hasPrefix("file://") {
            return URL(string: uri)?.path
        }
        if uri.hasPrefix("asset://") {
            let relative = String(uri.dropFirst("asset://".count)).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            guard let root = Bundle.main.resourcePath else { return nil }
            return (root as NSString).appendingPathComponent(relative)
        }
        if uri.hasPrefix("/") {
            return uri
        }
        return nil
    }

    private func loadRemote(_ uri: String, cacheKey: String?) {
        guard let url = URL(string: uri) else {
            emitError("Invalid URL: \(uri)")
            return
        }
        var context: [SDWebImageContextOption: Any] = [:]
        if let cacheKey, !cacheKey.isEmpty {
            context[.cacheKeyFilter] = SDWebImageCacheKeyFilter { _ in cacheKey }
        }
        autoPlayAnimatedImage = shouldPlay
        // Cancels this view's previous request, so a slow earlier download can't land later.
        sd_setImage(with: url, placeholderImage: nil, options: [.retryFailed], context: context, progress: nil) { [weak self] image, error, _, _ in
            guard let self else { return }
            if let image {
                self.didLoad(image)
            } else if let error, (error as NSError).code != SDWebImageError.Code.cancelled.rawValue {
                self.emitError(error.localizedDescription)
            }
        }
    }

    private func loadLocal(_ path: String, token: Int) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            let image = SDAnimatedImage(contentsOfFile: path)
            DispatchQueue.main.async {
                guard let self, token == self.generation else { return }
                guard let image else {
                    self.emitError(FileManager.default.fileExists(atPath: path)
                        ? "Not an APNG or AVIF image: \(path)"
                        : "File not found: \(path)")
                    return
                }
                self.autoPlayAnimatedImage = self.shouldPlay
                self.image = image
                self.didLoad(image)
            }
        }
    }

    private func didLoad(_ image: UIImage) {
        updatePlayback()
        onLoad?(["width": image.size.width * image.scale, "height": image.size.height * image.scale])
    }

    private var shouldPlay: Bool { autoPlay && !paused }

    private func updatePlayback() {
        autoPlayAnimatedImage = shouldPlay
        if shouldPlay && window != nil {
            startAnimating()
        } else {
            stopAnimating()
        }
    }

    private func emitError(_ message: String) {
        onError?(["message": message])
    }
}

@objc(ApngAvifViewManager)
final class ApngAvifViewManager: RCTViewManager {
    override static func requiresMainQueueSetup() -> Bool { true }

    override func view() -> UIView! { ApngAvifView(frame: .zero) }
}
