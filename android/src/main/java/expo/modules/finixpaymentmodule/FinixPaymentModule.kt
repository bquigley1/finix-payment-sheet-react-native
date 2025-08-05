package expo.modules.finixpaymentmodule

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.exception.Exceptions
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.finix.finixpaymentsheet.domain.model.PaymentSheetColors
import com.finix.finixpaymentsheet.domain.model.PaymentSheetResources
import com.finix.finixpaymentsheet.domain.model.tokenize.TokenizedResponse
import com.finix.finixpaymentsheet.ui.viewModel.paymentSheet.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class FinixPaymentModule : Module() {
  private var applicationId: String? = null
  private var isSandbox: Boolean = true
  private var currentActivity: Activity? = null
  private var composeView: ComposeView? = null

  override fun definition() = ModuleDefinition {
    Name("FinixPaymentModule")
    
    Events("onPaymentSuccess", "onPaymentCancel", "onPaymentError")
    
    AsyncFunction("initSDK") { applicationId: String, environment: String, promise: Promise ->
      this@FinixPaymentModule.applicationId = applicationId
      this@FinixPaymentModule.isSandbox = environment == "Sandbox"
      promise.resolve(null)
    }
    
    AsyncFunction("presentPaymentSheet") { 
      sheetType: String,
      themeParams: Map<String, Any>?,
      showCancelButton: Boolean,
      showCancelItem: Boolean,
      card: Map<String, Any>?,
      showCountry: Boolean,
      address: Map<String, Any>?,
      promise: Promise ->
      
      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject("PRESENTATION_ERROR", "No current activity", null)
        return@AsyncFunction
      }
      
      val appId = applicationId
      if (appId == null) {
        promise.reject("INIT_ERROR", "SDK not initialized. Call initSDK first", null)
        return@AsyncFunction
      }
      
      currentActivity = activity
      
      activity.runOnUiThread {
        try {
          showPaymentSheet(
            activity,
            sheetType,
            appId,
            isSandbox,
            themeParams,
            showCancelButton,
            showCancelItem,
            card,
            showCountry,
            address
          )
          promise.resolve(null)
        } catch (e: Exception) {
          promise.reject("PRESENTATION_ERROR", e.message, e)
        }
      }
    }
    
    AsyncFunction("presentBankPaymentSheet") { 
      showCancelButton: Boolean,
      showCancelItem: Boolean,
      accountType: String?,
      promise: Promise ->
      
      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject("PRESENTATION_ERROR", "No current activity", null)
        return@AsyncFunction
      }
      
      val appId = applicationId
      if (appId == null) {
        promise.reject("INIT_ERROR", "SDK not initialized. Call initSDK first", null)
        return@AsyncFunction
      }
      
      currentActivity = activity
      
      activity.runOnUiThread {
        try {
          showBankPaymentSheet(
            activity,
            appId,
            isSandbox,
            showCancelButton,
            showCancelItem,
            accountType
          )
          promise.resolve(null)
        } catch (e: Exception) {
          promise.reject("PRESENTATION_ERROR", e.message, e)
        }
      }
    }
  }
  
  private fun showPaymentSheet(
    activity: Activity,
    sheetType: String,
    applicationId: String,
    isSandbox: Boolean,
    themeParams: Map<String, Any>?,
    showCancelButton: Boolean,
    showCancelItem: Boolean,
    card: Map<String, Any>?,
    showCountry: Boolean,
    address: Map<String, Any>?
  ) {
    val composeView = ComposeView(activity).apply {
      setContent {
        PaymentSheetWrapper(
          sheetType = sheetType,
          applicationId = applicationId,
          isSandbox = isSandbox,
          themeParams = themeParams,
          showCancelButton = showCancelButton,
          showCancelItem = showCancelItem,
          card = card,
          showCountry = showCountry,
          address = address,
          onDismiss = {
            sendEvent("onPaymentCancel", mapOf<String, Any>())
            removeComposeView()
          },
          onSuccess = { token ->
            val tokenMap = mapOf(
              "id" to token.id,
              "fingerprint" to token.fingerprint,
              "created" to token.created,
              "updated" to token.updated,
              "instrument" to token.instrument,
              "expires" to token.expires,
              "isoCurrency" to token.isoCurrency
            )
            sendEvent("onPaymentSuccess", tokenMap)
            removeComposeView()
          },
          onError = { error ->
            sendEvent("onPaymentError", mapOf("message" to error))
            removeComposeView()
          }
        )
      }
    }
    
    this.composeView = composeView
    (activity.window.decorView as android.view.ViewGroup).addView(composeView)
  }
  
  private fun showBankPaymentSheet(
    activity: Activity,
    applicationId: String,
    isSandbox: Boolean,
    showCancelButton: Boolean,
    showCancelItem: Boolean,
    accountType: String?
  ) {
    val composeView = ComposeView(activity).apply {
      setContent {
        BankPaymentSheetWrapper(
          applicationId = applicationId,
          isSandbox = isSandbox,
          showCancelButton = showCancelButton,
          showCancelItem = showCancelItem,
          accountType = accountType,
          onDismiss = {
            sendEvent("onPaymentCancel", mapOf<String, Any>())
            removeComposeView()
          },
          onSuccess = { token ->
            val tokenMap = mapOf(
              "id" to token.id,
              "fingerprint" to token.fingerprint,
              "created" to token.created,
              "updated" to token.updated,
              "instrument" to token.instrument,
              "expires" to token.expires,
              "isoCurrency" to token.isoCurrency
            )
            sendEvent("onPaymentSuccess", tokenMap)
            removeComposeView()
          },
          onError = { error ->
            sendEvent("onPaymentError", mapOf("message" to error))
            removeComposeView()
          }
        )
      }
    }
    
    this.composeView = composeView
    (activity.window.decorView as android.view.ViewGroup).addView(composeView)
  }
  
  private fun removeComposeView() {
    currentActivity?.runOnUiThread {
      composeView?.let { view ->
        (view.parent as? android.view.ViewGroup)?.removeView(view)
      }
      composeView = null
    }
  }
  
  @Composable
  private fun PaymentSheetWrapper(
    sheetType: String,
    applicationId: String,
    isSandbox: Boolean,
    themeParams: Map<String, Any>?,
    showCancelButton: Boolean,
    showCancelItem: Boolean,
    card: Map<String, Any>?,
    showCountry: Boolean,
    address: Map<String, Any>?,
    onDismiss: () -> Unit,
    onSuccess: (TokenizedResponse) -> Unit,
    onError: (String) -> Unit
  ) {
    val showSheet = remember { mutableStateOf(true) }
    
    if (showSheet.value) {
      val colors = createPaymentSheetColors(themeParams)
      val resources = PaymentSheetResources()
      
      when (sheetType) {
        "complete" -> CompletePaymentSheetOutlined(
          onDismiss = {
            showSheet.value = false
            onDismiss()
          },
          onNegativeClick = {
            showSheet.value = false
            onDismiss()
          },
          onPositiveClick = { token ->
            showSheet.value = false
            onSuccess(token)
          },
          applicationId = applicationId,
          isSandbox = isSandbox,
          paymentSheetColors = colors,
          paymentSheetResources = resources
        )
        
        "partial" -> PartialPaymentSheetOutlined(
          onDismiss = {
            showSheet.value = false
            onDismiss()
          },
          onNegativeClick = {
            showSheet.value = false
            onDismiss()
          },
          onPositiveClick = { token ->
            showSheet.value = false
            onSuccess(token)
          },
          applicationId = applicationId,
          isSandbox = isSandbox,
          paymentSheetColors = colors,
          paymentSheetResources = resources
        )
        
        "basic" -> BasicPaymentSheetOutlined(
          onDismiss = {
            showSheet.value = false
            onDismiss()
          },
          onNegativeClick = {
            showSheet.value = false
            onDismiss()
          },
          onPositiveClick = { token ->
            showSheet.value = false
            onSuccess(token)
          },
          applicationId = applicationId,
          isSandbox = isSandbox,
          paymentSheetColors = colors,
          paymentSheetResources = resources
        )
        
        "minimal" -> MinimalPaymentSheetOutlined(
          onDismiss = {
            showSheet.value = false
            onDismiss()
          },
          onNegativeClick = {
            showSheet.value = false
            onDismiss()
          },
          onPositiveClick = { token ->
            showSheet.value = false
            onSuccess(token)
          },
          applicationId = applicationId,
          isSandbox = isSandbox,
          paymentSheetColors = colors,
          paymentSheetResources = resources
        )
        
        "basicBank" -> {
          // This is handled by presentBankPaymentSheet
          showSheet.value = false
          onError("basicBank should be called through presentBankPaymentSheet")
        }
        
        else -> {
          showSheet.value = false
          onError("Unknown sheet type: $sheetType")
        }
      }
    }
  }
  
  @Composable
  private fun BankPaymentSheetWrapper(
    applicationId: String,
    isSandbox: Boolean,
    showCancelButton: Boolean,
    showCancelItem: Boolean,
    accountType: String?,
    onDismiss: () -> Unit,
    onSuccess: (TokenizedResponse) -> Unit,
    onError: (String) -> Unit
  ) {
    val showSheet = remember { mutableStateOf(true) }
    
    if (showSheet.value) {
      // Note: The actual bank payment sheet composable needs to be determined from Finix SDK
      // This is a placeholder implementation
      BasicPaymentSheetOutlined(
        onDismiss = {
          showSheet.value = false
          onDismiss()
        },
        onNegativeClick = {
          showSheet.value = false
          onDismiss()
        },
        onPositiveClick = { token ->
          showSheet.value = false
          onSuccess(token)
        },
        applicationId = applicationId,
        isSandbox = isSandbox
      )
    }
  }
  
  private fun createPaymentSheetColors(themeParams: Map<String, Any>?): PaymentSheetColors {
    if (themeParams == null) return PaymentSheetColors()
    
    return PaymentSheetColors(
      surface = parseColor(themeParams["surface"] as? String),
      textColor = parseColor(themeParams["text"] as? String),
      containerColor = parseColor(themeParams["container"] as? String),
      errorLabelColor = parseColor(themeParams["errorLabel"] as? String),
      tokenizeButtonColor = parseColor(themeParams["tokenizeButton"] as? String),
      tokenizeButtonTextColor = parseColor(themeParams["tokenizeButtonText"] as? String),
      cancelButtonColor = parseColor(themeParams["cancelButton"] as? String),
      cancelButtonTextColor = parseColor(themeParams["cancelButtonText"] as? String)
    )
  }
  
  private fun parseColor(hex: String?): Color {
    if (hex == null) return Color.Unspecified
    
    return try {
      val colorString = hex.removePrefix("#")
      val colorInt = colorString.toLong(16).toInt()
      
      if (colorString.length == 6) {
        // RGB format, add full alpha
        Color(0xFF000000.toInt() or colorInt)
      } else if (colorString.length == 8) {
        // ARGB format
        Color(colorInt)
      } else {
        Color.Unspecified
      }
    } catch (e: Exception) {
      Color.Unspecified
    }
  }
}