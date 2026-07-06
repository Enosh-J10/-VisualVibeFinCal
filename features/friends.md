# Friends & Social System

The Friends system allows users to connect for collaboration in Smart Travel and Chat.

## Core Concepts
- **FinCalc ID**: A unique, 6-character alphanumeric code generated from the user's UID (e.g., `FIN-X9Y2Z4`).
- **Requests**: Handled via the `friendRequests` collection in Firestore.
- **Friendship**: Represented by a single document in the `friends` collection with a deterministic ID (`lowerUID_higherUID`).

## Main Screens
- **`FriendsScreen.kt`**: Tabbed interface for listing friends, pending requests, and searching.
- **`FriendsViewModel.kt`**: Manages real-time listeners for requests and friendship states.

## Key Behaviors
- **Search**: Users can search by Name, Email, or exact FinCalc ID.
- **Deep Linking**: Support for `fincalc://add-friend?id=ID` allows users to share their connection link via external apps.
- **Nicknames**: Users can set custom nicknames for their friends that are stored privately in their own profile (`users/{uid}/friendSettings/{friendId}`).
- **Blocking**: Blocked users are stored in `users/{uid}/blockedUsers/`. Blocking automatically removes the existing friendship.

## Safety Rules
- Only **Accepted** friends can be added to Smart Travel trips.
- Friendships must always be created using sorted UIDs to prevent duplicate records.
- `fromUid` in a request must always match the authenticated user's UID.
