package com.roamwise.service;

import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.DayResponse;
import com.roamwise.dto.TripResponse;
import org.springframework.stereotype.Service;

@Service
public class IcsExportService {

    public String generateIcs(TripResponse trip) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Roamwise//Trip Export//EN\r\n");

        for (DayResponse day : trip.getDays()) {
            for (ActivityResponse activity : day.getActivities()) {
                String dateStr = day.getDayDate().toString().replace("-", "");
                String startStr = activity.getStartTime().toString().replace(":", "") + "00";
                String endStr = activity.getEndTime().toString().replace(":", "") + "00";

                sb.append("BEGIN:VEVENT\r\n");
                sb.append("UID:").append(activity.getId()).append("@roamwise.com\r\n");
                sb.append("DTSTART:").append(dateStr).append("T").append(startStr.substring(0, 6)).append("\r\n");
                sb.append("DTEND:").append(dateStr).append("T").append(endStr.substring(0, 6)).append("\r\n");
                sb.append("SUMMARY:").append(activity.getName()).append("\r\n");
                if (activity.getNotes() != null) {
                    sb.append("DESCRIPTION:").append(activity.getNotes()).append("\r\n");
                }
                sb.append("END:VEVENT\r\n");
            }
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }
}
