# 1-on-1 Text Chat

FinCalc includes a secure messaging system for accepted friends to communicate about financial matters or shared trips.

## Design Constraints
- **Text Only**: The chat system is intentionally restricted to text only.
- **No Media**: Image, Video, and File sharing were removed to optimize performance and eliminate Firebase Storage costs/security complexity for this release.
- **Deterministic IDs**: Every chat room uses a `chatId` formed by `sortedUID1_sortedUID2`.

## Key Components
- **`ChatListScreen.kt`**: Overview of active conversations and unread badges.
- **`ChatRoomScreen.kt`**: Real-time messaging interface.
- **`ChatViewModel.kt`**: Manages message sending and real-time Firestore listeners.

## Features
- **Typing Indicators**: Real-time status update in `chats/{chatId}/status/{uid}`.
- **Online Status**: "Online" is displayed if the user's `lastActive` timestamp was within the last 60 seconds.
- **Unread Count**: Managed via a `unreadCounts` map in the chat room document.
- **FCM Triggers**: Sending a message creates a document in `notifications/` which can be used to trigger background notifications via Cloud Functions (Note: Currently optimized for Spark plan foreground notifications).

## Known Limitations
- Background notifications require the app to be in a cached state or foreground due to the lack of Cloud Functions in the standard Spark plan configuration.
- Messages are capped at 2,000 characters.

## Testing Checklist
- [ ] Message appears instantly for both users.
- [ ] Typing indicator appears when the other user types.
- [ ] "Unsend" (Delete) removes the message for both parties.
- [ ] Chat ID is identical regardless of who starts the conversation.
