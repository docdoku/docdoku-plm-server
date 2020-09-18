/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2020 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.plm.server.ws.webrtc;


import com.docdoku.plm.server.ws.WebSocketMessage;
import com.docdoku.plm.server.ws.WebSocketModule;
import com.docdoku.plm.server.ws.WebSocketSessionsManager;
import com.docdoku.plm.server.ws.chat.Room;

import javax.inject.Inject;
import javax.websocket.Session;
import java.util.logging.Logger;

import static com.docdoku.plm.server.ws.webrtc.WebSocketUtils.*;

/**
 * WebRTC module plugin implementation
 *
 * @author Morgan Guimard
 */
@WebRTCWebSocketModule
public class WebRTCWebSocketModuleImpl implements WebSocketModule {

    private static final Logger LOGGER = Logger.getLogger(WebRTCWebSocketModuleImpl.class.getName());

    @Inject
    private WebSocketSessionsManager webSocketSessionsManager;

    @Override
    public boolean canDecode(WebSocketMessage webSocketMessage) {
        return WebSocketUtils.getAllTypes().contains(webSocketMessage.getType());
    }

    @Override
    public void process(Session session, WebSocketMessage webSocketMessage) {

        String sender = webSocketSessionsManager.getHolder(session);
        if(sender != null) {
            switch (webSocketMessage.getType()) {

                case WEBRTC_INVITE:
                    onWebRTCInviteMessage(sender, session, webSocketMessage);
                    break;

                case WEBRTC_ACCEPT:
                    onWebRTCAcceptMessage(sender, session, webSocketMessage);
                    break;

                case WEBRTC_REJECT:
                    onWebRTCRejectMessage(sender, session, webSocketMessage);
                    break;

                case WEBRTC_HANGUP:
                    onWebRTCHangupMessage(sender, session, webSocketMessage);
                    break;

                case WEBRTC_ANSWER:
                case WEBRTC_OFFER:
                case WEBRTC_CANDIDATE:
                case WEBRTC_BYE:
                    processP2P(sender, session, webSocketMessage);
                    break;

                default:
                    break;
            }
        } else {
            LOGGER.info("Request with unregistered session");
        }
    }


    private void processP2P(String sender, Session session, WebSocketMessage webSocketMessage) {
        // webRTC P2P signaling messages
        // These messages are forwarded to the remote peer(s) in the room

        String roomKey = webSocketMessage.getString("roomKey");
        Room room = Room.getByKeyName(roomKey);

        if (room != null && room.hasUser(sender)) {
            // forward the message to the other peer
            Session otherSession = room.getOtherUserSession(session);

            // on bye message, remove the user from the room
            if (WEBRTC_BYE.equals(webSocketMessage.getType())) {
                room.removeSession(session);
            }

            if (otherSession != null) {
                webSocketSessionsManager.send(otherSession, webSocketMessage);
            }
        }

    }

    private void onWebRTCHangupMessage(String sender, Session session, WebSocketMessage webSocketMessage) {
        String roomKey = webSocketMessage.getString("roomKey");
        Room room = Room.getByKeyName(roomKey);

        if (room != null) {
            Session otherSession = room.getOtherUserSession(session);
            room.removeSession(session);

            WebSocketMessage message = WebSocketUtils.createMessage(WEBRTC_HANGUP, sender, roomKey, null, null, 0, null, null, null, null, null);
            webSocketSessionsManager.send(otherSession, message);

        }
    }

    private void onWebRTCRejectMessage(String sender, Session session, WebSocketMessage webSocketMessage) {
        String roomKey = webSocketMessage.getString("roomKey");
        String reason = webSocketMessage.getString("reason");
        Room room = Room.getByKeyName(roomKey);
        String remoteUser = webSocketMessage.getString("remoteUser");

        if (room != null) {
            // send "room reject event" to caller, to remove invitations in other tabs if any

            WebSocketMessage message = WebSocketUtils.createMessage(WEBRTC_ROOM_REJECT_EVENT, null, roomKey, reason, null, room.getOccupancy(), sender, null, null, null, null);
            webSocketSessionsManager.broadcast(sender, message);

            Session otherSession = room.getUserSession(remoteUser);
            if (otherSession != null) {
                WebSocketMessage otherMessage = WebSocketUtils.createMessage(WEBRTC_REJECT, sender, roomKey, reason, null, 0, null, null, null, null, null);
                webSocketSessionsManager.send(otherSession, otherMessage);
            }
        }
    }

    private void onWebRTCAcceptMessage(String sender, Session session, WebSocketMessage webSocketMessage) {
        String roomKey = webSocketMessage.getString("roomKey");
        Room room = Room.getByKeyName(roomKey);

        if (room != null && !room.hasUser(sender) && room.getOccupancy() == 1) {

            room.addUserSession(session, sender);
            // send room join event to caller (all channels to remove invitations if any)
            WebSocketMessage message = WebSocketUtils.createMessage(WEBRTC_ROOM_JOIN_EVENT, sender, roomKey, null, null, room.getOccupancy(), sender, null, null, null, null);
            webSocketSessionsManager.broadcast(sender, message);

            // send room join event to the other user in room
            Session otherSession = room.getOtherUserSession(session);

            if (otherSession != null) {
                WebSocketMessage otherMessage = WebSocketUtils.createMessage(WEBRTC_ACCEPT, sender, roomKey, null, null, 0, null, null, null, null, null);
                webSocketSessionsManager.send(otherSession, otherMessage);
            }
        }
    }


    private void onWebRTCInviteMessage(String sender, Session session, WebSocketMessage webSocketMessage) {

        String remoteUser = webSocketMessage.getString("remoteUser");
        String roomKey = sender + "-" + remoteUser;

        if (!webSocketSessionsManager.hasSessions(remoteUser)) {
            WebSocketMessage message = WebSocketUtils.createMessage(WEBRTC_REJECT, null, roomKey, WebSocketUtils.WEBRTC_OFFLINE, null, 0, remoteUser, null, null, null, null);
            webSocketSessionsManager.send(session, message);
            return;
        }


        Room room = Room.getByKeyName(roomKey);

        if (room == null) {
            room = new Room(roomKey);
        }
        //else :  multiple invitations, caller is spamming or something goes wrong.
        // the room is ready to receive user sessions.
        // add the caller session in the room
        room.addUserSession(session, sender);

        // send room join event to caller session (single channel)
        WebSocketMessage message = WebSocketUtils.createMessage(WEBRTC_ROOM_JOIN_EVENT, null, roomKey, null, null, room.getOccupancy(), sender, null, null, null, null);
        webSocketSessionsManager.send(session, message);

        // send invitation to the remote user sessions (all channels)
        WebSocketMessage remoteUserMessage = WebSocketUtils.createMessage(WEBRTC_INVITE, sender, roomKey, null, webSocketMessage.getString("context"), room.getOccupancy(), null, null, null, null, null);
        webSocketSessionsManager.broadcast(remoteUser, remoteUserMessage);
    }

}
