package com.csulcv.Smessage;

/**
 * Contact.java
 * 
 * @author Paul
 *
 */
public class Contact {
    
    private String contactName;
    private String contactPhotoId;
    
    /**
     * Constructor for creating a new Contact.
     */
    public Contact (String name, String photoId) {        
        contactName = name;
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
    
}
