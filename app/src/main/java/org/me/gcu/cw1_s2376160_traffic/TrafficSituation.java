package org.me.gcu.cw1_s2376160_traffic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TrafficSituation {
    private String situationId = "";
    private String recordType = "";
    private String specificManagementType = "";
    private String comment = "";
    private String startTime = "";
    private String endTime = "";
    private String impactDetails = "";
    private String delayType = "";
    private String roadName = "Unknown Road";
    private final ArrayList<Coordinates> locations = new ArrayList<>();

    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getSpecificManagementType() { return specificManagementType; }
    public void setSpecificManagementType(String specificManagementType) {
        this.specificManagementType = specificManagementType;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) {
        this.comment = comment;
        extractRoadName(comment);
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getImpactDetails() { return impactDetails; }
    public void setImpactDetails(String impactDetails) { this.impactDetails = impactDetails; }

    public String getDelayType() { return delayType; }
    public void setDelayType(String delayType) { this.delayType = delayType; }

    public String getRoadName() { return roadName; }

    public ArrayList<Coordinates> getLocations() { return locations; }
    public void addLocation(double lat, double lng) {
        this.locations.add(new Coordinates(lat, lng));
    }

    // String breakdown to extract Scottish trunk road names (e.g. M8, A78, A90) from comment text
    private void extractRoadName(String text) {
        if (text == null || text.isEmpty()) return;
        String[] tokens = text.split("[\\s,()]+");
        for (String token : tokens) {
            if (token.matches("^[AM]\\d{1,3}(\\(M\\))?$")) {
                this.roadName = token;
                return;
            }
        }
    }

    // Computes duration in days from ISO-8601 validity timestamps for colour-coding roadworks
    public long getDurationInDays() {
        if (startTime.isEmpty() || endTime.isEmpty()) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
        try {
            String cleanStart = startTime.length() >= 19 ? startTime.substring(0, 19) : startTime;
            String cleanEnd = endTime.length() >= 19 ? endTime.substring(0, 19) : endTime;
            Date start = sdf.parse(cleanStart);
            Date end = sdf.parse(cleanEnd);
            if (start != null && end != null) {
                long diffMillis = Math.abs(end.getTime() - start.getTime());
                return TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);
            }
        } catch (ParseException ignored) {
        }
        return 0;
    }

    // Computes duration left in days from now to the end timestamp
    public long getDurationLeftInDays() {
        if (endTime.isEmpty()) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
        try {
            String cleanEnd = endTime.length() >= 19 ? endTime.substring(0, 19) : endTime;
            Date end = sdf.parse(cleanEnd);
            if (end != null) {
                long diffMillis = end.getTime() - System.currentTimeMillis();
                if (diffMillis > 0) {
                    return TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS);
                }
            }
        } catch (ParseException ignored) {
        }
        return 0;
    }

    // Haversine formula to compute distance in km from a target coordinate
    public double getDistanceFromKm(double targetLat, double targetLng) {
        if (locations.isEmpty()) return Double.MAX_VALUE;
        double lat1 = locations.get(0).getLatitude();
        double lon1 = locations.get(0).getLongitude();
        double r = 6371.0;
        double dLat = Math.toRadians(targetLat - lat1);
        double dLon = Math.toRadians(targetLng - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}