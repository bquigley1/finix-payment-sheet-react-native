import ExpoModulesCore
import FinixPaymentSheet
import UIKit

public class FinixPaymentModule: Module {
    private var presentedSheetController: UIViewController?
    private var currentAction: PaymentAction?

  public func definition() -> ModuleDefinition {
    Name("FinixPaymentModule")
    Events("onPaymentSuccess", "onPaymentCancel", "onPaymentError")
    
    // 1) Initialize once at app start
    AsyncFunction("initSDK") { (applicationId: String, environment: String, promise: Promise) in
      let env: FinixAPIEndpoint = (environment == "Sandbox") ? .Sandbox : .Live
      self.credentials = FinixCredentials(applicationId: applicationId, environment: env)
      promise.resolve(nil)
    }

    // 2) Present card/bank sheet with ALL args from the native API
    AsyncFunction("presentPaymentSheet") { (
      sheetType: String,
      themeParams: [String: Any]?,
      showCancelButton: Bool,
      showCancelItem: Bool,
      cardInfo: [String: Any]?,
      showCountry: Bool,
      addressInfo: [String: Any]?,
      promise: Promise
    ) in
      DispatchQueue.main.async {
        guard let root = Self.findRootVC() else {
          return promise.reject("PRESENTATION_ERROR", "Couldn't find root VC")
        }
        
        var style: PaymentInputController.Style
        switch sheetType {
            case "complete":  style = .complete
            case "partial":   style = .partial
            case "basic":     style = .basic
            case "minimal":   style = .minimal
            case "basicBank": style = .basicBank
            default:
                return promise.reject(
                "INVALID_SHEET_TYPE",
                "Unknown sheetType: \(sheetType)"
            )
        }

        // build theme
        let theme = themeParams.flatMap { self.createColorTheme(from: $0) } ?? ColorTheme.default
        // maybe build Card?
        let card: Card? = cardInfo.flatMap { dict in
          guard
            let name       = dict["name"] as? String,
            let number     = dict["cardNumber"] as? String,
            let sec        = dict["securityCode"] as? String,
            let expDict    = dict["expiration"] as? [String:Any],
            let month      = expDict["month"] as? Int,
            let year       = expDict["year"] as? Int
          else { return nil }
          return Card(
            name: name,
            cardNumber: number,
            expiration: Expiration(month: month, year: year),
            securityCode: sec
          )
        }
        // maybe build Address
        let address: Address? = addressInfo.flatMap { dict in
          guard
            let line1      = dict["line1"] as? String,
            let city       = dict["city"] as? String,
            let region     = dict["region"] as? String,
            let postalCode = dict["postalCode"] as? String,
            let country    = dict["country"] as? String
          else { return nil }
          return Address(
            line1: line1,
            line2: dict["line2"] as? String,
            city: city,
            region: region,
            postalCode: postalCode,
            country: country
          )
        }

        // maybe build Config
        let configuration: PaymentInputController.Configuration? =  PaymentInputController.Configuration(
            title: "Add payment method",
            branding: PaymentInputController.Branding(title: ""),
            buttonTitle: "Save"
          )

        // create & present
        let action = PaymentAction(credentials: self.credentials, configuration: configuration)
        self.currentAction = action
        action.delegate = self
        let controller = action.paymentSheet(
          style: style,
          theme: theme,
          showCancelButton: showCancelButton,
          showCancelItem: showCancelItem,
          card: card,
          showCountry: showCountry,
          address: address
        )
        controller.delegate = self
        self.presentedSheetController = controller
        action.present(from: root, paymentSheet: controller)
        promise.resolve("")
      }
    }

    // 3) Present bank (ACH) sheet with ALL its args
    AsyncFunction("presentBankPaymentSheet") { (
      showCancelButton: Bool,
      showCancelItem: Bool,
      accountType: String?,
      promise: Promise
    ) in
      DispatchQueue.main.async {
        guard let root = Self.findRootVC() else {
          return promise.reject("PRESENTATION_ERROR", "Couldn't find root VC")
        }
        let acct: BankAccountType? = accountType.flatMap { BankAccountType(rawValue: $0) }
        let action = PaymentAction(credentials: self.credentials)
        action.delegate = self
        self.currentAction = action
        let controller = action.bankPaymentSheet(
          showCancelButton: showCancelButton,
          showCancelItem: showCancelItem,
          accountType: acct
        )
        controller.delegate = self
        self.presentedSheetController = controller
        action.present(from: root, paymentSheet: controller)
        promise.resolve(nil)
      }
    }
  }

  private func dismissAll(completion: (() -> Void)? = nil) {
  DispatchQueue.main.async {
    guard let root = Self.findRootVC() else {
      completion?()
      return
    }
    // This will dismiss the top-most modal (your Finix sheet)
    self.presentedSheetController?.dismiss(animated: true) {
      self.presentedSheetController = nil
      completion?()
    }
  }
}



  // stored after initSDK
  private var credentials: FinixCredentials!

  // robust root‐VC finder for multi‐window apps
  private static func findRootVC() -> UIViewController? {
    return UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first { $0.isKeyWindow }?
      .rootViewController
  }

  // same createColorTheme(...) helper from before…
  private func createColorTheme(from params: [String:Any]) -> ColorTheme {
    func uiColor(from hex: String?) -> UIColor? {
        // 1) sanitize string
        guard let hex = hex?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "#", with: ""),
          !hex.isEmpty
        else { return nil }

        // 2) scan into a real variable
        var rgbValue: UInt64 = 0
        let scanner = Scanner(string: hex)
        guard scanner.scanHexInt64(&rgbValue) else {
        return nil
        }

        // 3) decompose into r/g/b
        let r = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = CGFloat(rgbValue & 0x0000FF) / 255.0

        return UIColor(red: r, green: g, blue: b, alpha: 1.0)
    }
    
    var theme = ColorTheme.default
    if let s = params["surface"] as? String, let c = uiColor(from: s) { theme.surface = c }
    if let l = params["label" ] as? String, let c = uiColor(from: l) { theme.label   = c }
    if let t = params["text"  ] as? String, let c = uiColor(from: t) { theme.text    = c }
    if let c = params["container"] as? String, let col = uiColor(from: c) { theme.container = col }
    if let c = params["cancelButton"]    as? String, let col = uiColor(from: c) { theme.cancelButton     = col }
    if let c = params["cancelButtonText"]as? String, let col = uiColor(from: c) { theme.cancelButtonText = col }
    if let c = params["tokenizeButton"]    as? String, let col = uiColor(from: c) { theme.tokenizeButton     = col }
    if let c = params["tokenizeButtonText"]as? String, let col = uiColor(from: c) { theme.tokenizeButtonText = col }
    if let c = params["errorLabel"] as? String, let col = uiColor(from: c) { theme.errorLabel = col }
    if let c = params["logoText" ] as? String, let col = uiColor(from: c) { theme.logoText  = col }
    return theme
  }
}

// MARK: – PaymentActionDelegate → JS events
extension FinixPaymentModule: PaymentActionDelegate {
  public func didSucceed(paymentController: PaymentInputController, instrument: TokenResponse) {
    dismissAll{
        self.sendEvent("onPaymentSuccess", [
      "id": instrument.id,
      "fingerprint": instrument.fingerprint,
      "created": instrument.created.timeIntervalSince1970,
      "updated": instrument.updated.timeIntervalSince1970,
      "instrument": instrument.instrument.rawValue,
      "expires": instrument.expires.timeIntervalSince1970,
      "isoCurrency": instrument.isoCurrency.rawValue
    ])
    }
  }

  public func didCancel(paymentController: PaymentInputController) {
    dismissAll{
        self.sendEvent("onPaymentCancel", [:])
    }
  }

  public func didFail(paymentController: PaymentInputController, error: Error) {
    dismissAll{
        self.sendEvent("onPaymentError", ["message": error.localizedDescription])
    }
  }
}
