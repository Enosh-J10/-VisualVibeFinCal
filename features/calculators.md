# Financial Calculators & Tools

FinCalc includes a robust suite of calculators for common financial and personal metrics.

## Available Tools
1. **Loan Calculator**: Fixed-rate loan estimations with monthly payment and total interest breakdown.
2. **Salary Calculator**: Converts gross salary to monthly, weekly, and daily estimates.
3. **Tax & Discount**: Calculates final price after applying percentage-based taxes or discounts.
4. **Tip & Split**: Simple tool for restaurant bills and group splitting.
5. **Currency Converter**: Real-time conversion using `https://open.er-api.com/`.
6. **BMI Calculator**: Basic health metric tool (Standard Guidelines 18+).
7. **Unit Converter**: Mass, length, and volume conversions.

## Design Patterns
- **Scaffold**: Most calculators use the `CalculatorScreenScaffold` for consistent UI.
- **Card-Based**: Input fields and results are grouped into `CalculatorCard` components.
- **Validation**: Inputs are sanitized via `ValidationUtils.formatNumericInput` to prevent crashes from invalid characters.

## Legal & Compliance
- **Disclaimer**: Every calculator screen displays a footer: *"Calculations are estimates and do not constitute financial advice."*
- **Precision**: Money calculations are generally handled using `Double` (Internal limitation, `BigDecimal` migration may be required for complex banking-grade rounding in the future).

## History System
- **Persistence**: Results are saved locally in the `history` map managed by `HistoryViewModel.kt`.
- **Clearing**: Users can clear history per-tool or globally.

## Development Rules
- Always validate numeric inputs before performing division.
- Ensure the disclaimer is visible and not cut off by the navigation bar.
