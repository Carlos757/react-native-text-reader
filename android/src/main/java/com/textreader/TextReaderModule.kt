package com.textreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.facebook.react.bridge.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TextReaderModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  companion object {
    private const val HTTP_CONNECT_TIMEOUT_MS = 10_000
    private const val HTTP_READ_TIMEOUT_MS = 10_000
    private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
  }

  override fun getName(): String = "TextReader"

  @Throws(IOException::class)
  private fun getInputImage(reactContext: ReactApplicationContext, url: String): InputImage {
    return if (url.contains("http://") || url.contains("https://")) {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.connectTimeout = HTTP_CONNECT_TIMEOUT_MS
      connection.readTimeout = HTTP_READ_TIMEOUT_MS
      connection.connect()

      val contentLength = connection.contentLength
      if (contentLength > MAX_IMAGE_BYTES) {
        connection.disconnect()
        throw IOException("Remote image exceeds maximum allowed size.")
      }

      val image: Bitmap? = connection.inputStream.use { stream ->
        BitmapFactory.decodeStream(stream)
      }
      connection.disconnect()

      if (image == null) {
        throw IOException("Failed to decode remote image.")
      }

      InputImage.fromBitmap(image, 0)
    } else {
      val uri = Uri.parse(url)
      InputImage.fromFilePath(reactContext, uri)
    }
  }

  private fun rectToMap(rect: Rect): WritableMap {
    return Arguments.createMap().apply {
      putInt("width", rect.width())
      putInt("height", rect.height())
      putInt("top", rect.top)
      putInt("left", rect.left)
    }
  }

  private fun cornerPointsToMap(points: Array<Point>): WritableArray {
    return Arguments.createArray().apply {
      points.forEach { point ->
        pushMap(Arguments.createMap().apply {
          putInt("x", point.x)
          putInt("y", point.y)
        })
      }
    }
  }

  private fun langToMap(lang: String): WritableArray {
    return Arguments.createArray().apply {
      pushMap(Arguments.createMap().apply {
        putString("languageCode", lang)
      })
    }
  }

  private fun lineConfidence(line: Text.Line): Float {
    val elements = line.elements
    if (elements.isEmpty()) return 1.0f
    return elements.map { element ->
      try {
        val method = element.javaClass.getMethod("getConfidence")
        (method.invoke(element) as? Float) ?: 1.0f
      } catch (_: Exception) {
        1.0f
      }
    }.average().toFloat()
  }

  private fun lineToMap(line: Text.Line): WritableMap {
    return Arguments.createMap().apply {
      putString("text", line.text)
      putDouble("confidence", lineConfidence(line).toDouble())
      line.boundingBox?.let { putMap("frame", rectToMap(it)) }
      line.cornerPoints?.let { putArray("cornerPoints", cornerPointsToMap(it)) }
      putArray("recognizedLanguages", langToMap(line.recognizedLanguage))

      val elementsArray = Arguments.createArray()
      line.elements.forEach { element ->
        elementsArray.pushMap(Arguments.createMap().apply {
          putString("text", element.text)
          element.boundingBox?.let { putMap("frame", rectToMap(it)) }
          element.cornerPoints?.let { putArray("cornerPoints", cornerPointsToMap(it)) }
        })
      }
      putArray("elements", elementsArray)
    }
  }

  private fun blockToMap(block: Text.TextBlock): WritableMap {
    return Arguments.createMap().apply {
      putString("text", block.text)
      block.boundingBox?.let { putMap("frame", rectToMap(it)) }
      block.cornerPoints?.let { putArray("cornerPoints", cornerPointsToMap(it)) }

      val linesArray = Arguments.createArray()
      block.lines.forEach { line ->
        linesArray.pushMap(lineToMap(line))
      }
      putArray("lines", linesArray)
      putArray("recognizedLanguages", langToMap(block.recognizedLanguage))
    }
  }

  @NonNull
  private fun getScriptTextRecognizerOptions(@Nullable script: String?): TextRecognizerOptionsInterface {
    return when (script) {
      "Chinese" -> ChineseTextRecognizerOptions.Builder().build()
      "Devanagari" -> DevanagariTextRecognizerOptions.Builder().build()
      "Japanese" -> JapaneseTextRecognizerOptions.Builder().build()
      "Korean" -> KoreanTextRecognizerOptions.Builder().build()
      else -> TextRecognizerOptions.DEFAULT_OPTIONS
    }
  }

  private fun sortedLines(visionText: Text, confidenceThreshold: Float): List<Text.Line> {
    return visionText.textBlocks
      .flatMap { block -> block.lines }
      .filter { line -> lineConfidence(line) >= confidenceThreshold }
      .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
  }

  private fun processImage(
    url: String,
    options: ReadableMap?,
    detailed: Boolean,
    promise: Promise
  ) {
    if (url.isEmpty()) {
      promise.reject("ERR_EMPTY_PATH", "Image path cannot be empty.")
      return
    }

    val script = options?.getString("script")
    val confidenceThreshold = if (options != null && options.hasKey("confidenceThreshold")) {
      options.getDouble("confidenceThreshold").toFloat()
    } else {
      0.0f
    }

    val recognizer: TextRecognizer = TextRecognition.getClient(getScriptTextRecognizerOptions(script))

    try {
      val image = getInputImage(reactApplicationContext, url)
      recognizer.process(image)
        .addOnSuccessListener { visionText ->
          val lines = sortedLines(visionText, confidenceThreshold)
          if (detailed) {
            promise.resolve(buildDetailedResult(visionText, lines))
          } else {
            val linesArray = Arguments.createArray()
            lines.forEach { line -> linesArray.pushString(line.text) }
            promise.resolve(linesArray)
          }
        }
        .addOnFailureListener { e ->
          promise.reject("ERR_OCR", "Text recognition failed: ${e.message}", e)
        }
        .addOnCompleteListener {
          recognizer.close()
        }
    } catch (e: IOException) {
      recognizer.close()
      promise.reject("ERR_IMAGE_LOADING", "Failed to load image: ${e.message}", e)
    } catch (e: Exception) {
      recognizer.close()
      promise.reject("ERR_IMAGE_PROCESSING", "Failed to process image: ${e.message}", e)
    }
  }

  private fun buildDetailedResult(visionText: Text, lines: List<Text.Line>): WritableMap {
    val lineTexts = lines.map { it.text }
    val detailsArray = Arguments.createArray()
    lines.forEach { line ->
      detailsArray.pushMap(lineToMap(line))
    }

    return Arguments.createMap().apply {
      putString("fullText", visionText.text)
      putArray("lines", Arguments.createArray().apply {
        lineTexts.forEach { pushString(it) }
      })
      putArray("details", detailsArray)
    }
  }

  @ReactMethod
  fun read(url: String, options: ReadableMap?, promise: Promise) {
    processImage(url, options, detailed = false, promise = promise)
  }

  @ReactMethod
  fun readDetailed(url: String, options: ReadableMap?, promise: Promise) {
    processImage(url, options, detailed = true, promise = promise)
  }
}
