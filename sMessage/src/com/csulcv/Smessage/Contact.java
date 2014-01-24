/**
 * Contact.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

public class Contact {
    
    private String contactName;
    private String contactPhoneNumber;
    private String contactPhotoId;
    
    /**
     * Constructor for creating a new Contact.
     */
    public Contact (String name, String number, String photoId) {        
        contactName = name;
        contactPhoneNumber = number;
        contactPhotoId = photoId;        
    }

    /**
     * Returns the name of the contact.
     * 
     * @return The name of the contact.
     */
    public String getContactName() {
        return contactName;
    }
    
    /**
     * Returns the photo ID for the contact's picture (if it exists).
     * 
     * @return The photo ID of the contact's picture (if it exists).
     */
    public String getContactPhotoId() {
        return contactPhotoId;
    }   
    
    /**
     * Returns the contact's phone number.
     * 
     * @return The contact's phone number.
     */
    public String getContactPhoneNumber() {
        return contactPhoneNumber;
    }
    
    /**
     * Used by ArrayAdapter<Contact> to produce a list of Strings.
     * 
     * If no contact name exists, return the contact's phone number. 
     */
    @Override
    public String toString() {        
        if (contactName.equals("null")) {
            return contactPhoneNumber;
        } else {
            return contactName;
        }        
    }
    
}
