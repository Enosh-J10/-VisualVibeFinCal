# User Profile & Avatar System

FinCalc manages user profiles with a focus on privacy and efficiency, specifically regarding media handling.

## Data Storage
- **Cloud**: Profile metadata (Name, Email, FinCalc ID) is stored in Firestore at `users/{uid}`.
- **Local Media**: Profile pictures are **not** stored in Firebase Storage. They are saved as `.jpg` files in the app's internal storage: `filesDir/profile_pictures/profile_$uid.jpg`.

## Profile Picture Logic
1. **Selection**: Uses the Android Photo Picker (`PickVisualMedia`).
2. **Persistence**: The selected image is copied to the local internal directory to ensure it survives cache clears.
3. **Reference**: The local `file://` URI is saved in `UserPrefs` for fast loading.
4. **Fallback**: If no image is set, the UI displays a `UserAvatar` component that generates initials from the user's name with a deterministic background color.

## Key Classes
- **`UserUtils.kt`**: Contains the logic for copying images to local storage and deleting them.
- **`SettingsScreen.kt`**: Provides the UI for uploading or removing the profile picture.
- **`CommonComponents.kt`**: Contains the `UserAvatar` Composable used throughout the app.

## Rationale
- **Cost**: Eliminates egress costs and storage fees associated with Firebase Storage.
- **Speed**: Instant loading from local disk without network latency.
- **Privacy**: User avatars never leave the physical device unless the user performs a manual backup.

## Development Rules
- Do not attempt to refactor the app to use Firebase Storage for avatars without an explicit architectural requirement.
- Always handle the case where the local file might be missing (e.g., app data wipe) by falling back to initials.
