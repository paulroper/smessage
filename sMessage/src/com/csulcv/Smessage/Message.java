package com.csulcv.Smessage;

/**
 * Message.java
 * 
 * @author Paul Roper
 *
 */
class Message {
     
     private String messageBody;
     private String senderAddress;
     
     /**
      * Constructor for a message.
      * 
      * @param body The message's body.
      * @param address The address that the message is intended for.
      */
     public Message(String body, String address) {
         messageBody = body;
         senderAddress = address;
     }
    
     /**
      * 
      * @return The body of the message.
      */
     public String getMessage() {
         return messageBody;
     }
    
     /**
      * 
      * @return The address that the message is inteded for.
      */
     public String getAddress() {
         return senderAddress;
     }
     
}
