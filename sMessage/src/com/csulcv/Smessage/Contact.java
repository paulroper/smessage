package com.csulcv.Smessage;

public class Contact {
    
    private String contactName;
    private String contactPhotoId;
    
    public Contact (String name, String photoId) {        
        contactName = name;
        contactPhotoId = photoId;        
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhotoId() {
        return contactPhotoId;
    }   

}
