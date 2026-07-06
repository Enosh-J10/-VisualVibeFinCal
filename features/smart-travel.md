# Smart Travel: Collaborative Trip Expenses

Smart Travel is a real-time collaborative tool for groups to track and settle expenses during trips.

## Logic & Permissions
- **Admin**: The trip creator (`createdByUid`). Only the admin can:
    - Edit trip metadata (Name, Destination, Currency).
    - Delete the entire trip.
    - Resolve (delete) flags.
- **Member**: Users invited by the admin. All members can:
    - Add new expenses.
    - Edit/Delete their own expenses.
    - Flag any expense for review.
    - View settlements.

## Data Structure
- Root: `trips/{tripId}`
- Sub: `trips/{tripId}/expenses/{expenseId}`
- Sub: `trips/{tripId}/expenses/{expenseId}/flags/{flagId}`

## Key Workflows
- **Settlement**: Uses a "Minimal Transactions" algorithm in `SmartTravelViewModel.kt` to calculate who owes whom the most efficient way.
- **Flagging**: If a member disagrees with an expense (e.g., "I didn't eat that pizza"), they can "Flag" it. The admin is then notified to resolve the issue.
- **Finalization**: Admins can "Finalize" a trip to lock it for new expenses, indicating the trip is settled.

## Classes
- **`SmartTravelScreen.kt`**: Trip list and creation.
- **`TripDetailScreen.kt`**: The multi-tab hub (Expenses, Members, Insights, Settlement).
- **`SmartTravelViewModel.kt`**: Core business logic and settlement calculator.

## Security
- Firestore rules enforce that only members present in the `memberUids` or `createdByUid` fields can read trip data.
- Trip deletion uses a recursive batch delete (expenses -> flags -> trip).
