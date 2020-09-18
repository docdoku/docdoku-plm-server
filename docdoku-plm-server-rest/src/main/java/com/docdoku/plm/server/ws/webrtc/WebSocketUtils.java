package com.docdoku.plm.server.ws.webrtc;

import com.docdoku.plm.server.ws.WebSocketMessage;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Arrays;
import java.util.List;

public class WebSocketUtils {
    public static final String WEBRTC_OFFLINE = "OFFLINE";
    public static final String WEBRTC_INVITE = "WEBRTC_INVITE";
    public static final String WEBRTC_ACCEPT = "WEBRTC_ACCEPT";
    public static final String WEBRTC_REJECT = "WEBRTC_REJECT";
    public static final String WEBRTC_HANGUP = "WEBRTC_HANGUP";
    public static final String WEBRTC_ROOM_JOIN_EVENT = "WEBRTC_ROOM_JOIN_EVENT";
    public static final String WEBRTC_ROOM_REJECT_EVENT = "WEBRTC_ROOM_REJECT_EVENT";

    public static final String WEBRTC_OFFER = "offer";
    public static final String WEBRTC_ANSWER = "answer";
    public static final String WEBRTC_BYE = "bye";
    public static final String WEBRTC_CANDIDATE = "candidate";

    WebSocketUtils() {
    }

    public static WebSocketMessage createMessage(String type, String remoteUser, String roomKey, String reason, String context, Integer roomOccupancy, String userLogin, String sdp, Integer label, String id, String candidate) {

        JsonObjectBuilder jsonObject = Json.createObjectBuilder();
        jsonObject.add("type", type);

        if (remoteUser != null) {
            jsonObject.add("remoteUser", remoteUser);
        }
        if (roomKey != null) {
            jsonObject.add("roomKey", roomKey);
        }
        if (reason != null) {
            jsonObject.add("reason", reason);
        }
        if (context != null) {
            jsonObject.add("context", context);
        }
        if (roomOccupancy != null) {
            jsonObject.add("roomOccupancy", roomOccupancy);
        }
        if (userLogin != null) {
            jsonObject.add("userLogin", userLogin);
        }

        // Signals
        if (sdp != null) {
            jsonObject.add("sdp", sdp);
        }
        if (id != null) {
            jsonObject.add("id", id);
        }
        if (candidate != null) {
            jsonObject.add("candidate", candidate);
        }
        if (label != null) {
            jsonObject.add("label", label);
        }

        return new WebSocketMessage(jsonObject.build());
    }

    public static List<String> getAllTypes() {
        return Arrays.asList(
                WEBRTC_INVITE, WEBRTC_ACCEPT, WEBRTC_REJECT, WEBRTC_HANGUP,
                WEBRTC_OFFER, WEBRTC_ANSWER, WEBRTC_BYE, WEBRTC_CANDIDATE
        );
    }
}