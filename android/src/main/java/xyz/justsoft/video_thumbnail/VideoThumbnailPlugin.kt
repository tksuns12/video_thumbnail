package xyz.justsoft.video_thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * VideoThumbnailPlugin
 */
class VideoThumbnailPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private var context: Context? = null
    private var executor: ExecutorService? = null
    private var channel: MethodChannel? = null
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        executor = Executors.newCachedThreadPool()
        channel =
            MethodChannel(binding.binaryMessenger, "plugins.justsoft.xyz/video_thumbnail")
        channel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        executor?.shutdown()
        executor = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val args: Map<String, Any>? = call.arguments()
        val video = args?.get("video") as String?
        val format = args?.get("format") as Int
        val maxh = args["maxh"] as Int
        val maxw = args["maxw"] as Int
        val timeMs = args["timeMs"] as Int
        val quality = args["quality"] as Int
        val method: String = call.method
        executor?.execute {
            var thumbnail: Any? = null
            var handled = false
            var exc: Exception? = null
            try {
                if (method == "file") {
                    val path = args["path"] as String?
                    thumbnail = buildThumbnailFile(
                        video,
                        path,
                        format,
                        maxh,
                        maxw,
                        timeMs,
                        quality
                    )
                    handled = true
                } else if (method == "data") {
                    thumbnail =
                        buildThumbnailData(video, format, maxh, maxw, timeMs, quality)
                    handled = true
                }
            } catch (e: Exception) {
                exc = e
            }
            onResult(result, thumbnail, handled, exc)
        }
    }

    private fun buildThumbnailData(
        vidPath: String?, format: Int, maxh: Int, maxw: Int,
        timeMs: Int, quality: Int
    ): ByteArray {
        // Log.d(TAG, String.format("buildThumbnailData( format:%d, maxh:%d, maxw:%d,
        // timeMs:%d, quality:%d )", format, maxh, maxw, timeMs, quality));
        val bitmap: Bitmap = createVideoThumbnail(vidPath, maxh, maxw, timeMs)
            ?: throw NullPointerException()
        val stream = ByteArrayOutputStream()
        bitmap.compress(intToFormat(format), quality, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun buildThumbnailFile(
        vidPath: String?, path: String?, format: Int,
        maxh: Int, maxw: Int, timeMs: Int, quality: Int
    ): String {
        // Log.d(TAG, String.format("buildThumbnailFile( format:%d, maxh:%d, maxw:%d,
        // timeMs:%d, quality:%d )", format, maxh, maxw, timeMs, quality));
        var mPath = path
        val bytes = buildThumbnailData(vidPath, format, maxh, maxw, timeMs, quality)
        val ext = formatExt(format)
        val i = vidPath!!.lastIndexOf(".")
        var fullPath = vidPath.substring(0, i + 1) + ext
        val isLocalFile = vidPath.startsWith("/") || vidPath.startsWith("file://")
        if (path == null && !isLocalFile) {
            mPath = context?.cacheDir?.absolutePath
        }
        if (path != null) {
            fullPath = if (mPath!!.endsWith(ext)) {
                mPath
            } else {
                // try to save to same folder as the vidPath
                val j = fullPath.lastIndexOf("/")
                if (mPath.endsWith("/")) {
                    mPath + fullPath.substring(j + 1)
                } else {
                    mPath + fullPath.substring(j)
                }
            }
        }
        try {
            val f = FileOutputStream(fullPath)
            f.write(bytes)
            f.close()
            Log.d(TAG, String.format("buildThumbnailFile( written:%d )", bytes.size))
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
        return fullPath
    }

    private fun onResult(result: MethodChannel.Result, thumbnail: Any?, handled: Boolean, e: Exception?) {
        runOnUiThread(Runnable {
            if (!handled) {
                result.notImplemented()
                return@Runnable
            }
            if (e != null) {
                e.printStackTrace()
                result.error("exception", e.message, null)
                return@Runnable
            }
            result.success(thumbnail)
        })
    }

    /**
     * Create a video thumbnail for a video. May return null if the video is corrupt
     * or the format is not supported.
     *
     * @param video   the URI of video
     * @param targetH the max height of the thumbnail
     * @param targetW the max width of the thumbnail
     */
    private fun createVideoThumbnail(
        video: String?, targetH: Int, targetW: Int,
        timeMs: Int
    ): Bitmap? {
        var targetWidth = targetW
        var targetHeight = targetH
        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            if (video!!.startsWith("/")) {
                setDataSource(video, retriever)
            } else if (video.startsWith("file://")) {
                setDataSource(video.substring(7), retriever)
            } else {
                retriever.setDataSource(video,  HashMap())
            }
            if (targetHeight != 0 || targetWidth != 0) {
                if (Build.VERSION.SDK_INT >= 27 && targetHeight != 0 && targetWidth != 0) {
                    // API Level 27
                    bitmap = retriever.getScaledFrameAtTime(
                        (timeMs * 1000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST,
                        targetWidth, targetHeight
                    )
                } else {
                    bitmap = retriever.getFrameAtTime(
                        (timeMs * 1000).toLong(),
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    if (bitmap != null) {
                        val width: Int = bitmap.getWidth()
                        val height: Int = bitmap.getHeight()
                        if (targetWidth == 0) {
                            targetWidth = Math.round(targetHeight.toFloat() / height * width)
                        }
                        if (targetHeight == 0) {
                            targetHeight = Math.round(targetWidth.toFloat() / width * height)
                        }
                        Log.d(
                            TAG,
                            String.format(
                                "original w:%d, h:%d => %d, %d",
                                width,
                                height,
                                targetWidth,
                                targetHeight
                            )
                        )
                        bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    }
                }
            } else {
                bitmap = retriever.getFrameAtTime(
                    (timeMs * 1000).toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {
                ex.printStackTrace()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
        return bitmap
    }

    companion object {
        private const val TAG = "ThumbnailPlugin"
        private const val HIGH_QUALITY_MIN_VAL = 70
        private fun intToFormat(format: Int): Bitmap.CompressFormat {
            return when (format) {
                0 -> Bitmap.CompressFormat.JPEG
                1 -> Bitmap.CompressFormat.PNG
                2 -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
        }

        private fun formatExt(format: Int): String {
            return when (format) {
                0 -> "jpg"
                1 -> "png"
                2 -> "webp"
                else -> "jpg"
            }
        }

        private fun runOnUiThread(runnable: Runnable) {
            Handler(Looper.getMainLooper()).post(runnable)
        }

        @Throws(IOException::class)
        private fun setDataSource(video: String?, retriever: MediaMetadataRetriever) {
            val videoFile = video?.let { File(it) }
            val inputStream = FileInputStream(videoFile?.absolutePath)
            retriever.setDataSource(inputStream.getFD())
        }
    }
}
