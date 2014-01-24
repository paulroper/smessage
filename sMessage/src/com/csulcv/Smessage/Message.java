/**
 * Message.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

class Message implements Comparable<Message> {
     
     private int messageThreadId;
     private String messageBody;
     private String senderAddress;
     private Long messageDate;
     
     /**
      * Constructor for creating a new Message.
      * 
      * @param threadId The thread ID of the conversation the message is from.
      * @param body     The actual message.
      * @param address  The recipient's phone number.
      * @param date     The date the message was sent.
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
      * Return the body of the message.
      * 
      * @return The body of the message.
      */
     public String getMessage() {
         return messageBody;
     }
    
     /**
      * Return the phone number of the message's sender/recipient.
      * 
      * @return The address that the message is intended for.
      */
     public String getAddress() {
         return senderAddress;
     }
     
     /** 
      * Return the date that the message was sent/received.
      * 
      * @return The date the message was sent/received.
      */
     public Long getDate() {
         return messageDate;
     }    
     
     /**
      * Used to compare objects so that {@link Collections#sort()} can be used.
      * 
      * @param  message The Message to compare dates with.
      * @return A value corresponding to whether the Message date is greater than, less than or equal to the 
      *         comparison date.
      */
     public int compareTo(Message message) {
         
         final int greaterThan = 1;
         final int equalTo = 0;
         final int lessThan = -1;
         
         if (message.getDate() > messageDate) {
             return greaterThan;          
         } else if (message.getDate() < messageDate) {
             return lessThan;
         } else {
             return equalTo;
         }
         
     }
     
    
}
