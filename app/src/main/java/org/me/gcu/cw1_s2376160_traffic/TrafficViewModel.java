package org.me.gcu.cw1_s2376160_traffic;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficViewModel extends AndroidViewModel {
    private static final String TAG = "TrafficViewModel";
    public static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private static final int MSG_DATA_PARSED = 100;
    private static final int MSG_OFFLINE_FALLBACK = 101;
    private static final long REFRESH_INTERVAL_MS = 600000L;

    private final MutableLiveData<ArrayList<TrafficSituation>> displayedSituations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<TrafficSituation> selectedSituation = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    private ArrayList<TrafficSituation> fullCurrentList = new ArrayList<>();
    private String currentFeedUrl = UNPLANNED_URL;
    private String clientId = "";
    private String clientKey = "";
    private String currentRoadFilter = "";
    private double currentRadiusKm = -1;
    // Default reference coordinates: Glasgow George Square (55.8611, -4.2502)
    private double userLat = 55.8611;
    private double userLng = -4.2502;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_DATA_PARSED) {
                fullCurrentList = (ArrayList<TrafficSituation>) msg.obj;
                applyFilters();
                statusMessage.setValue("Feed updated (" + fullCurrentList.size() + " items)");
            } else if (msg.what == MSG_OFFLINE_FALLBACK) {
                fullCurrentList = (ArrayList<TrafficSituation>) msg.obj;
                applyFilters();
                statusMessage.setValue("Offline mode: showing cached data");
            }
        }
    };

    private final Runnable periodicRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchFeedData(currentFeedUrl);
            mainHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    public TrafficViewModel(@NonNull Application application) {
        super(application);
    }

    public void initCredentialsAndStart(String id, String key) {
        boolean firstStart = this.clientId.isEmpty();
        this.clientId = id != null ? id : "";
        this.clientKey = key != null ? key : "";
        if (firstStart) {
            startAutoRefresh();
        }
    }

    public void startAutoRefresh() {
        mainHandler.removeCallbacks(periodicRefreshRunnable);
        mainHandler.post(periodicRefreshRunnable);
    }

    public void stopAutoRefresh() {
        mainHandler.removeCallbacks(periodicRefreshRunnable);
    }

    public void switchFeed(String feedUrl) {
        this.currentFeedUrl = feedUrl;
        fetchFeedData(feedUrl);
    }

    public void fetchFeedData(String targetUrl) {
        executorService.execute(new NetworkDownloadTask(targetUrl));
    }

    public void setFilters(String roadNameQuery, double radiusKm, double lat, double lng) {
        this.currentRoadFilter = roadNameQuery != null ? roadNameQuery.trim().toUpperCase() : "";
        this.currentRadiusKm = radiusKm;
        this.userLat = lat;
        this.userLng = lng;
        applyFilters();
    }

    private void applyFilters() {
        ArrayList<TrafficSituation> filtered = new ArrayList<>();
        for (TrafficSituation item : fullCurrentList) {
            boolean matchesRoad = currentRoadFilter.isEmpty()
                    || item.getRoadName().toUpperCase().contains(currentRoadFilter)
                    || item.getComment().toUpperCase().contains(currentRoadFilter);
            boolean matchesDistance = currentRadiusKm <= 0
                    || item.getDistanceFromKm(userLat, userLng) <= currentRadiusKm;
            if (matchesRoad && matchesDistance) {
                filtered.add(item);
            }
        }
        displayedSituations.setValue(filtered);
    }

    public LiveData<ArrayList<TrafficSituation>> getDisplayedSituations() { return displayedSituations; }
    public LiveData<TrafficSituation> getSelectedSituation() { return selectedSituation; }
    public void selectSituation(TrafficSituation situation) { selectedSituation.setValue(situation); }
    public LiveData<String> getStatusMessage() { return statusMessage; }

    private class NetworkDownloadTask implements Runnable {
        private final String targetUrl;

        NetworkDownloadTask(String url) {
            this.targetUrl = url;
        }

        @Override
        public void run() {
            String cacheFileName = targetUrl.contains("UnplannedEvents")
                    ? "unplanned_cache.xml" : "roadworks_cache.xml";
            StringBuilder resultBuilder = new StringBuilder();
            boolean networkSuccess = false;

            try {
                URL url = new URL(targetUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(8000);
                urlConnection.setReadTimeout(8000);
                String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes(StandardCharsets.UTF_8));
                urlConnection.setRequestProperty("Authorization", auth);

                try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        resultBuilder.append(inputLine);
                    }
                    networkSuccess = true;
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e(TAG, "Network download failed, falling back to cache: " + e.getMessage());
            }

            if (networkSuccess && resultBuilder.length() > 0) {
                String xmlData = resultBuilder.toString();
                saveToInternalCache(cacheFileName, xmlData);
                ArrayList<TrafficSituation> parsed = DatexXmlParser.parseXmlString(xmlData);
                Message msg = mainHandler.obtainMessage(MSG_DATA_PARSED, parsed);
                mainHandler.sendMessage(msg);
            } else {
                String cachedXml = loadFromInternalCache(cacheFileName);
                ArrayList<TrafficSituation> parsed = DatexXmlParser.parseXmlString(cachedXml);
                Message msg = mainHandler.obtainMessage(MSG_OFFLINE_FALLBACK, parsed);
                mainHandler.sendMessage(msg);
            }
        }
    }

    private void saveToInternalCache(String fileName, String content) {
        File file = new File(getApplication().getFilesDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Cache write error: " + e.getMessage());
        }
    }

    private String loadFromInternalCache(String fileName) {
        File file = new File(getApplication().getFilesDir(), fileName);
        if (!file.exists()) return "";
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Cache read error: " + e.getMessage());
        }
        return sb.toString();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
        executorService.shutdownNow();
    }
}