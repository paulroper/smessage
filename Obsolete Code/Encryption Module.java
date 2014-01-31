  
    /** 
     * Encrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to encrypt.
     * @param key             A public key for encryption.
     * @return                A String containing the encrypted message.
     */
    public static String rsaEncrypt(Context activityContext, String message, CipherParameters key) throws Exception {

        Log.d(TAG, "Message to encrypt: " + message);

        byte[] data = message.getBytes(Charset.defaultCharset());           
        RSAEngine engine = new RSAEngine();
        final boolean ENCRYPT = true;
                
        // encrypt = true encrypts and encrypt = false decrypts
        engine.init(ENCRYPT, key);        
        int blockSize = engine.getInputBlockSize();
        
        Log.d(TAG, "Block size is " + blockSize + ", data array size is " + data.length);
        
        ArrayList<byte[]> blockList = new ArrayList<byte[]>();        
        
        // Turn message into chunks that are encrypted/decrypted with each iteration
        for (int chunkPos = 0; chunkPos < data.length; chunkPos += blockSize) {   
            
            Log.d(TAG, chunkPos + " / " +  blockSize + " blocks processed.");
            
            //int chunkSize = Math.min(blockSize, data.length - (chunkPos * blockSize));
            int chunkSize = Math.min(blockSize, data.length - chunkPos);
            
            Log.d(TAG, "Chunk size is: " + chunkSize + " min(" + blockSize + ", " 
                    + (data.length - chunkPos) + ")");
            
            blockList.add(engine.processBlock(data, chunkPos, chunkSize));  
        }
                
        // Rebuild the message by concatenating the blocks together into a String
        StringBuilder rsaMessage = new StringBuilder();
        
        for (byte[] block : blockList) {

            Log.d(TAG, "Block: " + Arrays.toString(block));

            // Base 64 is used so that the cipher text can be sent in a human readable form (i.e. using characters from
            // a character set).
            rsaMessage.append(new String(Base64.encode(block), Charset.defaultCharset()));
                
        }

        Log.d(TAG, "Encrypted message, base 64 " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }
    
    /** 
     * Encrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to decrypt.
     * @param key             A private key for decryption.
     * @return                A String containing the decrypted message.
     */
    public static String rsaDecrypt(Context activityContext, String message, CipherParameters key) throws Exception {
      
        Log.d(TAG, "Message to decrypt: " + message);
        
        // We need to decode the the message from base 64 before it can be decrypted
        byte[] data = Base64.decode(message.getBytes());        
        
        RSAEngine engine = new RSAEngine();
        final boolean ENCRYPT = false;
                
        // encrypt = true encrypts and encrypt = false decrypts
        engine.init(ENCRYPT, key);                   
        int blockSize = engine.getInputBlockSize();

        Log.d(TAG, "Block size is " + blockSize + ", data array size is " + data.length);
        
        ArrayList<byte[]> blockList = new ArrayList<byte[]>();        
        
        // Turn message into chunks that are decrypted with each iteration
        for (int chunkPos = 0; chunkPos < data.length; chunkPos += blockSize) {  
            
            Log.d(TAG, chunkPos + " / " +  blockSize + " blocks processed.");
            
            // As we're working on the message in chunks of 256, we'll need more than one iteration to work on messages
            // longer than this.
            //int chunkSize = Math.min(blockSize, data.length - (chunkPos * blockSize));
            int chunkSize = Math.min(blockSize, data.length - chunkPos);            
            
            Log.d(TAG, "Chunk size is: " + chunkSize + " min(" + blockSize + ", " 
                    + (data.length - chunkPos) + ")");
            
            blockList.add(engine.processBlock(data, chunkPos, chunkSize));  
        }                
        
        // Rebuild the message by concatenating the blocks together into a String
        StringBuilder rsaMessage = new StringBuilder();
        
        for (byte[] block : blockList) {
            Log.d(TAG, "Block: " + new String(block));
            rsaMessage.append(new String(block));                
        }

        Log.d(TAG, "Decrypted message: " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }