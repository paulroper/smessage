/**
 * MainActivity.java
 * @author Paul Roper
 *
 * The main screen to which the user is brought when the application is launched.
 */
package com.csulcv.Smessage;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public final static String CONTACT_NAME_PHONE_NUMBER = "com.csulcv.smessage.contactNamePhoneNumber";
    private final static String TAG = "Smessage: Main Activity"; 
    
    private static final int LOADER_ID = 0;
    private ListView contactList = null;
    private ArrayAdapter<Contact> contactListAdapter = null;    

    private static final boolean loggingEnabled = true;
    
    /**
     * 
     * @see android.app.Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
 
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);        

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
            
            case R.id.action_new_message:
                Intent intent = new Intent(this, NewMessageActivity.class);
                startActivity(intent);
                return true;
            
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
        
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        
        Uri smsUri = Uri.parse("content://sms/");    

         /* SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA 
         */
         
        String[] returnedColumns = {"_id", "thread_id", "address", "body", "date", "type"};

        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id DESC, date ASC";
        
        return new CursorLoader(this, smsUri, returnedColumns, null, null, sortOrder);       
        
    }

    /*
     * Uses conversations to build the loader rather than all of the SMS messages available
     * 
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        
        Uri smsUri = Uri.parse("content://sms/conversations");    

         SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         
        String[] returnedColumns = {"thread_id", "msg_count", "snippet"};

        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id DESC, date ASC";
        
        return new CursorLoader(this, smsUri, returnedColumns, null, null, sortOrder);       
        
    }*/
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor smsCursor) {           
        
        // Get ListView used for contacts and get contacts
        contactList = (ListView) findViewById(R.id.contact_list);
        
        // Setup adapter for message list using array list of messages
        contactListAdapter = new ArrayAdapter<Contact>(this, 
                R.layout.contact_list_row, 
                R.id.contact_name, 
                getContactNamesFromConversations(getNewestConversationMessages(smsCursor)));
        
        contactList.setAdapter(contactListAdapter);          
        contactList.setOnItemClickListener(new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = (Contact) parent.getAdapter().getItem(position);
                
                Log.i("Contact clicked: ", contact.getContactName() + " " + contact.getContactPhoneNumber()); 
                
                // Create intent used to move to SendMessage activity
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);

                Bundle contactNamePhoneNumber = new Bundle();                
                intent.putExtra(CONTACT_NAME_PHONE_NUMBER, contactNamePhoneNumber);
                
                contactNamePhoneNumber.putString("CONTACT_NAME", contact.getContactName());
                contactNamePhoneNumber.putString("CONTACT_PHONE_NUMBER", contact.getContactPhoneNumber());
                
                Log.i(TAG, "Starting SendMessage activity");          
                    
                startActivity(intent);   
            }
            
        });
        
    }    
    
    /**
     * Invoked when the CursorLoader is being reset. For example, this is
     * called if the data in the provider changes and the Cursor becomes stale.
     * 
     * TODO: Refresh message list when this happens.
     * 
     * @see
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
         getSupportLoaderManager().restartLoader(LOADER_ID, null, this);          
    }        
   
    /**
     * Get the newest message from a conversation.
     *  
     * Retrieve a list of the newest messages from each conversation thread. These (along with the address of the 
     * contact the message is for) are used to build a contacts list.
     * 
     * @param smsCursor The cursor to iterate over and retrieve messages from.
     * @return          An ArrayList containing Message objects.
     */
    public ArrayList<Message> getConversations(Cursor smsCursor) {     
        
        ArrayList<Message> newestConversationMessages = new ArrayList<Message>();        
       
        // Indices are fixed constants based on position in the returnedColumns array  
        @SuppressWarnings("unused")
        final int ID_COLUMN_INDEX = 0; 
        
        @SuppressWarnings("unused")
        final int MSG_COUNT_COLUMN_INDEX = 1;
        
        final int THREAD_ID_COLUMN_INDEX = 0;       
        final int SNIPPET_COLUMN_INDEX = 2;
        
        /* 
         * Use a cursor to iterate over the SMS table. Offsets are used to (hopefully) speed up access.
         * Offsets'll have to be changed based on the number of columns we're querying.
         * 
         * TODO: Drafts are seen as new messages
         */
        while (smsCursor.moveToNext()) {  
           
            for (int i = 0; i < smsCursor.getColumnCount(); i++) {
                Log.d(smsCursor.getColumnName(i), smsCursor.getString(i));
                
                if (smsCursor.getColumnName(i).equals("body") && smsCursor.getString(i) != null) {
                    Log.i("Adding message", "Adding message to ArrayList");
                    newestConversationMessages.add(new Message(smsCursor.getInt(THREAD_ID_COLUMN_INDEX), 
                            "null", smsCursor.getString(SNIPPET_COLUMN_INDEX), null));
                }
                
            }            
     
        }
            
        // Sort conversations in ascending order
        Collections.sort(newestConversationMessages);        
        return newestConversationMessages;
        
    }
    
    /**
     * Get the newest message from a conversation.
     *  
     * Retrieve a list of the newest messages from each conversation thread. These (along with the address of the 
     * contact the message is for) are used to build a contacts list.
     * 
     * @param smsCursor The cursor to iterate over and retrieve messages from. 
     * @return          An ArrayList containing Message objects.
     */
    public ArrayList<Message> getNewestConversationMessages(Cursor smsCursor) {     
        
        ArrayList<Message> newestConversationMessages = new ArrayList<Message>();
        
        // Indices are fixed constants based on position in the returnedColumns array
        @SuppressWarnings("unused")
        final int ID_COLUMN_INDEX = 0;
        
        final int THREAD_ID_COLUMN_INDEX = 1;
        final int ADDRESS_COLUMN_INDEX = 2;
        final int MESSAGE_BODY_COLUMN_INDEX = 3;
        final int DATE_COLUMN_INDEX = 4;
                
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
                
                if (loggingEnabled) {

                    Log.i("Message " + messageCounter, "-----------------");
                    
                    for (int i = 0; i < smsCursor.getColumnCount(); i++) {                        
                        Log.d(smsCursor.getColumnName(i), smsCursor.getString(i) + "");                        
                    }
                    
                    Log.d("Adding message", "Adding message to ArrayList");                    
                    messageCounter++;                       
                    Log.i("End of messages", "-----------------");
                    
                }
                
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
               
                if (loggingEnabled) { 
                    
                    Log.i("Message " + messageCounter, "-----------------");
                    
                    for (int i = 0; i < smsCursor.getColumnCount(); i++) {              
                        Log.d(smsCursor.getColumnName(i), smsCursor.getString(i) + "");
                    }
                    
                    Log.d("Adding message", "Adding message to ArrayList");
                    
                    messageCounter++;
                
                }    
                    
                newestConversationMessages.add(new Message(smsCursor.getInt(THREAD_ID_COLUMN_INDEX), 
                        smsCursor.getString(MESSAGE_BODY_COLUMN_INDEX), smsCursor.getString(ADDRESS_COLUMN_INDEX),                         
                        smsCursor.getLong(DATE_COLUMN_INDEX)));
                
            } else {                    
                smsCursor.moveToPrevious();
            }    
    
        }
     
        // Sort messages in ascending order
        Collections.sort(newestConversationMessages);
        
        return newestConversationMessages;
        
    }    
    
    /**
     * Use a list of the latest text messages per conversation thread to get a list of contacts that have a conversation
     * thread on-going.
     * 
     * @param  newestConversationMessages The list of messages to lookup the contact names from.
     * @return                            A list containing contacts with an on-going conversation thread.
     */
    public ArrayList<Contact> getContactNamesFromConversations(ArrayList<Message> newestConversationMessages) {             
        
        /*
         * Contacts columns seem to be: _ID, LOOKUP_KEY, DISPLAY_NAME, PHOTO_ID, IN_VISIBLE_GROUP, HAS_PHONE_NUMBER,
         * TIMES_CONTACTED, LAST_TIME_CONTACTED, STARRED, CUSTOM_RINGTONE, SEND_TO_VOICEMAIL 
         */
        String[] returnedColumns = {PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER, PhoneLookup.PHOTO_ID};

        ArrayList<Contact> contacts = new ArrayList<Contact>();
        String lookupAddress = "";
        Uri lookupUri = null;
        Cursor contactsCursor = null;
        
        final int DISPLAY_NAME_COLUMN_INDEX = 0;
        final int NUMBER_COLUMN_INDEX = 1;
        final int PHOTO_ID_COLUMN_INDEX = 2;
        
        int contactCounter = 1;
        
        /*
         * For each of the latest messages, use the contact's address to find the contact's name from the phone book.
         * If a name does not exist, just use the address as a name.
         */
        for (Message m : newestConversationMessages) {             

            lookupAddress = m.getAddress();
            
            // TODO: Currently skips draft messages as they have no recipient address and the wrong thread_id
            if (lookupAddress == null) {
                continue;
            }
            
            // Use the contact's number from the message list to get their contact name
            lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(lookupAddress));            
                       
            // Run the query
            contactsCursor = getContentResolver().query(lookupUri, returnedColumns, null, null, null);
            
            /*
             * If a contact is found for the given address, add it to the contacts list. Otherwise only add the address.
             */
            if (contactsCursor.moveToFirst()) {              
                
                if (loggingEnabled) {
                
                    Log.i("Contact " + contactCounter, "-----------------");
                    
                    for (int i = 0; i < contactsCursor.getColumnCount(); i++) {
                        Log.d(contactsCursor.getColumnName(i), contactsCursor.getString(i) + "");
                    }
                                    
                    Log.d("Adding contact", contactsCursor.getString(DISPLAY_NAME_COLUMN_INDEX));
                    
                    contactCounter++;
                
                }
                    
                contacts.add(new Contact(contactsCursor.getString(DISPLAY_NAME_COLUMN_INDEX),
                        contactsCursor.getString(NUMBER_COLUMN_INDEX), 
                        contactsCursor.getString(PHOTO_ID_COLUMN_INDEX)));  
                
            } else {  
                
                if (loggingEnabled) {
                    Log.i("Contact " + contactCounter, "-----------------");
                    Log.d("Adding contact", lookupAddress + "");  
                    contactCounter++;
                }    
                    
                contacts.add(new Contact("null", lookupAddress, "null"));          
            } 

            // Close the cursor, we're going to open a new one on the next iteration
            contactsCursor.close();
                       
        }
        
        if (loggingEnabled) {
            Log.i("End of contacts", "-----------------");
        }
        
        return contacts;
        
    }
    
    /**
     * Convert a list of contacts into a list of contact names.
     * 
     * @param  contacts An ArrayList of contacts.
     * @return          An ArrayList of contact names.
     */
    public ArrayList<String> getContactNames(ArrayList<Contact> contacts) {
        
        ArrayList<String> contactNames = new ArrayList<String>();
        
        for (Contact c : contacts) {
            contactNames.add(c.getContactName());
        }
        
        return contactNames;        
        
    }    
    
}
