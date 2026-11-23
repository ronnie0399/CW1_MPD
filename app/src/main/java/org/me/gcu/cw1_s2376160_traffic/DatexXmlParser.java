package org.me.gcu.cw1_s2376160_traffic;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class DatexXmlParser {
    private static final String TAG = "Parsing";

    public static ArrayList<TrafficSituation> parseXmlString(String xmlString) {
        ArrayList<TrafficSituation> situations = new ArrayList<>();
        if (xmlString == null || xmlString.isEmpty()) return situations;

        int startIndex = xmlString.indexOf("<?");
        if (startIndex > 0) {
            xmlString = xmlString.substring(startIndex);
        }

        TrafficSituation current = null;
        boolean insideGeneralPublicComment = false;
        Double tempLat = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xmlString));

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();

                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName.equalsIgnoreCase("situation")) {
                        current = new TrafficSituation();
                        if (xpp.getAttributeCount() > 0) {
                            current.setSituationId(xpp.getAttributeValue(0));
                        }
                    } else if (current != null) {
                        if (tagName.equalsIgnoreCase("situationRecord")) {
                            if (xpp.getAttributeCount() > 0) {
                                String typeAttr = xpp.getAttributeValue(0);
                                current.setRecordType(typeAttr);
                                Log.d(TAG, "Situation record attribute: " + typeAttr);
                            }
                        } else if (tagName.equalsIgnoreCase("generalPublicComment")) {
                            insideGeneralPublicComment = true;
                        } else if (tagName.equalsIgnoreCase("value") && insideGeneralPublicComment) {
                            current.setComment(xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("overallStartTime")) {
                            current.setStartTime(xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("overallEndTime")) {
                            current.setEndTime(xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("numberOfOperationalLanes")) {
                            current.setImpactDetails("Operational lanes: " + xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("numberOfLanesRestricted")) {
                            current.setImpactDetails("Restricted lanes: " + xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("delaysType")) {
                            current.setDelayType(xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                                || tagName.equalsIgnoreCase("abnormalTrafficType")
                                || tagName.equalsIgnoreCase("roadMaintenanceType")) {
                            current.setSpecificManagementType(xpp.nextText().trim());
                        } else if (tagName.equalsIgnoreCase("latitude")) {
                            try {
                                tempLat = Double.parseDouble(xpp.nextText().trim());
                            } catch (NumberFormatException ignored) {
                            }
                        } else if (tagName.equalsIgnoreCase("longitude")) {
                            try {
                                double lng = Double.parseDouble(xpp.nextText().trim());
                                if (tempLat != null) {
                                    current.addLocation(tempLat, lng);
                                    tempLat = null;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (tagName.equalsIgnoreCase("generalPublicComment")) {
                        insideGeneralPublicComment = false;
                    } else if (tagName.equalsIgnoreCase("situation") && current != null) {
                        situations.add(current);
                        current = null;
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "XML Parsing Exception: " + e.getMessage());
        }
        Log.d(TAG, "Total parsed situations: " + situations.size());
        return situations;
    }
}