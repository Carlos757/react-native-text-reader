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
  private fun remoteBitmap(url: String): Bitmap {
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

    return image ?: throw IOException("Failed to decode remote image.")
  }

  @Throws(IOException::class)
  private fun getInputImage(reactContext: ReactApplicationContext, url: String): InputImage {
    return if (url.contains("http://") || url.contains("https://")) {
      InputImage.fromBitmap(remoteBitmap(url), 0)
    } else {
      val uri = Uri.parse(url)
      InputImage.fromFilePath(reactContext, uri)
    }
  }

  private fun croppedInputImage(
    reactContext: ReactApplicationContext,
    url: String,
    region: ReadableMap
  ): InputImage {
    val bitmap = loadBitmap(reactContext, url)
    val x = (region.getDouble("x") * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val y = (region.getDouble("y") * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val width = (region.getDouble("width") * bitmap.width).toInt()
      .coerceIn(1, bitmap.width - x)
    val height = (region.getDouble("height") * bitmap.height).toInt()
      .coerceIn(1, bitmap.height - y)

    return InputImage.fromBitmap(Bitmap.createBitmap(bitmap, x, y, width, height), 0)
  }

  @Throws(IOException::class)
  private fun loadBitmap(reactContext: ReactApplicationContext, url: String): Bitmap {
    if (url.contains("http://") || url.contains("https://")) {
      return remoteBitmap(url)
    }

    val stream = reactContext.contentResolver.openInputStream(Uri.parse(url))
      ?: throw IOException("Could not open image at $url")

    return stream.use { BitmapFactory.decodeStream(it) }
      ?: throw IOException("Failed to decode image at $url")
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

  private fun lineConfidence(line: Text.Line): Float? {
    val confidences = line.elements.mapNotNull { it.confidence }
    if (confidences.isEmpty()) {
      return null
    }
    return confidences.average().toFloat()
  }

  private fun normalizedBox(rect: Rect?, width: Int, height: Int): WritableMap? {
    if (rect == null || width <= 0 || height <= 0) {
      return null
    }
    return Arguments.createMap().apply {
      putDouble("x", rect.left.toDouble() / width)
      putDouble("y", rect.top.toDouble() / height)
      putDouble("width", rect.width().toDouble() / width)
      putDouble("height", rect.height().toDouble() / height)
    }
  }

  private fun lineToMap(
    line: Text.Line,
    imageWidth: Int,
    imageHeight: Int,
    includeWords: Boolean
  ): WritableMap {
    return Arguments.createMap().apply {
      putString("text", line.text)
      lineConfidence(line)?.let { putDouble("confidence", it.toDouble()) }
      line.boundingBox?.let { putMap("frame", rectToMap(it)) }
      normalizedBox(line.boundingBox, imageWidth, imageHeight)?.let { putMap("box", it) }
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

      if (includeWords) {
        val wordsArray = Arguments.createArray()
        line.elements.forEach { element ->
          wordsArray.pushMap(Arguments.createMap().apply {
            putString("text", element.text)
            normalizedBox(element.boundingBox, imageWidth, imageHeight)?.let {
              putMap("box", it)
            }
            element.confidence?.let { putDouble("confidence", it.toDouble()) }
          })
        }
        putArray("words", wordsArray)
      }
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
      .filter { line -> (lineConfidence(line) ?: Float.MAX_VALUE) >= confidenceThreshold }
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

    val includeWords = options?.hasKey("includeWords") == true &&
      options.getBoolean("includeWords")

    val recognizer: TextRecognizer = TextRecognition.getClient(getScriptTextRecognizerOptions(script))

    try {
      val region = if (options?.hasKey("regionOfInterest") == true) {
        options.getMap("regionOfInterest")
      } else {
        null
      }
      val image = if (region != null) {
        croppedInputImage(reactApplicationContext, url, region)
      } else {
        getInputImage(reactApplicationContext, url)
      }

      recognizer.process(image)
        .addOnSuccessListener { visionText ->
          val lines = sortedLines(visionText, confidenceThreshold)
          if (detailed) {
            promise.resolve(
              buildDetailedResult(
                visionText,
                lines,
                image.width,
                image.height,
                includeWords
              )
            )
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

  private fun buildDetailedResult(
    visionText: Text,
    lines: List<Text.Line>,
    imageWidth: Int,
    imageHeight: Int,
    includeWords: Boolean
  ): WritableMap {
    val lineTexts = lines.map { it.text }
    val detailsArray = Arguments.createArray()
    lines.forEach { line ->
      detailsArray.pushMap(lineToMap(line, imageWidth, imageHeight, includeWords))
    }

    return Arguments.createMap().apply {
      putString("fullText", visionText.text)
      putArray("lines", Arguments.createArray().apply {
        lineTexts.forEach { pushString(it) }
      })
      putArray("details", detailsArray)
      putString("coordinateSpace", "normalized-top-left")
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
