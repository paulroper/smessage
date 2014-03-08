
    /*
     * Uses conversations to build the loader rather than all of the SMS messages available
     * 
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        
        Uri smsUri = Uri.parse("content://sms/conversations");    

         SMS columns seem to be: _ID, THREAD_ID, ADDRESS, PERSON, DATE, DATE_SENT, READ, SEEN, STATUS
         * SUBJECT, BODY, PERSON, PROTOCOL, REPLY_PATH_PRESENT, SERVICE_CENTRE, LOCKED, ERROR_CODE, META_DATA
         
        String[] returnedColumns = {"_id", "thread_id", "msg_count", "snippet"};

        // Default sort order is date DESC, change to date ASC so texts appear in order
        String sortOrder = "thread_id DESC, date ASC";
        
        return new CursorLoader(this, smsUri, returnedColumns, null, null, sortOrder);       
        
    }*/