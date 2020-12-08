package com.example.imagegallerysaver

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageGallerySaverPlugin : FlutterPlugin, MethodCallHandler {

    private var context: Context? = null
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("ImageGallerySaverPlugin", "onAttachedToEngine")
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "image_gallery_saver")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d("ImageGallerySaverPlugin", "onMethodCall, method = ${call.method}")
        when (call.method) {
            "saveImageToGallery" -> {
                val image = call.argument<ByteArray>("imageBytes") ?: return
                val quality = call.argument<Int>("quality") ?: return
                val name = call.argument<String>("name")
                result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image, 0, image.size), quality, name))
            }
            "saveFileToGallery" -> {
                val path = call.arguments as String
                result.success(saveFileToGallery(path))
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        context = null
        channel.setMethodCallHandler(null)
    }

    private fun generateFile(extension: String = "", name: String? = null): File? {
        val pictureDir = context?.getExternalFilesDir(DIRECTORY_PICTURES)
        pictureDir?.run {
            if (!pictureDir.exists()) {
                pictureDir.mkdirs()
            }
            var fileName = name ?: System.currentTimeMillis().toString()
            if (extension.isNotEmpty()) {
                fileName += (".$extension")
            }
            val file = File(pictureDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            return file
        }
        return null
    }

    private fun saveImageToGallery(bmp: Bitmap, quality: Int, name: String?): String {
        val file = generateFile("jpg", name = name)
        try {
            val fos = FileOutputStream(file)
            println("ImageGallerySaverPlugin $quality")
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            val uri = Uri.fromFile(file)
            MediaStore.Images.Media.insertImage(context?.contentResolver, bmp, "","")
            context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            bmp.recycle()
            return uri.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun saveFileToGallery(filePath: String): String {
        return try {
            val originalFile = File(filePath)
            val file = generateFile(originalFile.extension)
            if (file != null) {
                originalFile.copyTo(file)
                val uri = Uri.fromFile(file)
                context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                uri.toString()
            }
            ""
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    private fun getApplicationName(): String {
        var ai: ApplicationInfo? = null
        try {
            ai = context?.packageManager?.getApplicationInfo(context?.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return if (ai != null) {
            context?.packageManager?.getApplicationLabel(ai)?.toString() ?: "image_gallery_saver"
        } else {
            "image_gallery_saver"
        }
    }
}
