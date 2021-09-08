package com.safeteens.child.database;

import android.content.Context;
import androidx.room.Room;
import com.safeteens.child.database.firebase.FirebaseHelper;
import com.safeteens.child.database.firebase.FirebaseServiceInterfaces;
import com.safeteens.child.database.firebase.firestoreDao.ChateeDao;
import com.safeteens.child.database.firebase.firestoreDao.CheckpointsDao;
import com.safeteens.child.database.local.SafeTeensDatabase;
import com.safeteens.child.database.local.entities.Chatee;
import com.safeteens.child.database.local.entities.MessageReadCheckpoint;
import com.safeteens.child.database.local.entities.MessageSyncStatus;
import com.safeteens.child.enums.MessageSyncStates;
import com.safeteens.child.helpers.AsyncHelper;
import com.safeteens.child.pojos.Message;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Database helper for offline/online messages.
 */
public class MessageDatabaseHelper {

    SafeTeensDatabase localDb;
    ChateeDao chateeOnlineDao;
    CheckpointsDao checkpointsOnlineDao;

    public MessageDatabaseHelper(Context context) {
        localDb = Room.databaseBuilder(context, SafeTeensDatabase.class, "database-name").build();
        chateeOnlineDao = new ChateeDao();
        checkpointsOnlineDao = new CheckpointsDao();
    }

    /**
     * LOCAL DB METHODS
     */

    public void getLocalWhatsappChateeForSender(String phoneNumber, AsyncHelper.CallBack<Chatee> chateeCallBack){
        new AsyncHelper<Chatee>().asyncForSingle(localDb.chateeDao().getWhatsappChateeForSender(phoneNumber), chateeCallBack);
    }

    public void createAndSaveNewChattee(Chatee newChatee, AsyncHelper.CallBack<Long> chateeIdCallback){
        new AsyncHelper<Long>().asyncForSingle(localDb.chateeDao().insert(newChatee), new AsyncHelper.CallBack<Long>() {
                    @Override
                    public void onSuccess(Long insertedChatteeId) {
                        newChatee.setId(insertedChatteeId);

                        chateeIdCallback.onSuccess(insertedChatteeId);

                        MessageReadCheckpoint newChatteeMessageCheckpoint = new MessageReadCheckpoint();
                        newChatteeMessageCheckpoint.setChateeId(insertedChatteeId);
                        newChatteeMessageCheckpoint.setSource(newChatee.getChateeSource());
                        Calendar startMessageDate = Calendar.getInstance();
                        startMessageDate.add(Calendar.DAY_OF_MONTH, -7);
                        newChatteeMessageCheckpoint.setStartMessageDate(startMessageDate.getTime());

                        new Thread(() -> localDb.messageCheckpointsDao().insertAll(newChatteeMessageCheckpoint)).start();
                        uploadChateeOnline(newChatee);
                        uploadCheckpointsOnline(newChatteeMessageCheckpoint);
                    }

                    @Override
                    public void onError(Throwable error) {}
                });
    }


    public List<MessageReadCheckpoint> getLocalMessageBreakPointsForChatee(Chatee chatee){
        return localDb.messageCheckpointsDao().getCheckpointsForChateeIdOrderedByLatest(chatee.getId());
    }

    public void deleteCheckpoint(MessageReadCheckpoint checkpoint){
        localDb.messageCheckpointsDao().delete(checkpoint);
    }

    public void updateCheckpoint(MessageReadCheckpoint checkpoint){
        localDb.messageCheckpointsDao().updateMessageReadCheckpoint(checkpoint);
    }

    public void updateAndCreateCheckpoint(MessageReadCheckpoint update, MessageReadCheckpoint create){
        localDb.messageCheckpointsDao().updateAndCreateCheckpoint(update, create);
        uploadCheckpointsOnline(create);
        uploadCheckpointsOnline(update, create);
    }


    /**
     * ONLINE FIREBASE DB METHODS
     */

    public void checkOnlineDbConnectivity(){
        FirebaseHelper.checkConnectivity();
    }

    public Message uploadMessageOnline(final Message message){
        message.setId(FirebaseHelper.generateId(FirebaseHelper.MESSAGES_COLLECTION));
        localDb.messageSyncStatusDao().insertAll(new MessageSyncStatus(message.getId(), MessageSyncStates.NEW,
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis())));
        FirebaseHelper.saveMessage(message, new FirebaseServiceInterfaces.successfulOperationCallback() {
            @Override
            public void onSuccess() {
                new Thread(() -> {
                    MessageSyncStatus currentMessageSyncStatus = localDb.messageSyncStatusDao().getMessageSycnStatus(message.getId());
                    currentMessageSyncStatus.setSyncState(MessageSyncStates.UPLOADED);
                    currentMessageSyncStatus.setLastModified(new Date(System.currentTimeMillis()));
                    new Thread(() -> {
                        localDb.messageSyncStatusDao().updateMessageSyncStatus(currentMessageSyncStatus);
                    }).start();
                }).start();
            }

            @Override
            public void onFailure() {

            }
        });
        return message;
    }

    public List<Message> uploadMessagesOnline(final List<Message> messages){
        List<Message> uploadedMessages = new ArrayList<>();
        messages.forEach(message -> uploadedMessages.add(uploadMessageOnline(message)));
        return uploadedMessages;
    }

    public void uploadChateeOnline(final Chatee chatee) {
        chateeOnlineDao.saveOne(chatee, writeSuccessful -> {});
    }

    public void uploadCheckpointsOnline(MessageReadCheckpoint... checkpoints) {
        checkpointsOnlineDao.saveList(writeSuccessful -> {}, checkpoints);
    }
}
