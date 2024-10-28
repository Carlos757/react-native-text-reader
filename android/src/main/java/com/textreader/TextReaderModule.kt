package com.textreader

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.facebook.react.bridge.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
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
import java.io.IOException
import java.net.URL

class TextReaderModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String {
        return "TextReader"
    }

    @Throws(IOException::class)
    private fun getInputImage(reactContext: ReactApplicationContext, url: String): InputImage {
        return if (url.contains("http://") || url.contains("https://")) {
            val urlInput = URL(url)
            val image: Bitmap = BitmapFactory.decodeStream(urlInput.openConnection().getInputStream())
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

    private fun lineToMap(line: Text.Line): WritableMap {
        return Arguments.createMap().apply {
            putString("text", line.text)
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

    @ReactMethod
    fun read(url: String, options: ReadableMap?, promise: Promise) {
        try {
            val script = options?.getString("script")

            val image = getInputImage(reactApplicationContext, url)
            
            val options = getScriptTextRecognizerOptions(script)

            val recognizer: TextRecognizer = TextRecognition.getClient(options)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val result = Arguments.createMap()
                    result.putString("text", visionText.text)

                    val blocksArray = Arguments.createArray()
                    visionText.textBlocks.forEach { block ->
                        blocksArray.pushMap(blockToMap(block))
                    }
                    result.putArray("blocks", blocksArray)

                    promise.resolve(result)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    promise.reject("Text recognition failed", e)
                }
        } catch (e: IOException) {
            e.printStackTrace()
            promise.reject("Text recognition failed", e)
        }
    }
}