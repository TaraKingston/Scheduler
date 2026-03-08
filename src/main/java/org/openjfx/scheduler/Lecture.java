package org.openjfx.scheduler;


public class Lecture {
    private String day;
    private String time;
    private String module;
    private String room;

    public Lecture(String module, String room, String day, String time) {
        this.module = module;
        this.room   = room;
        this.day    = day;
        this.time   = time;
    }

    public String getDay()    { return day; }
    public String getTime()   { return time; }
    public String getModule() { return module; }
    public String getRoom()   { return room; }
}

