/**
 * ConversationActivity.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.util.encoders.Base64;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

public class ConversationActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private SmsManager smsManager = SmsManager.getDefault();
    private KeyStoreManager keyStoreManager = null;

    private static final String TAG = "Smessage: Conversation Activity";
    private static final int LOADER_ID = 0;

    private String contactName = "";
    private String contactPhoneNumber = "";

    private boolean conversationSecure = false;
    private String keyStorePassword = "";
    private byte[] secretKey = null;

    // ArrayAdapter stores messages from the SMS database and the ListView displays them
    private ListView messageList = null;
    private ArrayAdapter<Message> messages = null;
    private LoaderManager.LoaderCallbacks<Cursor> callbacks = this;

    private static final boolean LOGGING_ENABLED = true;
           
    /**
     * 
     * @see android.app.Activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {  

        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_conversation);

        getConversationInformation();

        initialiseKeyStore();
        initialiseActionBar();

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        
    }

    /**
     * Get the contact's name and phone number plus the user's key store password from MainActivity.
     */
    private void getConversationInformation() {

        // Get the message from the intent that created this activity
        Intent intent = getIntent();
        Bundle conversationInformation = intent.getBundleExtra(MainActivity.CONVERSATION_INFORMATION);

        contactName = conversationInformation.getString("CONTACT_NAME");
        contactPhoneNumber = conversationInformation.getString("CONTACT_PHONE_NUMBER");
        keyStorePassword = conversationInformation.getString("KEY_STORE_PASSWORD");

    }

    /**
     * Load the key store manager and find out if the conversation has already been secured.
     */
    private void initialiseKeyStore() {

        try {

            keyStoreManager = new KeyStoreManager(getBaseContext(), keyStorePassword);

            if (keyStoreManager.keyExists(contactPhoneNumber)) {
                conversationSecure = true;
                secretKey = keyStoreManager.getSecretKey(contactPhoneNumber);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading the KeyStoreManager", e);
        }

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
    }    
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conversation_activity_actions, menu);
        return true;
        
    }    
    
    /**
     * 
     * @see android.app.Activity
     */    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            
            case R.id.action_search:
                //openSearch();
                return true;
            
            case R.id.action_settings:
                //openSettings();
                return true;
                
            case R.id.action_secure_conversation:
                // TODO: Use value of conversationSecure to change what the button does
                startSecureConversation();
                return true;
            
            default:
                return super.onOptionsItemSelected(item);
        
        }
        
    }
    
    public void startSecureConversation() {

        // TODO: Some sort of box asking if the user is sure and that they'll be charged for the text or whatever
        AsymmetricKeyParameter publicKey = null;

        try {
            publicKey = keyStoreManager.getPublicKey(KeyStoreGenerator.OWN_PUBLIC_KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Error getting public key from key store", e);
        }

        // Turn the public key into a Base64 String ready to be sent
        String publicKeyMessage = new String(Base64.encode(CryptoUtils.convertToPublicKey((RSAKeyParameters) publicKey).getEncoded()));

        EditText messageBox = (EditText) findViewById(R.id.edit_message);

        // TODO: Use real PEM here!
        messageBox.setText("----BEGIN PUBLIC KEY----" + publicKeyMessage);

        // Send the key to the recipient
        // ArrayList<String> splitMessage = smsManager.divideMessage(publicKeyMessage);
        //smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);

    }
    
    /**
     * Set up the ActionBar for this activity.
     */
    public void initialiseActionBar() {        
        
        // Show the Up button in the action bar.
        setupActionBar();
        
        ActionBar actionBar = getSupportActionBar();
        
        if (contactName.equals("null")) {
            actionBar.setTitle(contactPhoneNumber);  
        } else {
            actionBar.setTitle(contactName);
            actionBar.setSubtitle(contactPhoneNumber);
        }        
        
    }
    
    /**
     * Send an SMS message.
     * 
     * @param view
     * @throws IllegalArgumentException If there's an error sending the message, throw an exception.
     */
    public void sendMessage(View view) throws Exception {
        
        // Get text message from the text box
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();      
        
        // If we have a message to send, split it and send it
        // TODO: Error checking!
        if (message != null) {          

            if (conversationSecure) {
                final boolean ENCRYPT = true;
                message = CryptoCore.aes(message, secretKey, ENCRYPT);
            }

            Log.i(TAG, "Sending text message");           
            
            ArrayList<String> splitMessage = smsManager.divideMessage(message);            
            smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);            
            
            editText.setText("");            
        
        } else {            
            // If there's no message to send, do nothing
        }   

        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        
    }
  
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        
        Uri smsUri = Uri.parse("content://sms/");
        String numberToFind = PhoneNumberHelperMethods.stripSeparatorsAndAreaCode(contactPhoneNumber);
        
        Log.i(TAG, "Address substring: " + numberToFind);
        
        /* 
         * SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumnsSmsCursor = {"_id", "thread_id", "address", "body", "date", "type"};

        String address = "";

        // Set up WHERE clause; find texts from address containing the number without an area code. If the number is
        // actually a word (like Google), don't strip any separators from the address stored in the table
        if (numberToFind.matches("\\d")) {
            address = "REPLACE(REPLACE(address, ' ', ''), '-', '') LIKE " + DatabaseUtils.sqlEscapeString("%" + numberToFind);
        } else {    
            address = "REPLACE(address, ' ', '') LIKE " + DatabaseUtils.sqlEscapeString("%" + numberToFind);
        }
        
        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id ASC, date ASC";
        
        return new CursorLoader(this, smsUri, returnedColumnsSmsCursor, address, null, sortOrder);        
        
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor smsCursor) {

        messageList = (ListView) findViewById(R.id.message_list);

        messages = new ArrayAdapter<Message>(this,
                R.layout.message_list_row,
                R.id.message,
                getMessages(smsCursor));

        // Get the cursor loader used for getting messages
        messageList.setAdapter(messages);
        messageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {

                final boolean ENCRYPT = true;

                // Get the details of the message clicked
                Message message = (Message) parent.getAdapter().getItem(position);
                String messageContents = message.getBody();

                Log.i("Message clicked: ", messageContents);

                // If no shared secret exists yet, treat the message as a key
                if (!conversationSecure && !keyStoreManager.keyExists(contactPhoneNumber)) {

                    // TODO: Make the headers constants and put them somewhere or use a real PEM object
                    if (messageContents.startsWith("----BEGIN PUBLIC KEY----")) {

                        try {

                            // 24 is the length of the ----BEGIN PUBLIC KEY---- header
                            messageContents = messageContents.substring(24);

                            Log.d(TAG, "Public key is " + messageContents);

                            // Turn the key message into an AsymmetricKeyParameter
                            AsymmetricKeyParameter publicKey = CryptoUtils.convertToAsymmetricKeyParameter(
                                    (KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(messageContents.getBytes())))));

                            // Create a new shared secret key and store it using the contact's phone number
                            secretKey = CryptoCore.generateAESKey();
                            keyStoreManager.addSecretKey(contactPhoneNumber, secretKey);

                            // Encrypt the key using the recipient's public key
                            String keyMessage = CryptoCore.rsa(new String(Base64.encode(secretKey)), publicKey, ENCRYPT);

                            // TODO: Use real PEM here!
                            EditText messageBox = (EditText) findViewById(R.id.edit_message);
                            messageBox.setText("----BEGIN SHARED KEY----" + keyMessage);

                            // Send it off
                            // ArrayList<String> splitMessage = smsManager.divideMessage(keyMessage);
                            //smsManager.sendMultipartTextMessage(contactPhoneNumber, null, splitMessage, null, null);

                            conversationSecure = true;

                        } catch (Exception e) {
                            Log.e(TAG, "The message selected wasn't a public key!", e);
                        }

                    } else if (messageContents.startsWith("----BEGIN SHARED KEY----")) {

                        try {

                            // 24 is the length of the ----BEGIN SHARED KEY---- header
                            messageContents = messageContents.substring(24);

                            Log.d(TAG, "Shared key is " + messageContents);

                            // Turn the key back into a byte array
                            secretKey = Base64.decode((CryptoCore.rsa(messageContents,
                                    keyStoreManager.getPrivateKey(KeyStoreGenerator.OWN_PRIVATE_KEY_ALIAS),
                                    !ENCRYPT))
                                    .getBytes());

                            keyStoreManager.addSecretKey(contactPhoneNumber, secretKey);

                            conversationSecure = true;

                        } catch (Exception e) {
                            Log.d(TAG, "The message selected wasn't a shared key!", e);
                        }

                    } else {
                        // Do nothing
                    }

                // If the conversation is already secure, try and decrypt the message
                } else if (conversationSecure) {

                    try {

                        messageContents = CryptoCore.aes(messageContents, secretKey, !ENCRYPT);
                        message.setBody(messageContents);

                        messages.notifyDataSetChanged();

                        Log.d(TAG, messageContents);

                    } catch (Exception e) {
                        Log.e(TAG, "Error decrypting message", e);
                    }

                } else {
                    // Do nothing
                }

            }

        });

        if (LOGGING_ENABLED) {

            Log.i(TAG, "Cursor finished loading");
            int messageCounter = 0;

            // Use cursor to iterate over the database and get each row
            while (smsCursor.moveToNext()) {

                Log.i("Message " + messageCounter, "-----------------");

                for (int i = 0; i < smsCursor.getColumnCount(); i++) {
                    Log.d(smsCursor.getColumnName(i) + "", smsCursor.getString(i) + "");
                }

                messageCounter++;

            }

        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Moving list to bottom");
                messageList.setSelection(messages.getCount());
            }

        });
        
    }    
    
    /*
     * Invoked when the CursorLoader is reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        
        /*
         * Clears out the adapter's reference to the Cursor.
         * This prevents memory leaks.
         */
        messages.clear();
        
    }

    /**
     * Uses the cursor provided to get the list of messages for the contact. Decryption happens here if required so that
     * the underlying SMS store is not affected.
     *
     * @param smsCursor A cursor returned by the cursor loader connected to the SMS database table.
     * @return          The list of messages associated with the contact.
     */
    private ArrayList<Message> getMessages(Cursor smsCursor) {

        ArrayList<Message> messages = new ArrayList<Message>();

        final int ID_COLUMN_INDEX = 0;
        final int THREAD_ID_COLUMN_INDEX = 1;
        final int ADDRESS_COLUMN_INDEX = 2;
        final int MESSAGE_BODY_COLUMN_INDEX = 3;
        final int DATE_COLUMN_INDEX = 4;
        final int TYPE_COLUMN_INDEX = 5;

        // While there's a message to get
        while (smsCursor.moveToNext()) {

            // Add it to the Message list
            messages.add(new Message(smsCursor.getInt(THREAD_ID_COLUMN_INDEX),
                    smsCursor.getString(MESSAGE_BODY_COLUMN_INDEX), smsCursor.getString(ADDRESS_COLUMN_INDEX),
                    smsCursor.getLong(DATE_COLUMN_INDEX)));

        }

        return messages;

    }
   
}
