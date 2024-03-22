package org.epics.archiverappliance.retrieval;

import org.epics.archiverappliance.common.TimeUtils;
import org.epics.archiverappliance.common.reports.Details;
import org.epics.archiverappliance.config.ConfigService;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class RetrievalMetrics implements Details {
    private long numberOfRequests = 0;
    private Instant lastRequest = null;
    private final Set<String> userIdentifiers = new HashSet<>();

    public RetrievalMetrics() {}

    public static final RetrievalMetrics EMPTY_METRICS = new RetrievalMetrics();

    public void updateMetrics(Instant lastRequest, String user) {
        this.numberOfRequests += 1;
        this.userIdentifiers.add(user);
        this.lastRequest = lastRequest;
    }

    public RetrievalMetrics sumMetrics(RetrievalMetrics newMetrics) {
        this.numberOfRequests += newMetrics.numberOfRequests;
        this.userIdentifiers.addAll(newMetrics.userIdentifiers);
        if (newMetrics.lastRequest != null) {
            if (this.lastRequest == null) {
                this.lastRequest = newMetrics.lastRequest;
            } else {
                long latestEpochMillis =
                        Long.max(this.lastRequest.toEpochMilli(), newMetrics.lastRequest.toEpochMilli());
                this.lastRequest = Instant.ofEpochMilli(latestEpochMillis);
            }
        }
        return this;
    }

    public static RetrievalMetrics calculateSummedMetrics(ConfigService configService) {
        var allMetrics = configService.getRetrievalRuntimeState().getRetrievalMetrics();
        return allMetrics.values().stream().reduce(new RetrievalMetrics(), RetrievalMetrics::sumMetrics);
    }

    public Map<String, String> getMetrics() {
        return Map.of(
                "Number of Retrieval Requests",
                String.valueOf(this.numberOfRequests),
                "Time of last Retrieval Request",
                TimeUtils.convertToHumanReadableString(lastRequest),
                "Number of unique users",
                String.valueOf(userIdentifiers.size()));
    }

    @Override
    public ConfigService.WAR_FILE source() {
        return ConfigService.WAR_FILE.RETRIEVAL;
    }

    @Override
    public LinkedList<Map<String, String>> details(ConfigService configService) {
        LinkedList<Map<String, String>> result = new LinkedList<>();
        result.add(metricDetail("Number of Retrieval Requests", String.valueOf(this.numberOfRequests)));
        result.add(metricDetail("Time of last Retrieval Request", TimeUtils.convertToHumanReadableString(lastRequest)));
        result.add(metricDetail("Number of unique users", String.valueOf(userIdentifiers.size())));
        return result;
    }
}
