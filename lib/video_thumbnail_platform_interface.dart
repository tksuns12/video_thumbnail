import 'package:flutter/services.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:video_thumbnail/video_thumbnail.dart';

import 'video_thumbnail_method_channel.dart';

abstract class VideoThumbnailPlatform extends PlatformInterface {
  /// Constructs a VideoThumbnailPlatform.
  VideoThumbnailPlatform() : super(token: _token);


  static final Object _token = Object();

  static VideoThumbnailPlatform _instance = MethodChannelVideoThumbnail();

  /// The default instance of [VideoThumbnailPlatform] to use.
  ///
  /// Defaults to [MethodChannelVideoThumbnail].
  static VideoThumbnailPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [VideoThumbnailPlatform] when
  /// they register themselves.
  static set instance(VideoThumbnailPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String> thumbnailFile(
      {required String video,
      Map<String, String>? headers,
      String? thumbnailPath,
      ImageFormat imageFormat = ImageFormat.PNG,
      int maxHeight = 0,
      int maxWidth = 0,
      int timeMs = 0,
      int quality = 10});

  Future<Uint8List> thumbnailData({
    required String video,
    Map<String, String>? headers,
    ImageFormat imageFormat = ImageFormat.PNG,
    int maxHeight = 0,
    int maxWidth = 0,
    int timeMs = 0,
    int quality = 10,
  });
}
