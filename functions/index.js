const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

exports.sendChatNotification = onDocumentCreated(
"notifications/{receiverUid}/items/{notificationId}",
async (event) => {
try {
const snap = event.data;
if (!snap) {
console.log("No notification snapshot found.");
return;
}

```
  const data = snap.data();
  const receiverUid = event.params.receiverUid;
  const notificationId = event.params.notificationId;

  if (!data) {
    console.log("No notification data found.");
    return;
  }

  const fromUid = data.fromUid || "";
  const toUid = data.toUid || receiverUid;

  if (!receiverUid || !toUid || receiverUid !== toUid) {
    console.log("Receiver UID mismatch.", { receiverUid, toUid });
    return;
  }

  if (fromUid === toUid) {
    console.log("Skipping self-notification.");
    return;
  }

  const receiverDoc = await admin
    .firestore()
    .collection("users")
    .doc(receiverUid)
    .get();

  if (!receiverDoc.exists) {
    console.log("Receiver user document not found.", receiverUid);
    return;
  }

  const receiverData = receiverDoc.data() || {};
  const fcmToken = receiverData.fcmToken;

  if (!fcmToken) {
    console.log("No FCM token found for receiver.", receiverUid);
    await snap.ref.set(
      {
        delivered: false,
        deliveryError: "NO_FCM_TOKEN",
        checkedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    return;
  }

  const title = data.title || "New message";
  const body = data.body || "You received a new message";

  const message = {
    token: fcmToken,
    notification: {
      title,
      body,
    },
    data: {
      type: String(data.type || "chat_message"),
      chatId: String(data.chatId || ""),
      fromUid: String(fromUid || ""),
      toUid: String(toUid || ""),
      messageId: String(data.messageId || notificationId || ""),
    },
    android: {
      priority: "high",
      notification: {
        channelId: "chat_messages",
        sound: "default",
        priority: "high",
      },
    },
  };

  const response = await admin.messaging().send(message);

  await snap.ref.set(
    {
      delivered: true,
      deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
      fcmResponse: response,
    },
    { merge: true }
  );

  console.log("Chat notification sent.", {
    receiverUid,
    notificationId,
    response,
  });
} catch (error) {
  console.error("sendChatNotification failed:", error);

  if (event.data) {
    await event.data.ref.set(
      {
        delivered: false,
        deliveryError: error.message || "UNKNOWN_ERROR",
        checkedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
  }
}
```

}
);
