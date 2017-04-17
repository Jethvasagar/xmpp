package com.higuys.main;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.higuys.R;
import com.higuys.helper.ChatMessageHelper;
import com.higuys.helper.ExceptionHandler;
import com.higuys.helper.FriendHelper;
import com.higuys.helper.GroupHelper;
import com.higuys.helper.MessageJSON;
import com.higuys.helper.TableHelper;
import com.higuys.service.ChatService;
import com.higuys.utils.Constants;
import com.higuys.utils.StoreUserData;
import com.higuys.utils.Utils;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.provided.SASLDigestMD5Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.util.Log.i;
import static org.jivesoftware.smack.packet.IQ.Type.set;

/**
 * Created by Arth Tilva on 15-07-2016.
 */
public class MyApplication extends Application {
    private XMPPTCPConnection connection;
    private MessageListener messageListener;
    private GroupMessageListener groupMessageListener;
    private boolean isFail;
    private String connectionStatus = "connected";

    protected void displayNotification() {
        i("Start", "notification");

   /* Invoking the default notification service */
        try {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setAutoCancel(true);
            mBuilder.setContentTitle(getResources().getString(R.string.app_name));
            mBuilder.getNotification().flags |= NotificationCompat.FLAG_AUTO_CANCEL;
            mBuilder.setContentText("You've received new message.");
            mBuilder.setTicker("New Message Alert!");
            mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
            mBuilder.setCategory(Notification.CATEGORY_MESSAGE);
            mBuilder.setSmallIcon(R.drawable.logo);
            Bitmap icon = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                    R.drawable.logo);
            mBuilder.setLargeIcon(icon);

            ArrayList<ChatMessageHelper> messageHelpers = new TableHelper().getUnReadMessages(getApplicationContext());
            mBuilder.setNumber(messageHelpers.size());

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            String msg = "";
            String[] events = new String[messageHelpers.size()];
            String users = null;
            for (int i = 0; i < messageHelpers.size(); ++i) {
                ChatMessageHelper chatMessageHelper = messageHelpers.get(i);
                //Roster roster = Roster.getInstanceFor(connection);
                String username = chatMessageHelper.from;
                if (chatMessageHelper != null) {
                    if (chatMessageHelper.group != null) {
                        if (chatMessageHelper.group.trim().length() == 0) {
                            ArrayList<FriendHelper> friendHelpers = new TableHelper().getUser(getApplicationContext(), username);
                            if (friendHelpers.size() > 0) {
                                username = friendHelpers.get(0).first + " " + friendHelpers.get(0).last;
                            }
                        } else {
                            username = chatMessageHelper.group;
                            String to = chatMessageHelper.from;
                            i("NOTIFICATION_From=>to", chatMessageHelper.from + "=" + chatMessageHelper.group);
                            try {
                                if (to.contains("@"))
                                    to = to.substring(0, to.indexOf("@"));
                                ArrayList<FriendHelper> lst = new TableHelper().getUser(getApplicationContext(), to);
                                FriendHelper friendHelper = lst.get(0);
                                username = friendHelper.first + " " + friendHelper.last + "@" + username.substring(0, username.indexOf("@"));
                            } catch (Exception e) {
                                e.printStackTrace();
                                username = chatMessageHelper.group;
                            }

                        }
                    }
                }
                Log.d("usser", username);
                if (users == null) {
                    if (chatMessageHelper.group == null || chatMessageHelper.group.trim().length() == 0)
                        users = chatMessageHelper.from;
                    else
                        users = chatMessageHelper.group;
                } else {
                    if (chatMessageHelper.group == null && users.equals(chatMessageHelper.from))
                        users = chatMessageHelper.from;
                    else if (users.equals(chatMessageHelper.group))
                        users = chatMessageHelper.group;
                    else
                        users = "";

                }
                Log.d("KARtik", users + " K");
                events[i] = username + ":" + chatMessageHelper.msg;

                /*if (chatMessageHelper.type.equals(ChatMessageHelper.MessageType.CHAT))
                else if (chatMessageHelper.type.equals(ChatMessageHelper.MessageType.VIDEO))
                    events[i] = username + ":" + " sent one video";
                else if (chatMessageHelper.type.equals(ChatMessageHelper.MessageType.IMAGE))
                    events[i] = username + ":" + " sent one image";
                else if (chatMessageHelper.type.equals(ChatMessageHelper.MessageType.OTHER))
                    events[i] = username + ":" + " sent one file";*/
                msg += events[i] + "\n";
            }
            //Log.d("Notification", msg);

            inboxStyle.setBigContentTitle("Messages:");

            for (int i = 0; i < events.length; i++) {
                inboxStyle.addLine(events[i]);
            }

            mBuilder.setStyle(inboxStyle);
            if (users == null)
                return;
            Intent resultIntent = new Intent(this, MainActivity.class);
            Log.d("Users", users);
            resultIntent.putExtra("start", "chat");
            resultIntent.putExtra("regMob", users);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 108, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(108);
            i("NOTIFICATION", "MYAPPLICATION");
            mNotificationManager.notify(108, mBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }


    public boolean checkInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setGroupMessageListener(GroupMessageListener messageListener) {
        this.groupMessageListener = messageListener;
    }

    public void setLoginListener(XMPPTCPConnection connection) {
        this.connection = connection;
    }

    public interface setLoginListener {
        public void onLogin(XMPPTCPConnection connection);
    }

    private void setGroupMessageListener(final MultiUserChat multiUserChat) {
        i("TAG", "setGroupMessageListener: ");
        multiUserChat.addMessageListener(new org.jivesoftware.smack.MessageListener() {
            @Override
            public void processMessage(Message message) {
                i("TAG", "processMessage: " + message.getSubject());

                if (message != null && message.getBody() != null) {
                    String user = message.getFrom();

                    i("TAG", "processMessage: " + user);
                    if (user.contains(Constants.IP)) {
                        user = user.substring(user.indexOf(Constants.IP) + Constants.IP.length());
                        user = user.replace("/", "");
                        Log.d("User", user);
                    }
                    if (user.equals(new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE)))
                        return;
                    /*chatMessageHelper.from = regMob;
                    chatMessageHelper.group = multiUserChat.getRoom().substring(0, multiUserChat.getRoom().indexOf("@"));
                    chatMessageHelper.to = message.getTo();
                    chatMessageHelper.filePath = "";
                    chatMessageHelper.chatId = message.getStanzaId();
                    chatMessageHelper.msg = message.getBody().trim();
                    chatMessageHelper.msg_time = getCurrentTime();
                    chatMessageHelper.sent = regMob.contains(new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE));
                    chatMessageHelper.msgStatus = TableHelper.MsgStatus.RECEIVED;*/

                    final ChatMessageHelper chatMessageHelper = new ChatMessageHelper();
                    Reader reader = new StringReader(message.getBody());
                    Gson gson = new Gson();
                    MessageJSON messageJSON;
                    try {
                        messageJSON = gson.fromJson(reader, MessageJSON.class);
                        chatMessageHelper.msg = messageJSON.message;
                        chatMessageHelper.messageType = messageJSON.type;
                    } catch (Exception e) {
                        chatMessageHelper.msg = message.getBody();
                        chatMessageHelper.messageType = MessageJSON.MessageType.CHAT;
                    }
                    if (user.contains("@")) {
                        chatMessageHelper.from = user.substring(0, user.indexOf("@"));
                    } else {
                        chatMessageHelper.from = user;
                    }
                    if (message.getTo().contains("@")) {
                        chatMessageHelper.to = message.getTo().substring(0, message.getTo().indexOf("@"));
                    } else {
                        chatMessageHelper.to = message.getTo();
                    }
                    chatMessageHelper.msgStatus = TableHelper.MsgStatus.RECEIVED;
                    chatMessageHelper.group = multiUserChat.getRoom().substring(0, multiUserChat.getRoom().indexOf("@"));
                    chatMessageHelper.filePath = "";
                    chatMessageHelper.uploadId = "";
                    chatMessageHelper.chatId = message.getStanzaId();
                    chatMessageHelper.sent = user.contains(new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE));
                    chatMessageHelper.read = false;
                    chatMessageHelper.msg_time = getCurrentTime();


                    TableHelper tableHelper = new TableHelper();
                    if (!tableHelper.isAlreadyGroupChatExist(getApplicationContext(), chatMessageHelper.chatId)) {
                        //if (new StoreUserData(getApplicationContext()).getBoolean(Constants.IS_GROUP_CHAT_OPEN))
                        tableHelper.insertGroupChat(getApplicationContext(), chatMessageHelper);
                        if (groupMessageListener != null)
                            groupMessageListener.msgReceived(chatMessageHelper);
                        //groupMessageListener.msgReceived(message.getFrom(), message.getBody(), chatMessageHelper.group);
                        if (!new StoreUserData(getApplicationContext()).getBoolean(Constants.IS_GROUP_CHAT_OPEN))
                            //displayNotification();
                            i("TAG", "processMessage: notification");
                    } else {
                        i("TAG", "processMessage: already exist");
                    }
                    /*if (!tableHelper.isAlreadyInsertedGroupUser(getApplicationContext(), chatMessageHelper.from)) {
                        final Call<ResponseBody> result;
                        //TODO: fetch list of users for group from XMPP
                        result = null;
                        //result = Utils.callApi().allUserList(chatMessageHelper.from, "true");
                        result.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                                String result = null;
                                try {
                                    result = response.body().string();

                                    if (result.trim().length() > 0) {
                                        try {
                                            final JSONObject jsonObject = new JSONObject(result);
                                            if (jsonObject.getString("result").equals("true")) {
                                                if (jsonObject.has("rows")) {
                                                    TableHelper tableHelper = new TableHelper();
                                                    JSONArray array = jsonObject.getJSONArray("rows");
                                                    for (int i = 0; i < array.length(); ++i) {
                                                        JSONObject object = array.getJSONObject(i);
                                                        FriendHelper friendHelper = new FriendHelper();
                                                        friendHelper.regMob = object.getString("device_id");
                                                        friendHelper.last = object.getString("last_name");
                                                        friendHelper.first = object.getString("first_name");
                                                        if (!tableHelper.isAlreadyInsertedGroupUser(getApplicationContext(), friendHelper.regMob))
                                                            tableHelper.insertGroupUser(getApplicationContext(), friendHelper);
                                                    }
                                                    if (!tableHelper.isAlreadyGroupChatExist(getApplicationContext(), chatMessageHelper.chatId)) {

                                                        tableHelper.insertGroupChat(getApplicationContext(), chatMessageHelper);
                                                        if (groupMessageListener != null)
                                                            groupMessageListener.msgReceived(chatMessageHelper.from, chatMessageHelper.msg, chatMessageHelper.group);
                                                        if (!new StoreUserData(getApplicationContext()).getBoolean(Constants.IS_GROUP_CHAT_OPEN))
                                                            displayNotification();
                                                    }
                                                }
                                            }
                                        } catch (JSONException e) {

                                            e.printStackTrace();
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                            }
                        });
                    } else {*/
                    /*}*/
                } else if (message.getSubject() != null) {
                    GroupHelper helper = new GroupHelper();
                    helper.groupId = multiUserChat.getRoom().substring(0, multiUserChat.getRoom().indexOf("@"));
                    new TableHelper().updateGroup(getApplicationContext(), helper.groupId, message.getSubject(), "");
                }
            }
        });
    }

    private ChatService.LoginListener loginListener;

    public void setLoginListener(ChatService.LoginListener loginListener) {
        this.loginListener = loginListener;
    }

    public void createNewAccount() {

        if (!checkInternetAvailable()) {
            return;
        }
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration
                            .builder()
                            .setServiceName(Constants.SERVICE_NAME)
                            .setHost(Constants.IP)
                            .setConnectTimeout(10000)
                            .setPort(Constants.XMPP_PORT)
                            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                            .build();

                    connection = new XMPPTCPConnection(conf);
                    connection.connect();
                    StoreUserData storeUserData = new StoreUserData(getApplicationContext());
                    storeUserData.getBoolean(Constants.IS_ACCOUNT_CREATED);
                    Log.d("JJ", storeUserData.getBoolean(Constants.IS_ACCOUNT_CREATED) + " ");
                    if (!storeUserData.getBoolean(Constants.IS_ACCOUNT_CREATED)) {
                        try {
                            AccountManager accountManager = AccountManager.getInstance(connection);
                            accountManager.sensitiveOperationOverInsecureConnection(true);

                            HashMap<String, String> attributes = new HashMap<>();
                            attributes.put("username", storeUserData.getString(Constants.USER_REG_MOBILE));
                            attributes.put("password", storeUserData.getString(Constants.USER_REG_MOBILE));
                            attributes.put("name", storeUserData.getString(Constants.USER_FIRST_NAME) + " " + storeUserData.getString(Constants.USER_LAST_NAME));
                            attributes.put("email", storeUserData.getString(Constants.USER_EMAIL));
                            attributes.put("phone", storeUserData.getString(Constants.USER_REG_MOBILE));
                            attributes.put("last", storeUserData.getString(Constants.USER_LAST_NAME));
                            attributes.put("first", storeUserData.getString(Constants.USER_FIRST_NAME));
                            accountManager.createAccount(storeUserData.getString(Constants.USER_REG_MOBILE), storeUserData.getString(Constants.USER_REG_MOBILE), attributes);
                            storeUserData.setBoolean(Constants.IS_ACCOUNT_CREATED, true);

                            isFail = false;
                        } catch (SmackException.NoResponseException | SmackException.NotConnectedException e) {
                            e.printStackTrace();
                            ExceptionHandler.appendLog(e);
                            isFail = true;
                        } catch (XMPPException.XMPPErrorException e) {
                            //regMob is already registered
                            e.printStackTrace();
                            storeUserData.setBoolean(Constants.IS_ACCOUNT_CREATED, true);
                            isFail = true;
                            ExceptionHandler.appendLog(e);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ExceptionHandler.appendLog(e);
                            isFail = true;
                        }
                    }

                    SASLMechanism mechanism = new SASLDigestMD5Mechanism();
                    SASLAuthentication.registerSASLMechanism(mechanism);
                    SASLAuthentication.blacklistSASLMechanism("PLAIN");
                    connection.login(storeUserData.getString(Constants.USER_REG_MOBILE), storeUserData.getString(Constants.USER_REG_MOBILE));
                    Presence presence = new Presence(Presence.Type.available);
                    connection.sendStanza(presence);

                    PingManager.setDefaultPingInterval(1);
                    PingManager pingManager = PingManager.getInstanceFor(connection);
                    pingManager.pingMyServer();
                    pingManager.registerPingFailedListener(new PingFailedListener() {
                        @Override
                        public void pingFailed() {
                            Log.d("appendLog", "pingFailed");

                        }
                    });

                    ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
                    reconnectionManager.enableAutomaticReconnection();
                    reconnectionManager.setFixedDelay(1500); // 15 * 1000
                    connection.addConnectionListener(new ConnectionListener() {
                        @Override
                        public void connected(XMPPConnection connection) {
                            i("TAG", "xmpp connected");
                            ArrayList<ChatMessageHelper> chatMessageHelpers = new TableHelper().getUnSentMessages(getApplicationContext());
                            for (int i = chatMessageHelpers.size() - 1; i > -1; i--) {
                                ChatMessageHelper chatMessageHelper = chatMessageHelpers.get(i);
                                i("UNSENT_MESSAGE_TO", chatMessageHelper.to);
                                i("UNSENT_MESSAGE_FROM", chatMessageHelper.from);
                                i("UNSENT_MESSAGE_MESS", chatMessageHelper.msg);
                                sendChatMessage(chatMessageHelper, true);
                            }
                        }

                        @Override
                        public void authenticated(XMPPConnection connection, boolean resumed) {

                        }

                        @Override
                        public void connectionClosed() {

                        }

                        @Override
                        public void connectionClosedOnError(Exception e) {

                        }

                        @Override
                        public void reconnectionSuccessful() {

                        }

                        @Override
                        public void reconnectingIn(int seconds) {

                        }

                        @Override
                        public void reconnectionFailed(Exception e) {

                        }
                    });
                    isFail = false;


                    return true;
                } catch (Exception ex) {
                    //Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost());
                    ex.printStackTrace();
                    Log.e("XMPPClient", ex.toString());
                    ExceptionHandler.appendLog(ex);
                    isFail = true;
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean aVoid) {
                super.onPostExecute(aVoid);
                Log.d("CREATE ACC", aVoid + "");
                if (isFail) {
                    if (loginListener != null)
                        loginListener.onError();
                    return;
                }
                if (!connection.isAuthenticated() && isOnline(getApplicationContext())) {
                    if (loginListener != null)
                        loginListener.onError();
                    return;
                }
                if (!aVoid) {
                    if (loginListener != null)
                        loginListener.onError();
                } else {
                    if (loginListener != null)
                        loginListener.onSuccess();
                    setXMPPConnection(connection);
                    new StoreUserData(getApplicationContext()).setBoolean(Constants.IS_APP_OPEN, true);
                    sendPendingMsg();

                    Roster roster = Roster.getInstanceFor(connection);

                    Collection<RosterGroup> entriesGroup = roster.getGroups();
                    Iterator<RosterGroup> groupIterator = entriesGroup.iterator();

                    while (groupIterator.hasNext()) {
                        RosterGroup rosterGroup = groupIterator.next();

                        if (!new TableHelper().isContainsGroup(getApplicationContext(), rosterGroup.getName())) {
                            new TableHelper().insertGroup(getApplicationContext(), rosterGroup.getName(), rosterGroup.getName(), "");
                        }
                        i("TAG", "onPostExecute: groups = " + rosterGroup.getName());
                        List<RosterEntry> rosterEntries = rosterGroup.getEntries();
                        for (RosterEntry rosterEntry : rosterEntries) {
                            i("TAG", "onPostExecute: entry " + rosterEntry.getUser());
                        }
                    }
                    new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... params) {
                            join_groups();
                            return null;
                        }
                    }.execute();
                }
            }
        }.execute();
    }

    public void reConnect() {
        if (connectionStatus.equals("")) {
            connectionStatus = "re-connecting";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.connect();
                        connection.login();
                        connectionStatus = "connected";
                    } catch (Exception e) {
                        ExceptionHandler.appendLog(e);
                    }
                }
            }).start();

        }
    }

    public void join_groups() {
        ArrayList<GroupHelper> groupHelperArrayList = new TableHelper().getGroupList(getApplicationContext());
        for (GroupHelper helper : groupHelperArrayList) {
            try {
                setGroupChatListener(helper);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendChatMessage(ChatMessageHelper chatMessageHelper) {
        SendMessage sendMessage = new SendMessage(chatMessageHelper);
        sendMessage.execute();
    }

    public void sendTypingStatus(ChatMessageHelper chatMessageHelper, boolean isTypingStatus, boolean typingStatus) {
        SendMessage sendMessage = new SendMessage(chatMessageHelper, isTypingStatus, typingStatus);
        sendMessage.execute();
    }

    public void sendChatMessage(ChatMessageHelper chatMessageHelper, boolean pending) {
        SendMessage sendMessage = new SendMessage(chatMessageHelper);
        if (pending) {
            sendMessage.setPending(true);
        }
        sendMessage.execute();
    }

    public void sendChatMessage(ChatMessageHelper chatMessageHelper, String message) {
        SendMessage sendMessage = new SendMessage(chatMessageHelper, message);
        sendMessage.execute();
    }

    class SendMessage extends AsyncTask<Void, Void, Void> {

        private ChatMessageHelper chatMessageHelper;
        private String message;
        private String send_id;
        boolean result = false;
        private boolean isPending = false;
        private boolean typingStatus = false;
        private boolean isTypingStatus = false;

        public SendMessage(ChatMessageHelper messageHelper, boolean isTypingStatus, boolean typingStatus) {
            this.chatMessageHelper = messageHelper;
            if (messageHelper.group != null && messageHelper.group.trim().length() > 0)
                send_id = chatMessageHelper.to + "@conference." + Constants.IP;
            else
                send_id = chatMessageHelper.to + "@" + Constants.IP;
            message = null;
            isPending = false;
            this.isTypingStatus = isTypingStatus;
            this.typingStatus = typingStatus;
        }


        public SendMessage(ChatMessageHelper messageHelper) {
            this.chatMessageHelper = messageHelper;
            if (messageHelper.group != null && messageHelper.group.trim().length() > 0)
                send_id = chatMessageHelper.to + "@conference." + Constants.IP;
            else
                send_id = chatMessageHelper.to + "@" + Constants.IP;
            message = null;
            isPending = false;
        }

        public SendMessage(ChatMessageHelper messageHelper, String message) {
            this.chatMessageHelper = messageHelper;
            if (messageHelper.group != null && messageHelper.group.trim().length() > 0)
                send_id = chatMessageHelper.to + "@conference." + Constants.IP;
            else
                send_id = chatMessageHelper.to + "@" + Constants.IP;
            this.message = message;
            isPending = false;
        }

        public void setPending(boolean pending) {
            isPending = pending;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d("sendMsg", send_id + " --> " + chatMessageHelper.msg);
                Message.Type type = Message.Type.chat;
                Message msg;
                msg = new Message(send_id, type);

                if (isTypingStatus) {
                    if (typingStatus) {
                        ChatStateExtension state = new ChatStateExtension(ChatState.composing);
                        msg.addExtension(state);
                    } else {
                        ChatStateExtension state = new ChatStateExtension(ChatState.paused);
                        msg.addExtension(state);
                    }
                } else {
                    //TODO: Add group condition here.
                    Gson gson = new Gson();
                    MessageJSON msgJson = new MessageJSON();
                    msgJson.type = chatMessageHelper.messageType;
                    msgJson.message = chatMessageHelper.msg;
                    i("TAG", "doInBackground: message sent= " + gson.toJson(msgJson));
                    msg.setBody(gson.toJson(msgJson));

                    if (chatMessageHelper.group != null && chatMessageHelper.group.trim().length() > 0) {
                        type = Message.Type.groupchat;
                    } else {
                        type = Message.Type.chat;
                    }
                    msg.setType(type);
                    msg.addExtension(new DeliveryReceipt(msg.getStanzaId()));
                }
                if (connection != null) {
                    chatMessageHelper.msgStatus = TableHelper.MsgStatus.SENT;
                    chatMessageHelper.chatId = msg.getStanzaId();
                    connection.sendStanza(msg);
                    i("MSG_STATUS", "SEND");
                } else {
                    chatMessageHelper.msgStatus = TableHelper.MsgStatus.PENDING;
                }
                result = true;

            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                ExceptionHandler.appendLog(e);
                chatMessageHelper.msgStatus = TableHelper.MsgStatus.PENDING;
                connectionStatus = "";
                reConnect();
                i("MSG_STATUS", "SMCK EXCE1" + e.getMessage());

            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.appendLog(e);
                chatMessageHelper.msgStatus = TableHelper.MsgStatus.PENDING;
                connectionStatus = "";
                i("MSG_STATUS", "SMCK EXE2" + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (!typingStatus) {
                try {
                    TableHelper tableHelper = new TableHelper();
                    if (isPending)
                        tableHelper.updateChat(getApplicationContext(), chatMessageHelper);
                    else
                        tableHelper.insertChat(getApplicationContext(), chatMessageHelper);
                } catch (Exception e) {
                    ExceptionHandler.appendLog(e);
                }
            }
        }
    }

    public void sendPendingMsg() {
        ArrayList<ChatMessageHelper> pendingMsgList = new TableHelper().getPendingMessages(getApplicationContext());
        for (int i = 0; i < pendingMsgList.size(); ++i) {
            SendMessage sendMessage = new SendMessage(pendingMsgList.get(i));
            sendMessage.setPending(true);
            sendMessage.execute();
        }
    }

    public static void unFriend(XMPPTCPConnection connection, String userName) {
        try {
            RosterPacket packet = new RosterPacket();
            packet.setType(set);
            RosterPacket.Item item = new RosterPacket.Item(userName, null);
            item.setItemType(RosterPacket.ItemType.remove);
            packet.addRosterItem(item);
            connection.sendStanza(packet);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
    }

    public XMPPTCPConnection getXMPPConnection() {
        return connection;
    }

    public void setXMPPConnection(XMPPTCPConnection connection) {
        this.connection = connection;
        setChatListener(connection);
        setLoginListener(connection);
        //setRosterListener(connection);
    }

    private void setRosterListener(XMPPTCPConnection connection) {
        if (connection != null) {
            Roster roster = Roster.getInstanceFor(connection);
            roster.addRosterListener(new RosterListener() {
                @Override
                public void entriesAdded(Collection<String> addresses) {

                    for (String jid : addresses) {
                        i("TAG", "entriesAdded: " + jid);
                        addToFriend(jid);
                    }
                }

                @Override
                public void entriesUpdated(Collection<String> addresses) {
                    for (String jid : addresses) {
                        i("TAG", "entriesUpdated: " + jid);
                    }

                }

                @Override
                public void entriesDeleted(Collection<String> addresses) {
                    for (String jid : addresses) {
                        i("TAG", "entriesDeleted: " + jid);
                    }
                }

                @Override
                public void presenceChanged(Presence presence) {

                }
            });
        }
    }

    void addToFriend(final String jid) {
        i("TAG", "addToFriend: ");
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    String user = jid.substring(0, jid.indexOf("@"));
                    ArrayList<FriendHelper> helper = new TableHelper().getUser(MyApplication.this, user);
                    FriendHelper friendHelper = null;
                    String name = "";
                    if (helper.size() > 0) {
                        friendHelper = helper.get(0);
                        name = friendHelper.first;
                    } else {
                        name = user;
                    }
                    Roster roster = Roster.getInstanceFor(getXMPPConnection());
                    Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
                    roster.createEntry(jid, name, null);
                    Presence pres = new Presence(Presence.Type.subscribe);
                    pres.setFrom(jid);

                    if (helper.size() > 0) {
                        friendHelper.isFriend = "true";
                        new TableHelper().addedInXMPP(MyApplication.this, friendHelper);
                    } else {
                        friendHelper = new FriendHelper();
                        friendHelper.first = user;
                        friendHelper.regMob = user;
                        friendHelper.isHIguys = "true";
                        friendHelper.isFriend = "true";
                        new TableHelper().insertUser(MyApplication.this, friendHelper);
                    }

                    getXMPPConnection().sendStanza(pres);
                } catch (SmackException.NotLoggedInException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

            }
        }.execute();
    }

    public String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);
        return df.format(new Date());
    }

    public void setChatListener(final XMPPConnection connection) {
        if (connection != null) {
            // Add a packet listener to get messages sent to us
            final ChatManager chatManager = ChatManager.getInstanceFor(connection);
            chatManager.addChatListener(new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {

                    chat.addMessageListener(new ChatMessageListener() {

                        @Override
                        public void processMessage(final Chat chat, final Message message) {
                            Log.d("ANY_CHECK", "==>" + message.toXML().toString());

                            String msg_xml = message.toXML().toString();

                            if (message.getBody() != null && message.getType() == Message.Type.chat) {
                                final String fromName = (message.getFrom());
                                new AsyncTask<Void, Void, Void>() {
                                    ChatMessageHelper chatMessageHelper;
                                    boolean isSkipeInsert = false;

                                    @Override
                                    protected Void doInBackground(Void... params) {

                                        Log.d("Msg Received", message.getBody());
                                        String user = fromName.substring(0, fromName.indexOf("@"));
                                        String msg = message.getBody();
                                        if (new TableHelper().isBlocked(getApplicationContext(), user)) {
                                            isSkipeInsert = true;
                                            return null;
                                        }
                                        Reader reader = new StringReader(message.getBody());
                                        Gson gson = new Gson();
                                        MessageJSON messageJSON = gson.fromJson(reader, MessageJSON.class);
                                        chatMessageHelper = new ChatMessageHelper();
                                        chatMessageHelper.from = user;
                                        chatMessageHelper.to = new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE);
                                        chatMessageHelper.msg = messageJSON.message;
                                        chatMessageHelper.messageType = messageJSON.type;
                                        chatMessageHelper.msgStatus = TableHelper.MsgStatus.RECEIVED;
                                        chatMessageHelper.group = "";
                                        chatMessageHelper.filePath = "";
                                        chatMessageHelper.read = false;
                                        chatMessageHelper.chatId = message.getStanzaId();
                                        chatMessageHelper.sent = false;
                                        chatMessageHelper.msg_time = getCurrentTime();
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        TableHelper tableHelper = new TableHelper();
                                        if (isSkipeInsert)
                                            return;
                                        if (chatMessageHelper != null) {
                                            if (!tableHelper.isAlreadyChatExist(getApplicationContext(), chatMessageHelper.chatId)) {
                                                tableHelper.insertChat(getApplicationContext(), chatMessageHelper);
                                            }
                                            String user = chatMessageHelper.from;
                                            if (tableHelper.isAlreadyInserted(getBaseContext(), user)) {
                                                if (messageListener != null) {
                                                    messageListener.msgReceived(user, chatMessageHelper);
                                                }
                                                if (!new StoreUserData(getApplicationContext()).getString(Constants.CURRENT_USER).equals(chatMessageHelper.from)) {
                                                    //  displayNotification();
                                                }
                                            } else {
                                                //TODO: Search from XMPP Code
//                                                final Call<ResponseBody> result;
//                                                result = Utils.callApi().allUserList(chatMessageHelper.from, "true");
//                                                result.enqueue(new Callback<ResponseBody>() {
//                                                    @Override
//                                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                                                        try {
//                                                            String result = null;
//                                                            result = response.body().string();
//                                                            final JSONObject jsonObject = new JSONObject(result);
//                                                            if (jsonObject.getString("result").equals("true")) {
//                                                                if (jsonObject.has("rows")) {
//                                                                    TableHelper tableHelper = new TableHelper();
//                                                                    JSONArray array = jsonObject.getJSONArray("rows");
//                                                                    for (int i = 0; i < array.length(); ++i) {
//                                                                        JSONObject object = array.getJSONObject(i);
//
//                                                                        FriendHelper friendHelper = new FriendHelper();
//                                                                        friendHelper.regMob = object.getString("device_id");
//                                                                        friendHelper.image = object.getString("photo");
//                                                                        friendHelper.last = object.getString("last_name");
//                                                                        friendHelper.first = object.getString("first_name");
//                                                                        if (!tableHelper.isAlreadyInserted(getApplicationContext(), friendHelper.regMob))
//                                                                            tableHelper.insertUser(getApplicationContext(), friendHelper);
//                                                                    }
//                                                                    Roster roster = Roster.getInstanceFor(connection);
//                                                                    Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
//                                                                    roster.createEntry(fromName, chatMessageHelper.from, null);
//
//                                                                    Presence pres = new Presence(Presence.Type.subscribe);
//                                                                    pres.setFrom(fromName);
//                                                                    connection.sendStanza(pres);
//                                                                    FriendHelper friendHelper = new FriendHelper();
//                                                                    friendHelper.regMob = chatMessageHelper.from;
//
//                                                                    if (messageListener != null) {
//                                                                        messageListener.msgReceived(chatMessageHelper.from, chatMessageHelper);
//                                                                    }
//                                                                    if (!new StoreUserData(getApplicationContext()).getString(Constants.CURRENT_USER).equals(chatMessageHelper.from)) {
//                                                                        displayNotification();
//                                                                    }
//                                                                }
//                                                            }
//                                                        } catch (JSONException | IOException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NotLoggedInException e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//
//                                                    @Override
//                                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
//
//                                                    }
//                                                });
                                            }
                                        }
                                    }
                                }.execute();
                            } else {
                                if (msg_xml.contains(ChatState.composing.toString())) {
                                    i("TAG", "processMessage: regMob started typing");
                                    messageListener.typingStatus(true);
                                } else if (msg_xml.contains(ChatState.paused.toString())) {
                                    i("TAG", "processMessage: regMob stopped typing");
                                    messageListener.typingStatus(false);
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    public interface MessageListener {
        public void msgReceived(String fromName, ChatMessageHelper body);

        public void typingStatus(boolean typingStatus);
    }

    public interface GroupMessageListener {
        public void msgReceived(String fromName, String body, String group);

        void msgReceived(ChatMessageHelper chatMessageHelper);
    }

    public void setGroupChatListener(final GroupHelper helper) {
        String jid = helper.groupId + "@conference." + Constants.IP;
        i("TAG", "setGroupChatListener: jid " + jid);
        if (connection == null) {
            i("TAG", "setGroupChatListener: returned from null");
            return;
        }
        final MultiUserChat multiUserChat = MultiUserChatManager.getInstanceFor(connection).getMultiUserChat(jid);

        if (multiUserChat.isJoined())
            try {
                multiUserChat.leave();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        if (!multiUserChat.isJoined()) {
            try {
                DiscussionHistory discussionHistory = new DiscussionHistory();
                ChatMessageHelper messageHelper = new TableHelper().getGroupLastMessage(getApplicationContext(), helper.groupId);

                if (messageHelper != null && messageHelper.msg_time != null) {
                    //2017-Feb-04 21:45:18
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MMM-dd' 'HH:mm:ss", Locale.US);
                    Date date = format.parse(messageHelper.msg_time);
                    calendar.setTime(date);
                    Date time = calendar.getTime();
                    // time.setTime(time.getTime() + 2);
                    discussionHistory.setSince(time);
                    //discussionHistory.setSince(new Date());
                    Log.i("TAG", "last message date: " + format.format(time));
                    Log.d("MUC", jid + " +" + time.getTime());
                    Log.i("TAG", "setGroupChatListener: " + new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE) + "@" + Constants.IP);
                    multiUserChat.createOrJoin(new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE) + "@" + Constants.IP, helper.password, discussionHistory, 1000);
                } else {
                    i("TAG", "setGroupChatListener: last message null");
                    multiUserChat.createOrJoin(new StoreUserData(getApplicationContext()).getString(Constants.USER_REG_MOBILE) + "@" + Constants.IP);
                }
                multiUserChat.changeAvailabilityStatus("join", Presence.Mode.available);
                /*new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {*/
                getGroupSubject(helper.groupId);
                i("TAG", "setGroupChatListener: subject " + multiUserChat.getSubject());
                String subject = multiUserChat.getSubject();
                if (subject != null) {
                    new TableHelper().updateGroup(getApplicationContext(), helper.groupId, subject, "");
                }
                    /*}
                }, 300);*/
                setGroupMessageListener(multiUserChat);

            } catch (SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                Log.d("Error", jid);
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("MUC Join", jid);
            setGroupMessageListener(multiUserChat);
        }
    }


    private void getGroupSubject(final String groupId) {

        Call<ResponseBody> result = Utils.callApiWithHeader().getGroupSubject(groupId);
        result.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                i("TAG", "onResponse: group create " + response.code());
                i("TAG", "onResponse: " + response.message());
                String res = null;
                try {
                    res = response.body().string();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
                if (response.code() == 200) {
                    try {
                        new TableHelper().updateGroup(getApplicationContext(), groupId, new JSONObject(res).getString("subject"), "");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }
}
