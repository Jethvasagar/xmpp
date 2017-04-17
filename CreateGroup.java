package com.ketan.openfire;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ketan.openfire.Adapter.GroupChatAdapter;
import com.ketan.openfire.Constants.Helper;
import com.ketan.openfire.Model.GroupChatUtils;
import com.ketan.openfire.Services.ConnectXmpp;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.json.JSONException;
import org.json.JSONObject;
import org.jxmpp.util.XmppStringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CreateGroup extends AppCompatActivity {
    String ktn;
    MultiUserChat muc;
    private EditText etGroupChat;
    public static ListView lvGroupChat;
    private ImageView sendMessage;
    private GroupChatUtils groupChatUtils;
    public static GroupChatAdapter groupChatAdapter;
    public static ArrayList<GroupChatUtils> arrayListGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupChat = (EditText)findViewById(R.id.etGroupChat);
        sendMessage = (ImageView)findViewById(R.id.sendMessage);
        lvGroupChat = (ListView) findViewById(R.id.lvGroupChat);

        arrayListGroup = new ArrayList<>();

        groupChatAdapter = new GroupChatAdapter(CreateGroup.this,arrayListGroup);
        //lvChat.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lvGroupChat.setAdapter(groupChatAdapter);

        /***
         * http://download.igniterealtime.org/smack/docs/latest/documentation/extensions/muc.html
         * https://community.igniterealtime.org/thread/54671
          */

     /* try{
            if(ConnectXmpp.connection!=null){
                Log.e("RES", String.valueOf(Roster.getInstanceFor(ConnectXmpp.connection).createGroup("ketantestgroup")));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }*/

        try{
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
            muc = manager.getMultiUserChat("myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal");
            List<String> joinedRooms = manager.getJoinedRooms(ConnectXmpp.connection.getUser());

            //Log.e("HOST",ConnectXmpp.connection.getHost());
            //Log.e("Service Name",ConnectXmpp.connection.getServiceName());
            //muc.join("myroom@conference.52.11.253.40");

            if(muc.isJoined()){

            }
            else {
                muc.create(XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));
                Form form = muc.getConfigurationForm();
                // Create a new form to submit based on the original form
                Form submitForm = form.createAnswerForm();

                // Sets the new owner of the room
                List<String> owners = new ArrayList<String>();
                owners.add(ConnectXmpp.connection.getUser());
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                //          submitForm.setAnswer("muc#roomconfig_persistentroom", false);
                submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                muc.sendConfigurationForm(submitForm);
                Log.e("joinedRooms", String.valueOf(joinedRooms.size()));
                Log.e("XMPP","People: "+ muc.getParticipants().toString());
                Roster roster = null;
                Collection<RosterEntry> entries = null;
                try {
                    roster = Roster.getInstanceFor(ConnectXmpp.connection);

                    if (!roster.isLoaded()) {
                        roster.reloadAndWait();
                    }
                    entries = roster.getEntries();
                    for (RosterEntry entry : entries) {
                        muc.invite(entry.getUser(), "Join My Group");
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }




//            muc.create(XmppStringUtils.parseBareJid("sagar@ip-172-31-26-54.us-west-2.compute.internal"));
            //Log.e("Nick Name",XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));

            // Get the the room's configuration form


            //muc.invite("sagar@ip-172-31-26-54.us-west-2.compute.internal", "Join My Group");



            //Get friend list and invite to join chatroom



            /*if(muc.isJoined()){
                Log.e("Open","Group Chat");

            }
            else {
                Log.e("Create","Room");
            }*/

            muc.addMessageListener(new MessageListener() {
                @Override
                public void processMessage(final Message message) {
                    Log.e("","--------------");
                    Log.e("Message",message.toString());
                    Log.e("Message Type",message.getType().toString());
                    Log.e("Message From",message.getFrom());
                    Log.e("Messgae Body",message.getBody());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            //XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser());

                            //Message Type: groupchat
                            //Message From: myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal/ketan@ip-172-31-26-54.us-west-2.compute.internal
                            //Message From: myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal/sagar
                            //Messgae Body: hello
                            //User: ketan@ip-172-31-26-54.us-west-2.compute.internal/Android
                            //E/Body: hello


                            String from = message.getFrom();
                            //Log.e("GroupName",from.substring(0,from.indexOf("@")));
                            Helper.roomIsOpen = from.substring(0,from.indexOf("@"));
                            if(from.contains("/")){
                                ktn = from.substring(from.indexOf("/")+1,from.length());
                                Log.e("ktn",ktn);
                                ktn = XmppStringUtils.parseBareJid(ktn.toString());
                                Log.e("ktn 2",ktn);
                            }

                            groupChatUtils = new GroupChatUtils(ktn,message.getBody().toString());

                            groupChatAdapter.add(groupChatUtils);

                            groupChatAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });

            /*MultiUserChatManager multiUserChatManager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
            MultiUserChat multiUserChat = multiUserChatManager.getMultiUserChat("myroom@conference.52.11.253.40");
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(Integer.MAX_VALUE);
            multiUserChat.join("ketan", null, history,
                    SmackConfiguration.getDefaultPacketReplyTimeout());*/
           /* if (mLastMessageDate == null)
                history.setMaxStanzas(300);
            else
                history.setSince(mLastMessageDate); //timestamp from your last message
*/

            //Log.e("Nick Name",XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));
            //Log.e("Nick Name",ConnectXmpp.connection.getUser());
            //getAllJoinedRoom();

            //createRoomRest();

           /* MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);

            Log.e("0", String.valueOf(manager.getJoinedRooms(ConnectXmpp.connection.getUser())));

            manager.getJoinedRooms(ConnectXmpp.connection.getUser());*/
/*
            MultiUserChatManager manager1 = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
            manager1.getJoinedRooms("sagar@ip-172-31-26-54.us-west-2.compute.internal");
            Log.e("1", String.valueOf(manager1.getJoinedRooms(ConnectXmpp.connection.getUser())));*/

        }
        catch (Exception ex){
            try {
                muc.leave();
                //muc.create(XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));
                muc.join(XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
            ex.printStackTrace();
        }

       //Roster.getInstanceFor(ConnectXmpp.connection).createGroup("ketantestgroup");

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String groupText = etGroupChat.getText().toString().trim();

                if(groupText.length()>0){

                    try {
                        etGroupChat.setText("");
                        Message message = new Message("myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal",Message.Type.groupchat);
                        message.setBody(groupText);
                        message.setType(Message.Type.groupchat);
                        message.setTo("myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal");
                        MultiUserChat muc = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection).getMultiUserChat("myroomketan@conference.ip-172-31-26-54.us-west-2.compute.internal");
                        muc.sendMessage(message);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            muc.leave();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void getAllGroup() {
        Roster roster = Roster.getInstanceFor(ConnectXmpp.connection);

        Collection<RosterGroup> entriesGroup = roster.getGroups();
        Iterator<RosterGroup> groupIterator = entriesGroup.iterator();

        while (groupIterator.hasNext()) {
            RosterGroup rosterGroup = groupIterator.next();

            Log.e("Group",rosterGroup.getName());

            List<RosterEntry> rosterEntries = rosterGroup.getEntries();
            for (RosterEntry rosterEntry : rosterEntries) {
                //Log.e("User",rosterEntry.getUser());
            }
        }
    }

    private void getAllJoinedRoom() {
        try{
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
            final MultiUserChat muc = manager.getMultiUserChat("myroom"+"@conference.52.11.253.40");

            manager.getJoinedRooms(ConnectXmpp.connection.getUser());

            // Create the room
            ConnectXmpp.connection.setPacketReplyTimeout(2000);
            /*String name = ConnectXmpp.connection.getUser();
            Log.e(" : Name : ",""+name);
            String name1 = name.substring(0, name.lastIndexOf("@"));
            Log.e(" : Name 1 : ",""+name1);*/
            muc.create(XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));


            Log.e("Nick Name",XmppStringUtils.parseBareJid(ConnectXmpp.connection.getUser()));

            // Get the the room's configuration form
            Form form = muc.getConfigurationForm();
            // Create a new form to submit based on the original form
            Form submitForm = form.createAnswerForm();

            Form f = new Form(DataForm.Type.submit);
            try {
                muc.sendConfigurationForm(f);

            } catch (XMPPException xe) {
                Log.e("Error", "on Sending Configuration Form"+ String.valueOf(xe));
            }

            // Sets the new owner of the room
            List<String> owners = new ArrayList<String>();
            owners.add(ConnectXmpp.connection.getUser());
            submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
            muc.sendConfigurationForm(submitForm);

            //muc.invite("sagar@ip-172-31-26-54.us-west-2.compute.internal", "Join My Group");

            Log.e("XMPP","People: "+ muc.getParticipants().toString());
            //^^returns an empty list

            muc.sendMessage("This is a test");


            muc.addInvitationRejectionListener(new InvitationRejectionListener() {
                @Override
                public void invitationDeclined(String invitee, String reason) {
                    Log.e("invitationDeclined",invitee+"\n"+reason);
                }
            });

            muc.addParticipantListener(new PresenceListener() {
                @Override
                public void processPresence(Presence presence) {
                    Log.e("addParticipantListener", String.valueOf(muc.getOccupantsCount()));
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void checkRoomAndInvite() {
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
        MultiUserChat muc = manager.getMultiUserChat("k3tan@conference.ip-172-31-26-54.us-west-2.compute.internal");
        try {
            muc.create("k3tan");
            muc.sendConfigurationForm(new Form(DataForm.Type.submit));
            muc.join("k3tan");
            //muc.getParticipants();
            muc.invite("sagar@ip-172-31-26-54.us-west-2.compute.internal", "Join My Group");

            Log.e("XMPP","People: "+ muc.getParticipants().toString());
            //^^returns an empty list

            muc.sendMessage("This is a test");

        } catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }

    private void createRoomRest() {

        JSONObject jsonBody = new JSONObject();
        try{
            jsonBody.put("roomName", "k3tan");
            jsonBody.put("naturalName", "k3tan");
            jsonBody.put("description","k3tan");
            jsonBody.put("persistent","true");
            jsonBody.put("canAnyoneDiscoverJID","true");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Log.e("jsonBody", String.valueOf(jsonBody));
        final String mRequestBody = jsonBody.toString();
        Log.e("requestBody",mRequestBody);

        StringRequest stringRequest = new StringRequest(Request.Method.POST,"http://52.11.253.40:9090/plugins/restapi/v1/chatrooms" , new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {
                if(response.contains("201")){
                    Log.e("Openfire :- ","ChatRoom Created Successfully");
                    addUserToRoom();
                }

            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                        if(volleyError instanceof NoConnectionError)
                        {
                            Toast.makeText(CreateGroup.this,"No Internet Connection",Toast.LENGTH_LONG).show();
                        }
                        else if (volleyError.networkResponse == null)
                        {
                            if (volleyError.getClass().equals(TimeoutError.class))
                            {
                                Toast.makeText(CreateGroup.this,"Oops. Connection Timeout!",Toast.LENGTH_LONG).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(CreateGroup.this,"Something Went Wrong Please Try Again", Toast.LENGTH_LONG).show();
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();
                String creds = "admin"+":"+"admin";
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                Log.e("Auth",auth);
                //String auth = "0no9X49fn51irT8a";
                params.put("Authorization",auth);
                params.put("Content-Type", "application/json");
                params.put("Accept","application/json");
                return params;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("servicename", "conference");

                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(60000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue requestQueue = Volley.newRequestQueue(CreateGroup.this);
        requestQueue.add(stringRequest);
    }

    private void addUserToRoom() {

        /*JSONObject jsonBody = new JSONObject();
        try{
            jsonBody.put("roomName", "k3tan");
            jsonBody.put("naturalName", "k3tan");
            jsonBody.put("description","k3tan");
            jsonBody.put("persistent","true");
            jsonBody.put("canAnyoneDiscoverJID","true");
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Log.e("jsonBody", String.valueOf(jsonBody));
        final String mRequestBody = jsonBody.toString();
        Log.e("requestBody",mRequestBody);*/

        StringRequest stringRequest = new StringRequest(Request.Method.POST,"http://52.11.253.40:9090/plugins/restapi/v1/chatrooms" , new Response.Listener<String>() {
            @Override
            public void onResponse(String response)
            {
                if(response.contains("201")){
                    Log.e("Openfire :- ","User Added in Room Successfully");
                }

            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                        if(volleyError instanceof NoConnectionError)
                        {
                            Toast.makeText(CreateGroup.this,"No Internet Connection",Toast.LENGTH_LONG).show();
                        }
                        else if (volleyError.networkResponse == null)
                        {
                            if (volleyError.getClass().equals(TimeoutError.class))
                            {
                                Toast.makeText(CreateGroup.this,"Oops. Connection Timeout!",Toast.LENGTH_LONG).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(CreateGroup.this,"Something Went Wrong Please Try Again", Toast.LENGTH_LONG).show();
                        }
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<String, String>();
                String creds = "admin"+":"+"admin";
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
                params.put("Authorization",auth);
                params.put("Content-Type", "application/xml");
                //params.put("Accept","application/xml");
                return params;
            }

            /*@Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }*/

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("roomname", "k3tan");
                params.put("name","sagar@ip-172-31-26-54.us-west-2.compute.internal");
                params.put("roles","members");
                params.put("servicename","conference");
                params.put("servicename","conference");

                return params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(60000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue requestQueue = Volley.newRequestQueue(CreateGroup.this);
        requestQueue.add(stringRequest);

    }

    public void createRoom(){

        try{
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(ConnectXmpp.connection);
            MultiUserChat muc = manager.getMultiUserChat("myroom@conference.52.11.253.40");

            // Create the room
            ConnectXmpp.connection.setPacketReplyTimeout(2000);
            String name = ConnectXmpp.connection.getUser();
            Log.e(" : Name : ",""+name);
            String name1 = name.substring(0, name.lastIndexOf("@"));
            Log.e(" : Name 1 : ",""+name1);
            muc.create(name1);

            // Get the the room's configuration form
            Form form = muc.getConfigurationForm();
            // Create a new form to submit based on the original form
            Form submitForm = form.createAnswerForm();

            Form f = new Form(DataForm.Type.submit);
            try {
                muc.sendConfigurationForm(f);

            } catch (XMPPException xe) {
                Log.e("Error", "on Sending Configuration Form"+ String.valueOf(xe));
            }

            // Sets the new owner of the room
            List<String> owners = new ArrayList<String>();
            owners.add(ConnectXmpp.connection.getUser());
            submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
            muc.sendConfigurationForm(submitForm);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }



}
