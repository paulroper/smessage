/**
 * Message.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

class Message implements Comparable<Message> {

    private int threadId;
    private String body;
    private String address;
    private Long date;

    /**
     * Constructor for creating a new Message.
     *
     * @param threadId The thread ID of the conversation the message is from.
     * @param body     The actual message.
     * @param address  The recipient's phone number.
     * @param date     The date the message was sent.
     */
    public Message(int threadId, String body, String address, Long date) {
        this.body = body;
        this.address = address;
        this.threadId = threadId;
        this.date = date;
    }

    public int getThreadId() {
        return threadId;
    }

    /**
     * Return the body of the message.
     *
     * @return The body of the message.
     */
    public String getBody() {
        return body;
    }


    /**
     * Return the phone number of the message's sender/recipient.
     *
     * @return The address that the message is intended for.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Return the date that the message was sent/received.
     *
     * @return The date the message was sent/received.
     */
    public Long getDate() {
        return date;
    }

    /**
     * Update the body of the message with a new version. Used to show a decrypted version of a message.
     *
     * @param body The new message body to replace with.
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Used to compare objects so that the sort method from the Java collections can be used.
     *
     * @param message The Message to compare dates with.
     * @return A value corresponding to whether the Message date is greater than, less than or equal to the
     * comparison date.
     */
    public int compareTo(Message message) {

        final int greaterThan = 1;
        final int equalTo = 0;
        final int lessThan = -1;

        if (message.getDate() > date) {
            return greaterThan;
        } else if (message.getDate() < date) {
            return lessThan;
        } else {
            return equalTo;
        }

    }

    public String toString() {
        return body;
    }

}
