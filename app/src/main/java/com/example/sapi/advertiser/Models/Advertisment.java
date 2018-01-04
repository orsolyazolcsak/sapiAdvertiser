package com.example.sapi.advertiser.Models;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Intern on 11/7/2017.
 */

public class Advertisment {
    public  String title;
    public String description;
    public String image;
    public String location;
    public double locationLat;
    public double locationLng;
    public String userImage;
    public String uid;
    public List<String> adImages =  Collections.synchronizedList(new ArrayList());


    public Advertisment(){

    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", description);
        result.put("image", image);
        result.put("location", location);
        result.put("locationLat", locationLat);
        result.put("locationLng", locationLat);
        result.put("userImage", userImage);
        result.put("uid", uid);
        result.put("adImages", adImages);
        return result;
    }
}
