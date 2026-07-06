# Data Models & Persistence (DATAMODEL.md)

This document describes the data structures used by FinCalc in both the local Room database and the Cloud Firestore backend.

---

## 1. Firestore Cloud Data

### Root Collections
| Path | Purpose | Key Fields |
| :--- | :--- | :--- |
| `users/{uid}` | User Profile | `name`, `email`, `finCalcId`, `profilePictureUrl` |
| `friends/{friendshipId}` | Friendship state | `memberUids` (Sorted list), `user1Uid`, `user2Uid` |
| `friendRequests/{requestId}` | Pending invites | `fromUid`, `toUid`, `status` ("pending", "accepted") |
| `chats/{chatId}` | Messaging rooms | `memberUids`, `lastMessage`, `lastMessageAt` |
| `trips/{tripId}` | Smart Travel trips | `name`, `createdByUid`, `memberUids`, `currencyCode` |
| `notes/{noteId}` | Cloud Notes (Legacy) | `uid`, `title`, `content` |

### Subcollections
- **`chats/{chatId}/messages/{messageId}`**: Text messages only.
- **`chats/{chatId}/status/{uid}`**: Real-time `isTyping` and `lastActive` timestamps.
- **`trips/{tripId}/expenses/{expenseId}`**: `amount`, `paidByUid`, `category`.
- **`trips/{tripId}/expenses/{expenseId}/flags/{flagId}`**: Disputes raised by members.
- **`users/{uid}/backups/latest`**: Single document containing the full app data JSON string.

---

## 2. Room Local Data

### Entities (`com.enosh.fincalc.data.local.entity`)
| Entity | Purpose |
| :--- | :--- |
| `Expense` | Personal daily spending records. |
| `Goal` | Savings targets with `targetAmount` and `savedAmount`. |
| `Budget` | Monthly limits mapped by `month` string (YYYY-MM). |
| `BusinessIncomeEntity`| Records for Smart Business income. |
| `BusinessTargetEntity`| Monthly income targets for business. |
| `ConversationEntity` | Local AI chat history (Titles). |
| `MessageEntity` | Local AI message history. |

### Database Strategy
- **Isolation**: Each user gets a unique database file: `fincalc_database_$uid.db`.
- **Guest Mode**: Uses `fincalc_database_guest.db`, which is wiped on logout.
- **Migrations**: Destructive migration is enabled (`fallbackToDestructiveMigration`). **Exercise caution when changing schemas.**

---

## 3. ID Conventions & Logic
- **Deterministic Friendship ID**: `sorted(uid1, uid2).join("_")`. Ensures only one document exists per pair.
- **Deterministic Chat ID**: Uses the same sorted UID logic as friendships.
- **FinCalc ID**: Generated via SHA-256 hash of the UID, truncated to 6 characters (e.g., `FIN-A1B2C3`).

---

## 4. Local Preferences (`SharedPreferences`)
- **`UserPrefs`**: `is_dark_mode`, `is_guest`, `keep_me_signed_in`.
- **`AssistantPrefs_$uid`**: `isRoastMode`, `personality`, `enabled`.
- **`secure_user_prefs`**: Managed by `SecurityUtils.kt`. Stores `app_pin_$uid` (hashed) and `biometric_enabled`. Includes fallback to `secure_user_prefs_fallback` on encryption errors.

---

## 5. Media & Assets
- **Avatars**: Stored as JPG files in `context.filesDir/profile_pictures/profile_$uid.jpg`.
- **Receipts**: Temporary capture in `cacheDir`. Permanently saved to Firestore only in Smart Travel (Legacy feature, currently discouraged for new logic without Storage review).

---
**Avoid storing large binary blobs in Firestore. Use JSON serialization for complex objects within single documents where appropriate.**
