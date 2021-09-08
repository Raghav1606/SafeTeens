package com.safeteens.child.database.firebase;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safeteens.child.pojos.Message;

public class FirestoreService {

    public static void writeObjectToDocumentRef(final DocumentReference documentReference,
                                                final Object object, final FirebaseServiceInterfaces.writeFirebaseCallback writeFirebaseCallback){
        documentReference.set(object).addOnSuccessListener(aVoid -> writeFirebaseCallback.onWriteCallBack(true));
    }

    public static void saveMessage(Message message, FirebaseServiceInterfaces.writeFirebaseCallback writeFirebaseCallback){
        writeObjectToDocumentRef(FirebaseFirestore.getInstance().collection("messages_raghav")
                .document(message.getId()), message, writeFirebaseCallback);

    }
}
