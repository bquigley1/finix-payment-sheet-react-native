# finix-payment-sheet-react-native

**Unofficial** React Native / Expo Turbo Module for integrating the **Finix Payment Sheet** on iOS and Android.
Provides a unified JavaScript API to launch the Finix native payment sheet in both bare React Native and Expo (managed or bare) projects.

## Features

- 🎨 Multiple payment sheet styles (Complete, Partial, Basic, Minimal)
- 🏦 Bank/ACH payment support
- 🎯 Full TypeScript support
- 🖌️ Customizable theming
- 📱 iOS and Android native implementations
- 🔐 Secure tokenization

## Installation

```bash
npm install finix-payment-sheet-react-native
# or
yarn add finix-payment-sheet-react-native
```

## Usage

```typescript
import FinixPaymentModule from 'finix-payment-sheet-react-native';

// Initialize the SDK (required once at app startup)
await FinixPaymentModule.initSDK('YOUR_APPLICATION_ID', 'Sandbox'); // or 'Live'

// Set up event listeners
FinixPaymentModule.addListener('onPaymentSuccess', (token) => {
  console.log('Payment successful:', token);
  // Token contains: id, fingerprint, created, updated, instrument, expires, isoCurrency
});

FinixPaymentModule.addListener('onPaymentCancel', () => {
  console.log('Payment cancelled by user');
});

FinixPaymentModule.addListener('onPaymentError', (error) => {
  console.error('Payment error:', error.message);
});

// Present payment sheet
await FinixPaymentModule.presentPaymentSheet(
  'complete',           // Sheet type: 'complete' | 'partial' | 'basic' | 'minimal'
  {                     // Optional theme customization
    surface: '#FFFFFF',
    label: '#000000',
    text: '#000000',
    container: '#F5F5F5',
    cancelButton: '#FF0000',
    cancelButtonText: '#FFFFFF',
    tokenizeButton: '#007AFF',
    tokenizeButtonText: '#FFFFFF',
    errorLabel: '#FF0000',
    logoText: '#000000'
  },
  true,                 // showCancelButton
  true,                 // showCancelItem
  {                     // Optional pre-filled card data
    name: 'John Doe',
    cardNumber: '4111111111111111',
    expiration: { month: 12, year: 2025 },
    securityCode: '123'
  },
  false,                // showCountry
  {                     // Optional billing address
    line1: '123 Main St',
    line2: 'Apt 4B',
    city: 'New York',
    region: 'NY',
    postalCode: '10001',
    country: 'US'
  }
);

// For bank/ACH payments
await FinixPaymentModule.presentBankPaymentSheet(
  true,                 // showCancelButton
  true,                 // showCancelItem
  'personalChecking'    // Optional: 'personalChecking' | 'personalSavings' | 'businessChecking' | 'businessSavings'
);
```

## API Reference

### Types

```typescript
type Environment = 'Sandbox' | 'Live';

type SheetType = 'complete' | 'partial' | 'basic' | 'minimal' | 'basicBank';

interface ColorTheme {
  surface?: string;
  label?: string;
  text?: string;
  container?: string;
  cancelButton?: string;
  cancelButtonText?: string;
  tokenizeButton?: string;
  tokenizeButtonText?: string;
  errorLabel?: string;
  logoText?: string;
}

interface TokenResponse {
  id: string;
  fingerprint: string;
  created: number;      // epoch seconds
  updated: number;      // epoch seconds
  instrument: 'card' | 'bank';
  expires: number;      // epoch seconds
  isoCurrency: 'USD';
}
```

### Methods

#### `initSDK(applicationId: string, environment: Environment): Promise<void>`
Initialize the Finix SDK. Must be called once at app startup.

#### `presentPaymentSheet(...): Promise<void>`
Present a payment sheet for card tokenization.

#### `presentBankPaymentSheet(...): Promise<void>`
Present a payment sheet for bank account tokenization.

### Events

- `onPaymentSuccess`: Fired when tokenization succeeds
- `onPaymentCancel`: Fired when user cancels the payment sheet
- `onPaymentError`: Fired when an error occurs

## Requirements

- React Native 0.70+
- iOS 13.0+
- Android API 24+

## License

MIT

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and feature requests, please use the [GitHub issues page](https://github.com/bquigley1/finix-payment-sheet-react-native/issues).
