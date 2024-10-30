import Vision

extension String {
  func stripPrefix(_ prefix: String) -> String {
    guard hasPrefix(prefix) else { return self }
    return String(dropFirst(prefix.count))
  }
}

@objc(TextReader)
class TextReader: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool { return true }

  @objc(read:withOptions:withResolver:withRejecter:)
  func read(imgPath: String, options: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard !imgPath.isEmpty else {
      reject("ERR_EMPTY_PATH", "Image path cannot be empty.", nil)
      return
    }

    let formattedImgPath = imgPath.stripPrefix("file://")
    let threshold = (options["visionIgnoreThreshold"] as? NSNumber)?.floatValue ?? 0.0

    do {
      let imgData = try Data(contentsOf: URL(fileURLWithPath: formattedImgPath))
      guard let image = UIImage(data: imgData), let cgImage = image.cgImage else {
        reject("ERR_IMAGE_PROCESSING", "Failed to load image from the provided path.", nil)
        return
      }

      let requestHandler = VNImageRequestHandler(cgImage: cgImage)
      let ocrRequest = VNRecognizeTextRequest { request, error in
        self.handleTextRecognitionResult(request: request, threshold: threshold, error: error, resolve: resolve, reject: reject)
      }

      if #available(iOS 16.0, *) {
        ocrRequest.automaticallyDetectsLanguage = true
      }

      try requestHandler.perform([ocrRequest])
    } catch {
      reject("ERR_IMAGE_LOADING", "Failed to load or process the image: \(error.localizedDescription)", nil)
    }
  }

  private func handleTextRecognitionResult(request: VNRequest, threshold: Float, error: Error?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    if let error = error {
      reject("ERR_OCR", "Error in text recognition: \(error.localizedDescription)", nil)
      return
    }

    guard let observations = request.results as? [VNRecognizedTextObservation] else {
      reject("ERR_OCR_RESULTS", "Failed to read text from the image.", nil)
      return
    }

    if observations.isEmpty {
      resolve([])
      return
    }

    let extractedStrings = observations.compactMap { observation -> String? in
      guard let topCandidate = observation.topCandidates(1).first else { return nil }
      if topCandidate.confidence >= threshold {
        return topCandidate.string
      }
      return nil
    }

    resolve(extractedStrings.isEmpty ? [] : extractedStrings)
  }
}
