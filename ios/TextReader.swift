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
  func read(imgPath: String, options: [String: Float], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard !imgPath.isEmpty else { reject("ERR", "Image path cannot be empty.", nil); return }

    let formattedImgPath = imgPath.stripPrefix("file://")
    var threshold: Float = 0.0

    if !(options["visionIgnoreThreshold"]?.isZero ?? true) {
      threshold = options["visionIgnoreThreshold"] ?? 0.0
    }

    do {
      let imgData = try Data(contentsOf: URL(fileURLWithPath: formattedImgPath))
      let image = UIImage(data: imgData)

      guard let cgImage = image?.cgImage else { return }

      let requestHandler = VNImageRequestHandler(cgImage: cgImage)

      let ocrRequest = VNRecognizeTextRequest { (request: VNRequest, error: Error?) in
        self.textReaderHandler(request: request, threshold: threshold, error: error, resolve: resolve, reject: reject)
      }

      try requestHandler.perform([ocrRequest])
    } catch {
      print(error)
      reject("ERR", error.localizedDescription, nil)
    }
  }

  func textReaderHandler(request: VNRequest, threshold: Float, error _: Error?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    guard let observations = request.results as? [VNRecognizedTextObservation] else { reject("ERR", "Failed to read the text.", nil); return }

    let strings = observations.compactMap { observation -> String? in
      if observation.topCandidates(1).first?.confidence ?? 0 >= threshold {
        return observation.topCandidates(1).first?.string
      } else {
        return nil
      }
    }
    
    resolve(strings)
  }
}