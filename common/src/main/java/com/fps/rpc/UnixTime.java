package com.fps.rpc;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UnixTime {

    private final long unixEpochInSecs;
    private final String signature;

    public UnixTime(String signature) {
        this(Instant.now().getEpochSecond(), signature);
    }

    public UnixTime(long unixEpochInSecs, String signature) {
        this.unixEpochInSecs = unixEpochInSecs;
        this.signature = signature;
    }

    public long value() {
        return unixEpochInSecs;
    }

    @Override
    public String toString() {
        return getFormattedTime(signature, unixEpochInSecs);
    }

    public static String getFormattedTime(String signature, long epochTimeInSeconds) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        return signature + " " + formatter.format(Instant.ofEpochSecond(epochTimeInSeconds));
    }

}

