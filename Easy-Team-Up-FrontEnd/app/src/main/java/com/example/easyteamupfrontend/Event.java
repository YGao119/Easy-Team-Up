package com.example.easyteamupfrontend;

import android.location.Location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class Event {
    private String name;
    private String description;
    private String ownerId;
    private String eventDate;
    private String duration;
    private String timeslots;
    private String dueDate;
    private String publicity;
    private String latitude;
    private String longitude;
    private String address;

    public Event(String name,
                 String description,
                 String ownerId,
                 String eventDate,
                 String duration,
                 String timeslots,
                 String dueDate,
                 String publicity,
                 String latitude,
                 String longitude,
                 String address
                 ){
        this.name = name;
        this.ownerId = ownerId;
        this.description = description;
        this.eventDate = eventDate;
        this.dueDate = dueDate;
        this.timeslots = timeslots;

        this.publicity = publicity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return eventDate;
    }

    public void setDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }



    public Set<String> getTimeSlots() {
        try {
            return (new ObjectMapper()).readValue(timeslots.getBytes(), Set.class);
        } catch (IOException e) {
            return null;
        }
    }

    public void setTimeSlots(Set<String> timeslots) throws JsonProcessingException {
        this.timeslots = (new ObjectMapper()).writeValueAsString(timeslots);
    }

    public String getPublicity() {
        return publicity;
    }

    public void setPublicity(String publicity) {
        this.publicity = publicity;
    }

    public LatLng getCoordinate() {
        return new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
    }

    public void setCoordinate(LatLng coordinate) {
        this.longitude = String.valueOf(coordinate.longitude);
        this.latitude = String.valueOf(coordinate.latitude);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean dueDatePassed(){
        try {
            LocalDateTime dueDateTime = LocalDateTime.parse(this.dueDate, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();
            return dueDateTime.isBefore(now);
        }
        catch (Exception e){
            return true;
        }
    }

    public String getDeterminedTime(){
        if(dueDatePassed()){
            return UserSession.determinedTime(this);
        }
        else{
            return "not determined";
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", duration='" + duration + '\'' +
                ", timeslots='" + timeslots + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", publicity='" + publicity + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
