# Smart Business Tracking

Smart Business is designed for small business owners and freelancers to track their daily income and performance targets.

## Features
- **Income Logging**: Track `Amount`, `Customer`, `Reason`, `Category`, and `Payment Method`.
- **Monthly Targets**: Set a goal and track progress via a visual progress bar.
- **Reporting**: Monthly summary and category breakdown views.

## Data & Persistence
- **Local**: Primarily uses the `BusinessIncomeEntity` and `BusinessTargetEntity` in the Room database.
- **Cloud**: Business data is backed up to Firestore during a global app backup (`BackupUtils.kt`).

## Main Screens
- **`SmartBusinessScreen.kt`**: Main dashboard with progress cards and income list.
- **`SmartBusinessViewModel.kt`**: Handles CRUD operations on the local database and filtering logic.

## Behavior
- **Filtering**: Records can be filtered by Category (Product Sales, Repairs, Services, etc.).
- **Isolation**: Records are scoped to the current user's local database file.

## Testing Checklist
- [ ] Adding an income updates the "Received" amount in the progress card.
- [ ] Percentage achieved correctly handles 0-target scenarios.
- [ ] Deleting a record requires confirmation.
