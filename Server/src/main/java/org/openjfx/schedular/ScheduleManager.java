package org.openjfx.schedular;



import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {

    private List<Lecture> schedule = new ArrayList<>();

    public String addLecture(String module, String room, String day, String time) {
        for (Lecture existing : schedule) {
            boolean sameTime = existing.getDay().equalsIgnoreCase(day)
                    && existing.getTime().equalsIgnoreCase(time);

            if (sameTime && existing.getRoom().equalsIgnoreCase(room))
                return "CLASH: Room " + room + " is already booked on " + day + " at " + time
                        + " for " + existing.getModule();

            if (sameTime && existing.getModule().equalsIgnoreCase(module))
                return "CLASH: " + module + " already has a lecture on " + day + " at " + time;
        }

        long totalModules  = schedule.stream().map(Lecture::getModule).distinct().count();
        boolean alreadyIn  = schedule.stream().anyMatch(l -> l.getModule().equalsIgnoreCase(module));
        if (!alreadyIn && totalModules >= 5)
            return "ERROR: Cannot add more than 5 modules.";

        schedule.add(new Lecture(module, room, day, time));
        return "SUCCESS: Lecture for " + module + " scheduled on " + day
                + " at " + time + " in room " + room;
    }

    public String removeLecture(String module, String day, String time) {
        for (Lecture lec : schedule) {
            if (lec.getModule().equalsIgnoreCase(module)
                    && lec.getDay().equalsIgnoreCase(day)
                    && lec.getTime().equalsIgnoreCase(time)) {
                String freed = "Room " + lec.getRoom() + " on " + day + " at " + time + " is now free.";
                schedule.remove(lec);
                return "SUCCESS: Removed " + module + " lecture. " + freed;
            }
        }
        return "ERROR: No lecture found for " + module + " on " + day + " at " + time;
    }

    public String getSchedule() {
        if (schedule.isEmpty()) return "EMPTY: No lectures scheduled yet.";

        StringBuilder sb = new StringBuilder("SCHEDULE:");
        for (Lecture lec : schedule) {
            sb.append(lec.getModule()).append("|")
                    .append(lec.getRoom()).append("|")
                    .append(lec.getDay()).append("|")
                    .append(lec.getTime()).append(";");
        }
        return sb.toString();
    }
}
