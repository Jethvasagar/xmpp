package com.sid.xmppconnect.xmpp;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sid.xmppconnect.R;
import com.sid.xmppconnect.model.User;
import com.sid.xmppconnect.util.Config;
import com.sid.xmppconnect.service.XMPPService;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by Jethva Sagar (Piml Sid) on 3/1/17
 * Email : jethvasagar2@gmail.com.
 */

public class MyXMPP {

    String LOG = "Sid";

    public static String username;
    public static String password;
    private String serverAddress;

    public static boolean connected = false;
    public static boolean isConnecting = false;
    public boolean isLoggedIn = false;

    public static MyXMPP instance = null;
    public static XMPPTCPConnection xmpptcpConnection;
    XMPPService contex;
    public static boolean instanceCreated = false;


    private MyXMPP(XMPPService xmppService, String serverName, String userName, String password){
        this.serverAddress = serverName;
        this.username = userName;
        this.password = password;
        this.contex = xmppService;

        init();
    }

    public static MyXMPP getInstance(XMPPService context, String server, String username, String password){

        if(instance == null){
                instance = new MyXMPP(context,server,username,password);
                instanceCreated = true;
        }
        return instance;
    }

    public void init(){
        initialiseConnection();
    }
    private void initialiseConnection(){

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

        DomainBareJid serviceName;
        try {
           // serviceName = JidCreate.domainBareFrom(Config.DOMAIN_NAME);
           // config.setServiceName(serviceName);

            InetAddress inetAddress = InetAddress.getLocalHost();
            InetAddress addr = InetAddress.getByName("192.168.209.2");
            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return false;
                }
            };
            serviceName = JidCreate.domainBareFrom("localhost");
            config.setHostnameVerifier(verifier);
            config.setHostAddress(inetAddress);

        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            config.setXmppDomain("kali.kali");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
       // config.setHost(Config.DOMAIN_NAME); //192.168.43.100
        config.setPort(Config.PORT);
        config.setUsernameAndPassword(Config.USERNAME,Config.PASSWORD);
        config.setDebuggerEnabled(true);
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);


        XMPPConnectionListener xmppConnectionListener = new XMPPConnectionListener();
        xmpptcpConnection = new XMPPTCPConnection(config.build());
        xmpptcpConnection.addConnectionListener(xmppConnectionListener);

        PingManager.setDefaultPingInterval(600);
        PingManager.getInstanceFor(xmpptcpConnection).registerPingFailedListener(new PingFailedListener() {
            @Override
            public void pingFailed() {
                Log.d(LOG,"pinkFailed()...");
            }
        });
    }

    public void disconnect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG,"XMPP disconnect success....");
                xmpptcpConnection.disconnect();
            }
        }).start();
    }

    public void connect(){

        AsyncTask<Void,Void,Boolean> connectionThread = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                if(xmpptcpConnection.isConnected())
                    return false;
                isConnecting = true;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG,"Connecting to xmpp server...");
                    }
                });

                try {
                    xmpptcpConnection.connect();
                    connected = true;

                    DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(xmpptcpConnection);
                    dm.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
                    dm.addReceiptReceivedListener(new ReceiptReceivedListener() {
                        @Override
                        public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
                            Log.d(LOG,"XMPP onReceiptReceived()... fromJid - " + fromJid  + " toJid - " + receiptId + " receipt - " + receiptId);

                        }

                    });

                    Log.d(LOG,"XMPP connetion succesfully....");
                } catch (SmackException | IOException | XMPPException | InterruptedException e) {
                    e.printStackTrace();
                }
                return isConnecting = false;
            }


        }.execute();
    }

    public List<User> rosterList(){

      /* if(!roster.isLoaded()){
            try {
                roster.reloadAndWait();
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        // public User(String username, String userid, int image, String status, String type, boolean isFriend) {

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final List<User> userList = new ArrayList<>();
            Roster roster = Roster.getInstanceFor(xmpptcpConnection);
                Collection<RosterEntry> entries = roster.getEntries();
                Log.d("Roster","buddy size :: " + entries.size());
                for(RosterEntry rosterEntry : entries){
                    Presence availibility = roster.getPresence(rosterEntry.getJid());
                    Presence.Mode mode = availibility.getMode();
                    userList.add(new User(rosterEntry.getName(),"", R.drawable.user_icon,""+retrieveState_mode(mode,availibility.isAvailable()),
                            ""+rosterEntry.getType(),true));
                }
        return userList;
    }

    /*---Get user mode---*/
    public static String retrieveState_mode(Presence.Mode userMode, boolean isOnline) {
        String userState = "offline";
        /** 0 for offline, 1 for online, 2 for away,3 for busy*/
        if(userMode == Presence.Mode.dnd) {
            userState = "busy";
        } else if (userMode == Presence.Mode.away || userMode == Presence.Mode.xa) {
            userState = "away";
        } else if (isOnline) {
            userState = "online";
        }
        return userState;
    }

    /*---add friend---*/

    public boolean addFriend(String fJID){

        /*--user sub1---*/
        Presence request = new Presence(Presence.Type.subscribe);
        request.setTo(fJID+"@kali.kali");
        try {
            xmpptcpConnection.sendStanza(request);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Presence accept = new Presence(Presence.Type.subscribed);
        accept.setTo("jay@kali.kali");
        try {
            xmpptcpConnection.sendStanza(accept);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }


    public boolean registerRoster(final String name, final String email){

        boolean iscrate = false;
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                boolean iscrate = false;
                AccountManager accountManager = AccountManager.getInstance(xmpptcpConnection);
                accountManager.sensitiveOperationOverInsecureConnection(true);
                HashMap<String, String> attributes = new HashMap<>();
                attributes.put("username", "done");
                attributes.put("password", "done");
                attributes.put("name", "done1 done2");
                attributes.put("email", "done@done.com");
                attributes.put("phone", "851146710");
                attributes.put("last", "doneLast");
                attributes.put("first", "doneFirst");

                try {
                    if(accountManager.supportsAccountCreation()){
                        accountManager.createAccount(Localpart.from("done"),"f",attributes);
                        iscrate = true;
                    }
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
                    e.printStackTrace();
                    Log.d("piml","Error is ::  "+e.getMessage());
                    iscrate = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
                return iscrate;
            }
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
            }
        }.execute((Void[]) null);


        return iscrate;
    }


    /*---send message2---*/

  /*  public void sendMessage(String jid, String body){

        try{
            Chat chat = ChatManager.getInstanceFor(xmpptcpConnection)
                    .createChat(jid);
            chat.sendMessage(body);

            ChatManager chatManager = ChatManager.getInstanceFor(xmpptcpConnection);
            chatManager.addChatListener(new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {
                    chat.addMessageListener(new ChatMessageListener() {
                        @Override
                        public void processMessage(Chat chat, Message message) {

                            if(message.getBody()!=null && message.getType() == Message.Type.chat){

                                Log.d("Sid","Receive message :: " + message.toXML().toString());
                                Log.d("Sid","from : " + message.getFrom().substring(0, message.getFrom().indexOf("@")));
                                Log.d("Sid","Body : " + message.getBody());
                                Log.d("Sid","subject : " + message.getSubject());
                                Log.d("Sid","Type : " + message.getType());

                            }
                        }
                    });
                }
            });

        }catch (Exception ex){ ex.printStackTrace(); }

        DiscussionHistory discussionHistroy = new DiscussionHistory();
        discussionHistroy.setMaxStanzas(0);

        
    }*/

    /*--create group chat--*/
    public void createGroupChat(){

        MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(xmpptcpConnection);
        try {

            EntityBareJid jid = JidCreate.entityBareFrom("myroom@conference.kali.kali");
            Resourcepart nickname = Resourcepart.from("testboot");

            MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat(jid);

            Set<Jid> owners = JidUtil.jidSetFrom(new String[]{"raju@kali.kali","sonali@kali.kali"});
            multiUserChat.create(nickname)
                .getConfigFormManager().setRoomOwners(owners).submitConfigurationForm();


        } catch (XmppStringprepException | MultiUserChatException.MucAlreadyJoinedException | SmackException.NotConnectedException | MultiUserChatException.MucConfigurationNotSupportedException | MultiUserChatException.MissingMucCreationAcknowledgeException | XMPPException.XMPPErrorException | MultiUserChatException.NotAMucServiceException | InterruptedException e) {
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        }

    }


    public class XMPPConnectionListener implements ConnectionListener{

        @Override
        public void connected(XMPPConnection connection) {
            Log.d(LOG,"XMPP connected..." + connection.getUser() + " " + connection.getServiceName());

            connected = true;
            if(!connection.isAuthenticated()){
                login();
            }
        }

        @Override
        public void authenticated(XMPPConnection connection,boolean resumed) {
            isLoggedIn = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        Thread.sleep(500);
                        Log.d(LOG,"XMPP authenticated...");
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void connectionClosed() {
            Log.d(LOG,"XMPP connectionClosed...");
            connected = false;
            isLoggedIn = false;
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            Log.d(LOG,"XMPP connectionClosedOnError..." + e.getMessage());

            connected = false;
            isLoggedIn = false;
        }

        @Override
        public void reconnectionSuccessful() {
            Log.d(LOG,"XMPP reconnectionSuccessful...");

            connected = true;
            isLoggedIn = true;
        }

        @Override
        public void reconnectingIn(int seconds) {
            Log.d(LOG,"XMPP reconnectingIn..." + seconds);

            isLoggedIn = false;
        }

        @Override
        public void reconnectionFailed(Exception e) {
            Log.d(LOG,"XMPP reconnectionFailed..." + e.getMessage());

            connected = false;
            isLoggedIn = false;
        }
    }

    public void login(){
        try {
            xmpptcpConnection.login(username,password);
            Log.d(LOG,"You are now connected to XMPP server...");
        } catch (XMPPException | IOException | SmackException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
