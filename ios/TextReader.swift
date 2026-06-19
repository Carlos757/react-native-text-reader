import Vision
import UIKit

extension String {
  func stripPrefix(_ prefix: String) -> String {
    guard hasPrefix(prefix) else { return self }
    return String(dropFirst(prefix.count))
  }
}

private struct RecognizedLine {
  let text: String
  let confidence: Float
  let frame: CGRect
}

private struct TextCandidate {
  let text: String
  let confidence: Float
  let box: CGRect
  let midY: CGFloat
  let minX: CGFloat
}

@objc(TextReader)
class TextReader: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool { return false }

  @objc(read:withOptions:withResolver:withRejecter:)
  func read(
    imgPath: String,
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    recognizeText(imgPath: imgPath, options: options) { result in
      switch result {
      case .success(let lines):
        resolve(lines.map { $0.text })
      case .failure(let error):
        reject(error.code, error.message, error.underlying)
      }
    }
  }

  @objc(readDetailed:withOptions:withResolver:withRejecter:)
  func readDetailed(
    imgPath: String,
    options: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    recognizeText(imgPath: imgPath, options: options) { result in
      switch result {
      case .success(let lines):
        let lineTexts = lines.map { $0.text }
        let details: [[String: Any]] = lines.map { line in
          var detail: [String: Any] = [
            "text": line.text,
            "confidence": line.confidence,
          ]
          detail["frame"] = [
            "top": Int(line.frame.origin.y * 1000),
            "left": Int(line.frame.origin.x * 1000),
            "width": Int(line.frame.width * 1000),
            "height": Int(line.frame.height * 1000),
          ]
          return detail
        }
        resolve([
          "fullText": lineTexts.joined(separator: "\n"),
          "lines": lineTexts,
          "details": details,
        ])
      case .failure(let error):
        reject(error.code, error.message, error.underlying)
      }
    }
  }

  private struct ReaderError: Error {
    let code: String
    let message: String
    let underlying: Error?
  }

  private func recognizeText(
    imgPath: String,
    options: [String: Any],
    completion: @escaping (Result<[RecognizedLine], ReaderError>) -> Void
  ) {
    guard !imgPath.isEmpty else {
      completion(.failure(ReaderError(code: "ERR_EMPTY_PATH", message: "Image path cannot be empty.", underlying: nil)))
      return
    }

    let formattedImgPath = imgPath.stripPrefix("file://")
    let threshold = (options["visionIgnoreThreshold"] as? NSNumber)?.floatValue ?? 0.0

    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let imgData = try Data(contentsOf: URL(fileURLWithPath: formattedImgPath))
        guard let image = self.fixedOrientationImage(from: imgData),
              let cgImage = image.cgImage else {
          completion(.failure(ReaderError(code: "ERR_IMAGE_PROCESSING", message: "Failed to load image from the provided path.", underlying: nil)))
          return
        }

        let requestHandler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        let ocrRequest = VNRecognizeTextRequest { request, error in
          if let error = error {
            completion(.failure(ReaderError(code: "ERR_OCR", message: "Error in text recognition: \(error.localizedDescription)", underlying: error)))
            return
          }

          guard let observations = request.results as? [VNRecognizedTextObservation] else {
            completion(.failure(ReaderError(code: "ERR_OCR_RESULTS", message: "Failed to read text from the image.", underlying: nil)))
            return
          }

          let lines = self.groupObservationsIntoLines(observations: observations, threshold: threshold)
          completion(.success(lines))
        }

        self.applyOptions(to: ocrRequest, from: options)

        try requestHandler.perform([ocrRequest])
      } catch {
        completion(.failure(ReaderError(code: "ERR_IMAGE_LOADING", message: "Failed to load or process the image: \(error.localizedDescription)", underlying: error)))
      }
    }
  }

  private func applyOptions(to request: VNRecognizeTextRequest, from options: [String: Any]) {
    if #available(iOS 16.0, *) {
      request.automaticallyDetectsLanguage = true
    }

    if let level = options["recognitionLevel"] as? String {
      request.recognitionLevel = level == "fast" ? .fast : .accurate
    } else {
      request.recognitionLevel = .accurate
    }

    if let useCorrection = options["useLanguageCorrection"] as? Bool {
      request.usesLanguageCorrection = useCorrection
    }

    if let languages = options["recognitionLanguages"] as? [String], !languages.isEmpty {
      request.recognitionLanguages = languages
    }

    if let customWords = options["customWords"] as? [String], !customWords.isEmpty {
      request.customWords = customWords
    }

    if let minHeight = options["minimumTextHeight"] as? NSNumber {
      request.minimumTextHeight = minHeight.floatValue
    }
  }

  private func fixedOrientationImage(from data: Data) -> UIImage? {
    guard let image = UIImage(data: data) else { return nil }
    if image.imageOrientation == .up { return image }

    UIGraphicsBeginImageContextWithOptions(image.size, false, image.scale)
    image.draw(in: CGRect(origin: .zero, size: image.size))
    let normalized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return normalized
  }

  private func groupObservationsIntoLines(
    observations: [VNRecognizedTextObservation],
    threshold: Float
  ) -> [RecognizedLine] {
    let candidates: [TextCandidate] = observations.compactMap { observation in
      guard let topCandidate = observation.topCandidates(1).first else { return nil }
      guard topCandidate.confidence >= threshold else { return nil }
      let box = observation.boundingBox
      return TextCandidate(
        text: topCandidate.string,
        confidence: topCandidate.confidence,
        box: box,
        midY: box.midY,
        minX: box.minX
      )
    }

    guard !candidates.isEmpty else { return [] }

    let sorted = candidates.sorted { lhs, rhs in
      if abs(lhs.midY - rhs.midY) > 0.02 {
        return lhs.midY > rhs.midY
      }
      return lhs.minX < rhs.minX
    }

    var lines: [RecognizedLine] = []
    var currentGroup: [TextCandidate] = []
    var currentMidY: CGFloat?

    for candidate in sorted {
      if let midY = currentMidY, abs(candidate.midY - midY) <= 0.02 {
        currentGroup.append(candidate)
      } else {
        if !currentGroup.isEmpty {
          lines.append(self.mergeGroup(currentGroup))
        }
        currentGroup = [candidate]
        currentMidY = candidate.midY
      }
    }

    if !currentGroup.isEmpty {
      lines.append(self.mergeGroup(currentGroup))
    }

    return lines
  }

  private func mergeGroup(_ group: [TextCandidate]) -> RecognizedLine {
    let sortedGroup = group.sorted { $0.minX < $1.minX }
    let text = sortedGroup.map { $0.text }.joined(separator: " ")
    let confidence = sortedGroup.map { $0.confidence }.max() ?? 0
    let minX = sortedGroup.map { $0.box.minX }.min() ?? 0
    let minY = sortedGroup.map { $0.box.minY }.min() ?? 0
    let maxX = sortedGroup.map { $0.box.maxX }.max() ?? 0
    let maxY = sortedGroup.map { $0.box.maxY }.max() ?? 0
    let frame = CGRect(x: minX, y: minY, width: maxX - minX, height: maxY - minY)
    return RecognizedLine(text: text, confidence: confidence, frame: frame)
  }
}
