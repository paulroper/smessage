
package com.csulcv.Smessage;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

/**
 * MainActivity.java
 * 
 * @author Paul Roper
 *
 */
public class MainActivity extends ActionBarActivity {

    public final static String TEST_NUMBER = "com.csulcv.smessage.testNumber";
    private final static String TAG = "Smessage: Main Activity"; 
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get ListView used for contacts and get contacts
        ListView contactList = (ListView) findViewById(R.id.contact_list);
        
        // Setup adapter for message list using array list of messages
        ArrayAdapter<String> contactListAdapter = new ArrayAdapter<String>(this, R.layout.contact_view_row, 
                R.id.contact_name, getContactNames(getContactNamesFromConversations(getNewestConversationMessages())));
        
        contactList.setAdapter(contactListAdapter);       
        
    }
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true; 
        
    }
    
    /**
     * 
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
            default:
                return super.onOptionsItemSelected(item);
        }
        
    }

    /**
     * Create a new message activity when the user clicks create new.
     * 
     * @param  view 
     * @throws Exception Throw an illegal state exception if the activity cannot be created.
     */
    public void createNewMessage(View view) throws Exception {
        
        // Create intent used to move to SendMessage activity
        Intent intent = new Intent(this, SendMessageActivity.class);
        
        // Get test phone number from text box
        EditText editText = (EditText) findViewById(R.id.test_address);        
        String testNumber = editText.getText().toString();  
        
        intent.putExtra(TEST_NUMBER, testNumber);
        
        Log.i(TAG, "Starting SendMessage activity");          
        
        try {
            startActivity(intent);            
        } catch (IllegalStateException e) {
            throw new Exception("Error creating new activity");
        }
        
    }

    /**
     * Get the newest message from a conversation.
     *  
     * Retrieve a list of the newest messages from each conversation thread. These (along with the address of the 
     * contact the message is for) are used to build a contacts list.
     * 
     * @return An ArrayList containing Message objects.
     */
    public ArrayList<Message> getNewestConversationMessages() {     
        
        ArrayList<Message> newestConversationMessages = new ArrayList<Message>();        
        Uri smsUri = Uri.parse("content://sms");    

        /* SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         */
        String[] returnedColumns = {"thread_id", "address", "body", "date"};

        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id DESC, date ASC";
        
        // Send the query to get SMS messages. Default sort order is date DESC.
        // TODO: Use a CursorLoader, it runs the query in the background.
        Cursor smsCursor = getContentResolver().query(smsUri, returnedColumns, null, null, sortOrder);

        // Indices are fixed constants based on position in the returnedColumns array
        final int THREAD_ID_COLUMN_INDEX = 0;
        final int ADDRESS_COLUMN_INDEX = 1;
        final int MESSAGE_BODY_COLUMN_INDEX = 2;
        final int DATE_COLUMN_INDEX = 3;
        
        int messageCounter = 1; 
        String currentThreadId = "";
        
        /* 
         * Use a cursor to iterate over the database. Offsets are used to (hopefully) speed up access.
         * Offsets'll have to be changed based on the number of columns we're querying.
         */
        while (smsCursor.moveToNext()) {  
            currentThreadId = smsCursor.getString(THREAD_ID_COLUMN_INDEX);                    

            /* 
             * We don't want to move the cursor past the last index so check for when it's on the last row and break
             * so it doesn't move past it.
             */
            if (smsCursor.isLast()) {
                newestConversationMessages.add(new Message(smsCursor.getInt(THREAD_ID_COLUMN_INDEX), 
                        smsCursor.getString(MESSAGE_BODY_COLUMN_INDEX), smsCursor.getString(ADDRESS_COLUMN_INDEX),
                        smsCursor.getLong(DATE_COLUMN_INDEX))); 
                
                for (int i = 0; i < smsCursor.getColumnCount(); i++) {                    
                    Log.d("Message " + (messageCounter + 1), "-----------------");
                    Log.d(smsCursor.getColumnName(i), smsCursor.getString(i));
                    Log.d("Adding message", "Adding message to ArrayList");
                }
                
                messageCounter++;                
                
                break;
                
            } else {
                smsCursor.moveToNext();
            }
            
            /* 
             * If the next thread ID in the message list is different then we've reached the end of the current thread. 
             * As the data is arranged by date, we add this newest message to the list and move on. 
             */
            if (!(smsCursor.getString(THREAD_ID_COLUMN_INDEX).equals(currentThreadId))) {               

                smsCursor.moveToPrevious();
               
                for (int i = 0; i < smsCursor.getColumnCount(); i++) {                    
                    Log.d("Message " + (messageCounter + 1), "-----------------");
                    Log.d(smsCursor.getColumnName(i), smsCursor.getString(i));
                    Log.d("Adding message", "Adding message to ArrayList");
                }
                
                newestConversationMessages.add(new Message(smsCursor.getInt(THREAD_ID_COLUMN_INDEX), 
                        smsCursor.getString(MESSAGE_BODY_COLUMN_INDEX), smsCursor.getString(ADDRESS_COLUMN_INDEX),                         
                        smsCursor.getLong(DATE_COLUMN_INDEX)));
                    
                messageCounter++;                
                    
            } else {                    
                smsCursor.moveToPrevious();
            }    
    
        }
     
        // Sort messages in ascending order
        Collections.sort(newestConversationMessages);
        
        return newestConversationMessages;
        
    }
    
    /**
     * Get the newest message from a conversation.
     *  
     * Retrieve a list of the newest messages from each conversation thread. These (along with the address of the 
     * contact the message is for) are used to build a contacts list.
     * 
     * @return An ArrayList containing Message objects.
     */
    public ArrayList<Message> getConversations() {     
        
        ArrayList<Message> newestConversationMessages = new ArrayList<Message>();        
        Uri smsUri = Uri.parse("content://sms/conversations");    

        /* 
         * SMS conversation provider columns: thread_id, msg_count, snippet
         */
        String[] returnedColumns = {"thread_id", "msg_count", "snippet"};
        
        // Send the query to get SMS messages. Default sort order is date DESC.
        // TODO: Use a CursorLoader, it runs the query in the background.
        Cursor smsCursor = getContentResolver().query(smsUri, returnedColumns, null, null, null);

        // Indices are fixed constants based on position in the returnedColumns array        
        @SuppressWarnings("unused")
        int msgCountColumnIndex = 1;
        
        int threadIdColumnIndex = 0;
        int snippetColumnIndex = 2;
        
        /* 
         * Use a cursor to iterate over the SMS table. Offsets are used to (hopefully) speed up access.
         * Offsets'll have to be changed based on the number of columns we're querying.
         */
        while (smsCursor.moveToNext()) {  
           
            for (int i = 0; i < smsCursor.getColumnCount(); i++) {
                Log.d(smsCursor.getColumnName(i) + "", smsCursor.getString(i) + "");
                
                if (smsCursor.getColumnName(i).equals("body") && smsCursor.getString(i) != null) {
                    Log.d("Adding message", "Adding message to ArrayList");
                    newestConversationMessages.add(new Message(smsCursor.getInt(threadIdColumnIndex), 
                            "null", smsCursor.getString(snippetColumnIndex), null));
                }
                
            }            
     
        }
            
        // Sort conversations in ascending order
        Collections.sort(newestConversationMessages);
        
        return newestConversationMessages;
        
    }
    
    /**
     * Use a list of the latest text messages per conversation thread to get a list of contacts that have a conversation
     * thread on-going.
     * 
     * @param  ArrayList<Message> newestConversationMessages
     * @return ArrayList<Contact> containing contacts with a conversation thread.
     */
    public ArrayList<Contact> getContactNamesFromConversations(ArrayList<Message> newestConversationMessages) {             
        
        /*
         * Contacts columns seem to be: _ID, LOOKUP_KEY, DISPLAY_NAME, PHOTO_ID, IN_VISIBLE_GROUP, HAS_PHONE_NUMBER,
         * TIMES_CONTACTED, LAST_TIME_CONTACTED, STARRED, CUSTOM_RINGTONE, SEND_TO_VOICEMAIL 
         */
        String[] returnedColumns = {PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_ID};

        ArrayList<Contact> contacts = new ArrayList<Contact>();
        String lookupAddress = "";
        Uri lookupUri = null;
        Cursor contactsCursor = null;
        
        int displayNameColumnIndex = 0;
        int photoIdColumnIndex = 1;
        
        /*
         * For each of the latest messages, use the contact's address to find the contact's name from the phone book.
         * If a name does not exist, just use the address as a name.
         */
        for (Message m : newestConversationMessages) {             

            lookupAddress = m.getAddress();
            
            // Use the contact's number from the message list to get their contact name
            lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(lookupAddress));            
                       
            // Run the query
            contactsCursor = getContentResolver().query(lookupUri, returnedColumns, null, null, null);
            
            /*
             * If a contact is found for the given address, add it to the contacts list. Otherwise only add the address.
             */
            if (contactsCursor.moveToFirst()) {              
                
                for (int i = 0; i < contactsCursor.getColumnCount(); i++) {
                    Log.d(contactsCursor.getColumnName(i) + "", contactsCursor.getString(i) + "");
                }
                                
                Log.d("Adding contact", contactsCursor.getString(displayNameColumnIndex));
                
                contacts.add(new Contact(contactsCursor.getString(displayNameColumnIndex),
                        contactsCursor.getString(photoIdColumnIndex)));  
                
            } else {  
                Log.d("Adding contact", lookupAddress);                    
                contacts.add(new Contact(lookupAddress, null));          
            } 
            
            // Close the cursor, we're going to open a new one on the next iteration
            contactsCursor.close();
                       
        }
        
        return contacts;
        
    }
    
    /**
     * Convert a list of contacts into a list of contact names.
     * 
     * @param  contacts An ArrayList of contacts.
     * @return An ArrayList of contact names.
     */
    public ArrayList<String> getContactNames(ArrayList<Contact> contacts) {
        
        ArrayList<String> contactNames = new ArrayList<String>();
        
        for (Contact c : contacts) {
            contactNames.add(c.getContactName());
        }
        
        return contactNames;        
        
    }
    
    /**
     * Get the user's telephone number.
     * 
     * @return A string containing the user's telephone number.
     */
    public String getOwnNumber() {    
        
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String ownNumber = telephonyManager.getLine1Number();        
        
        return PhoneNumberUtils.formatNumber(ownNumber);
        
    }  

}
