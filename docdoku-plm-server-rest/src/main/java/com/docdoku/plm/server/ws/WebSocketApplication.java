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

package com.docdoku.plm.server.ws;

import com.docdoku.plm.server.core.common.JWTokenUserGroupMapping;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.ITokenManagerLocal;
import com.docdoku.plm.server.core.services.IUserManagerLocal;
import com.docdoku.plm.server.config.AuthConfig;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class expose a web socket end point on path {contextPath}/ws
 * <p>
 * It maintains the list of current users sockets, receive, process and broadcast messages.
 * Authentication is made by passing a jwt token on first message.
 * <p>
 */
@ServerEndpoint(
        value = "/ws",
        decoders = {
                WebSocketMessageDecoder.class
        },
        encoders = {
                WebSocketMessageEncoder.class
        }
)
public class WebSocketApplication {

    private static final Logger LOGGER = Logger.getLogger(WebSocketApplication.class.getName());

    private static final String AUTH = "AUTH";

    @Inject
    @Any
    private Instance<WebSocketModule> webSocketModules;

    @Inject
    private IUserManagerLocal userManager;

    @Inject
    private WebSocketSessionsManager webSocketSessionsManager;

    @Inject
    private AuthConfig authConfig;

    @Inject
    private ITokenManagerLocal tokenManager;

    public WebSocketApplication() {
    }

    @OnError
    public void error(Session session, Throwable error) {
        LOGGER.log(Level.SEVERE, "WebSocket error", error);
        unTrackSession(session);
    }

    @OnClose
    public void close(Session session, CloseReason reason) {
        LOGGER.log(Level.FINE, "WebSocket closed with message '" +
                reason.getReasonPhrase() + "' and code " + reason.getCloseCode());
        unTrackSession(session);
    }

    @OnOpen
    public void open(Session session) {
        session.getUserProperties().put(AUTH, null);
    }

    @OnMessage
    public void message(Session session, WebSocketMessage message) {
        if (null == session.getUserProperties().get(AUTH)) {
            authenticateOrClose(session, message);
            return;
        }

        // Modules are responsible for :
        // Exit if caller is not allowed to reach callee (business)
        // Prevent caller to join himself

        WebSocketModule selectedModule = selectModule(message);

        if (null != selectedModule) {
            selectedModule.process(session, message);
        } else {
            LOGGER.log(Level.WARNING, "No modules for type " + message.getType());
        }

    }

    private void authenticateOrClose(Session session, WebSocketMessage message) {

        String type = message.getType();

        if (AUTH.equals(type)) {
            String jwt = message.getString("jwt");

            JWTokenUserGroupMapping jwTokenUserGroupMapping = tokenManager.validateAuthToken(authConfig.getJWTKey(), jwt);

            if (null != jwTokenUserGroupMapping) {
                UserGroupMapping userGroupMapping = jwTokenUserGroupMapping.getUserGroupMapping();
                String login = userGroupMapping.getLogin();
                if (login != null) {
                    session.getUserProperties().put(AUTH, jwt);
                    webSocketSessionsManager.addSession(login, session);
                    return;
                }
            }
        }

        // Authentication failed, close socket
        closeSession(session);
        unTrackSession(session);

    }

    private WebSocketModule selectModule(WebSocketMessage webSocketMessage) {
        for (WebSocketModule webSocketModule : webSocketModules) {
            if (webSocketModule.canDecode(webSocketMessage)) {
                return webSocketModule;
            }
        }
        return null;
    }

    private void closeSession(Session session) {
        try {
            session.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    private void unTrackSession(Session session) {
        if (null != session.getUserProperties().get(AUTH)) {
            webSocketSessionsManager.removeSession(session);
        }
    }

}
