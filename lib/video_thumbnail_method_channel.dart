import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:video_thumbnail/video_thumbnail.dart';

import 'video_thumbnail_platform_interface.dart';

/// An implementation of [VideoThumbnailPlatform] that uses method channels.
class MethodChannelVideoThumbnail extends VideoThumbnailPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel =
      const MethodChannel('plugins.justsoft.xyz/video_thumbnail');

  @override
  Future<String> thumbnailFile(
      {required String video,
      Map<String, String>? headers,
      String? thumbnailPath,
      ImageFormat imageFormat = ImageFormat.PNG,
      int maxHeight = 0,
      int maxWidth = 0,
      int timeMs = 0,
      int quality = 10}) async {
    assert(video.isNotEmpty);
    if (video.isEmpty) {
      throw ArgumentError('video must not be empty');
    }
    final reqMap = <String, dynamic>{
      'video': video,
      'headers': headers,
      'path': thumbnailPath,
      'format': imageFormat.index,
      'maxh': maxHeight,
      'maxw': maxWidth,
      'timeMs': timeMs,
      'quality': quality
    };
    return await methodChannel.invokeMethod('file', reqMap);
  }

  @override
  Future<Uint8List> thumbnailData({
    required String video,
    Map<String, String>? headers,
    ImageFormat imageFormat = ImageFormat.PNG,
    int maxHeight = 0,
    int maxWidth = 0,
    int timeMs = 0,
    int quality = 10,
  }) async {
    assert(video.isNotEmpty);
    if (video.isEmpty) {
      throw ArgumentError('video must not be empty');
    }

    final reqMap = <String, dynamic>{
      'video': video,
      'headers': headers,
      'format': imageFormat.index,
      'maxh': maxHeight,
      'maxw': maxWidth,
      'timeMs': timeMs,
      'quality': quality,
    };

    return await methodChannel.invokeMethod('data', reqMap);
  }
}
