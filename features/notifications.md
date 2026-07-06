# Notification System

FinCalc uses a hybrid notification system consisting of local "Fun Reminders" and foreground FCM-triggered chat notifications.

## 1. Fun Reminders (Local)
- **Purpose**: Engages users with financial tips or playful "roasts" based on their spending habits.
- **Engine**: `WorkManager`.
- **Worker**: `FunReminderWorker.kt`.
- **Frequency**: Configurable in Settings (Low, Medium, High).
- **Silent Hours**: Reminders are suppressed between 10 PM and 8 AM to avoid disturbing the user.

## 2. Chat Notifications
- **Status**: Foreground/Cached only.
- **Mechanism**:
    1. A message is sent via Firestore.
    2. A record is added to the `notifications/` collection.
    3. `FinCalcMessagingService.kt` (FCM) receives the event.
    4. If the app is active or in memory, a high-priority heads-up notification is displayed via `NotificationHelper.kt`.
- **Limitation**: Due to the Spark plan (Free Tier), background "Closed-App" notifications via Cloud Functions are not active.

## 3. Notification Channels
Defined in `NotificationHelper.kt`:
- `chat_messages`: High importance, lights, and vibration.
- `general_notifications`: Default importance.
- `fun_reminders`: Default importance.

## Implementation Notes
- **Permission**: The app requests `POST_NOTIFICATIONS` on Android 13+ in `HomeActivity`.
- **Nav Targets**: Tapping a notification uses `navigate_to` intent extras to deep-link directly into the relevant chat or tool.

## Things Not to Change
- Do not remove the `quiet hours` check in `FunReminderWorker`.
- Do not increase notification priority for "Fun Reminders" beyond `DEFAULT`.
