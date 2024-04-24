import Flutter
import AVFoundation
import UIKit

@available(iOS 13.0.0, *)
public class VideoThumbnailPlugin: NSObject, FlutterPlugin {
    var myResult: FlutterResult?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "plugins.justsoft.xyz/video_thumbnail", binaryMessenger: registrar.messenger())
    let instance = VideoThumbnailPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      myResult = result
      let arguments = call.arguments as? Dictionary<String, Any>
      let params = Params(
        video: arguments?["video"] as! String, headers: arguments?["headers"] as! Dictionary<String, String>?, format: arguments?["format"] as! Int, maxh: arguments?["maxh"] as! Int, maxw: arguments?["maxw"] as! Int,timeMs: arguments?["timeMs"] as! Int, quality: arguments?["quality"] as! Int);

    switch call.method {
    case "file":
        getThumbnailFile(params:params)
    case "data":
        getThumbnailData(params:params) {image,actualTime,error in
            if (image != nil) {
                result(image!.toUIImage().pngData())
            } else {
                    result(FlutterError(code: "Failed", message: "Failed to generate thumbnail", details: error));
                
            }
        }

    default:
      result(FlutterMethodNotImplemented)
    }
  }
    
    private func getThumbnailFile(params: Params) -> Void {
        let url = getURLFromFlutterPath(pathString: params.video)
        let asset = AVAsset(url: url!);
        let generator = AVAssetImageGenerator(asset: asset)
        generator.maximumSize = CGSize(width: params.maxw, height: params.maxh)
        if #available(iOS 16, *) {
            generator.generateCGImageAsynchronously(for: CMTime(seconds: Double(params.timeMs), preferredTimescale: 1000) ) {image,actualTime,error in
                if (image != nil) {
                    let uiImage = image!.toUIImage()
                    let directory = self.getCacheDirectoryPath().appendingPathComponent(UUID().uuidString + ".png")
                    try? uiImage.pngData()?.write(to: directory)
                    self.myResult!(directory.path())
                } else {
                  
                    self.myResult!(FlutterError(code: "Failed", message: "Failed to generate thumbnail", details: nil))
                    
                }
                
            }
        } else {
            // Fallback on earlier versions
        }
    }
    
    private func getCacheDirectoryPath() -> URL {
      let arrayPaths = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)
      let cacheDirectoryPath = arrayPaths[0]
      return cacheDirectoryPath
    }
    
    private func getThumbnailData(params: Params,onComplete: @escaping (CGImage?, CMTime, (any Error)?) -> Void  ) {
        let url = getURLFromFlutterPath(pathString: params.video)
        let asset = AVAsset(url: url!);
        let generator = AVAssetImageGenerator(asset: asset)
        generator.maximumSize = CGSize(width: params.maxw, height: params.maxh)
        if #available(iOS 16, *) {
            generator.generateCGImageAsynchronously(for: CMTime(seconds: Double(params.timeMs), preferredTimescale: 1000), completionHandler: onComplete )
        } else {
            // Fallback on earlier versions
        }
    }
    
    private func getURLFromFlutterPath(pathString: String) -> URL? {
        let url = pathString.hasPrefix("file://")
            ? URL(fileURLWithPath: (pathString as NSString?)?.substring(from: 7) ?? "")
        : (pathString.hasPrefix("/") ? URL(fileURLWithPath: pathString) : URL(string: pathString))
        return url
    }
    
}

extension CGImage {
    public func toUIImage()->UIImage {
        return UIImage(cgImage: self)
    }
}
