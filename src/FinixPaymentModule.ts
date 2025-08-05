import { NativeModule, requireNativeModule } from 'expo-modules-core'

export type Environment =
  | 'Sandbox'
  | 'Live'

export type SheetType =
  | 'complete'
  | 'partial'
  | 'basic'
  | 'minimal'
  | 'basicBank'

export interface ColorTheme {
  surface?: string
  label?: string
  text?: string
  container?: string
  cancelButton?: string
  cancelButtonText?: string
  tokenizeButton?: string
  tokenizeButtonText?: string
  errorLabel?: string
  logoText?: string
}

export interface Expiration {
  month: number
  year: number
}

export interface CardParams {
  name: string
  cardNumber: string
  expiration: Expiration
  securityCode: string
}

export interface AddressParams {
  line1: string
  line2?: string
  city: string
  region: string
  postalCode: string
  country: string
}

export interface TokenResponse {
  id: string
  fingerprint: string
  created: number   // epoch seconds
  updated: number   // epoch seconds
  instrument: 'card' | 'bank'
  expires: number   // epoch seconds
  isoCurrency: 'USD'
}

export type FinixPaymentEvents = {
  onPaymentSuccess: (token: TokenResponse) => void
  onPaymentCancel: () => void
  onPaymentError: (error: { message: string }) => void
}

declare class FinixPaymentModule extends NativeModule<FinixPaymentEvents> {
  /** Must call once at app startup */
  initSDK(applicationId: string, environment: Environment): Promise<void>

  /**
   * style: one of the SheetType strings
   * themeParams: any subset of ColorTheme
   * showCancelButton: default true
   * showCancelItem:   default true
   * card: optional prefilled card object
   * showCountry: default false
   * address: optional billing address
   */
  presentPaymentSheet(
    sheetType: SheetType,
    themeParams?: ColorTheme,
    showCancelButton?: boolean,
    showCancelItem?: boolean,
    card?: CardParams,
    showCountry?: boolean,
    address?: AddressParams
  ): Promise<void>

  /**
   * showCancelButton: default true
   * showCancelItem:   default true
   * accountType:      one of the BankAccountType raw-values
   */
  presentBankPaymentSheet(
    showCancelButton?: boolean,
    showCancelItem?: boolean,
    accountType?: 'personalChecking' | 'personalSavings' | 'businessChecking' | 'businessSavings'
  ): Promise<void>
}

export default requireNativeModule<FinixPaymentModule>('FinixPaymentModule')