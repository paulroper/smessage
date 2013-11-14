package com.csulcv.Smessage;

import java.util.Date;

/**
 * Message.java
 * 
 * @author Paul Roper
 *
 */
class Message {
     
     private int messageThreadId;
     private String messageBody;
     private String senderAddress;
     private Long messageDate;
     
     /**
      * Constructor for a message.
      * 
      * @param body The message's body.
      * @param address The address that the message is intended for.
      */
     public Message(int threadId, String body, String address, Long date) {
         messageBody = body;
         senderAddress = address;
         messageThreadId = threadId;
         messageDate = date;
     }     
     
     public int getThreadId() {
         return messageThreadId;
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
     
     public Long getDate() {
         return messageDate;
     }
    
     
    
}
