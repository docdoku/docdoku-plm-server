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

package com.docdoku.plm.server.ws.collaborative;


import com.docdoku.plm.server.ws.WebSocketApplication;
import com.docdoku.plm.server.ws.WebSocketMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.websocket.Session;
import java.io.StringReader;
import java.util.HashMap;

/**
 * @author Asmae CHADID
 */
@RunWith(MockitoJUnitRunner.class)
public class CollaborativeRoomTest {
    private static Session master = Mockito.mock(Session.class);
    private static Session slave1 = Mockito.mock(Session.class);
    private static Session slave2 = Mockito.mock(Session.class);

    @BeforeClass
    public static void init() {
        Mockito.when(master.getUserProperties()).thenReturn(new HashMap<String, Object>() {{
            put(WebSocketApplication.LOGIN, "master1");
        }});

        Mockito.when(slave1.getUserProperties()).thenReturn(new HashMap<String, Object>() {{
            put(WebSocketApplication.LOGIN, "slave1");
        }});

        Mockito.when(slave2.getUserProperties()).thenReturn(new HashMap<String, Object>() {{
            put(WebSocketApplication.LOGIN, "slave2");
        }});
    }

    @Test
    public void shouldReturnEmptyMasterName() {
        //Given
        CollaborativeRoom nullCollaborativeRoom = Mockito.spy(new CollaborativeRoom(null));
        //then
        Assert.assertTrue(nullCollaborativeRoom.getMasterName().isEmpty());
    }

    @Test
    public void shouldReturnNotNullMasterName() {
        //Given
        CollaborativeRoom nullCollaborativeRoom = Mockito.spy(new CollaborativeRoom(master));
        //then
        Assert.assertTrue(!nullCollaborativeRoom.getMasterName().isEmpty());
    }

    @Test
    public void shouldReturnNotNullCollaborativeRoom() {
        //Given
        CollaborativeRoom collaborativeRoom = Mockito.spy(new CollaborativeRoom(master));
        //Then
        Assert.assertNotNull(CollaborativeRoom.getByKeyName(collaborativeRoom.getKey()));
    }

    @Test
    public void shouldReturnFourRooms() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom2 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom3 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom4 = Mockito.spy(new CollaborativeRoom(master));

        //Then
        Assert.assertEquals(4, CollaborativeRoom.getAllCollaborativeRooms().size());
    }

    @Test
    public void shouldDeleteRooms() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom2 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom3 = Mockito.spy(new CollaborativeRoom(master));
        CollaborativeRoom collaborativeRoom4 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.delete();
        collaborativeRoom2.delete();
        //Then
        Assert.assertEquals(2, CollaborativeRoom.getAllCollaborativeRooms().size());
    }

    @Test
    public void shouldReturnMaster() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //Then
        Assert.assertEquals("master1", collaborativeRoom1.getLastMaster());
    }

    @Test
    public void shouldAddSlave() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.addSlave(slave1);
        //Then
        Assert.assertEquals(1, collaborativeRoom1.getSlaves().size());
    }

    @Test
    public void shouldRemoveSlave() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.addSlave(slave1);
        collaborativeRoom1.addSlave(slave2);
        collaborativeRoom1.removeSlave(slave1);
        //Then
        Assert.assertEquals(slave2, collaborativeRoom1.getSlaves().get(0));
    }

    @Test
    public void shouldAddPendingUser() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.addPendingUser("user1");
        collaborativeRoom1.addPendingUser("user2");
        collaborativeRoom1.addPendingUser("user2");
        //Then
        Assert.assertEquals(3, collaborativeRoom1.getPendingUsers().size());
    }

    @Test
    public void shouldRemovePendingUser() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.addPendingUser("user1");
        collaborativeRoom1.addPendingUser("user2");
        collaborativeRoom1.addPendingUser("user2");
        collaborativeRoom1.addPendingUser("user2");

        collaborativeRoom1.removePendingUser("user2");
        //Then
        Assert.assertEquals(3, collaborativeRoom1.getPendingUsers().size());
    }

    @Test
    public void shouldReturnSlave1Slave2AndNull() {
        //Given
        CollaborativeRoom collaborativeRoom1 = Mockito.spy(new CollaborativeRoom(master));
        //When
        collaborativeRoom1.addSlave(slave1);
        collaborativeRoom1.addSlave(slave2);
        //Then
        Assert.assertEquals(slave1, collaborativeRoom1.findUserSession("slave1"));
        Assert.assertEquals(slave2, collaborativeRoom1.findUserSession("slave2"));
        Assert.assertNull(collaborativeRoom1.findUserSession("slave3"));
    }

    @Test
    public void shouldSaveCommands() {
        //Given
        String msg = "{  \n" +
                "   \"broadcastMessage\":{  \n" +
                "      \"cameraInfos\":{  \n" +
                "         \"target\":{  \n" +
                "            \"x\":0,\n" +
                "            \"y\":0,\n" +
                "            \"z\":0\n" +
                "         },\n" +
                "         \"camPos\":{  \n" +
                "            \"x\":2283.8555345202267,\n" +
                "            \"y\":1742.2368392950543,\n" +
                "            \"z\":306.5925754554133\n" +
                "         },\n" +
                "         \"camOrientation\":{  \n" +
                "            \"x\":-0.16153026619659236,\n" +
                "            \"y\":0.9837903505522302,\n" +
                "            \"z\":0.07787502335635015\n" +
                "         },\n" +
                "         \"layers\":\"create layer\",\n" +
                "         \"colourEditedObjects\":true,\n" +
                "         \"clipping\":\"1\",\n" +
                "         \"explode\":\"3\",\n" +
                "         \"smartPath\":[  \n" +
                "            \"9447-9445-9441\",\n" +
                "            \"9447-9445-9443\",\n" +
                "            \"9447-9445-9444\",\n" +
                "            \"9446\"\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";


        JsonObject jsObj = Json.createReader(new StringReader(msg)).readObject();
        JsonObject broadcastMessage = jsObj.containsKey("broadcastMessage") ? jsObj.getJsonObject("broadcastMessage") : null;

        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("type", "COLLABORATIVE_COMMANDS")
                .add("key", "key-12545695-7859-458")
                .add("broadcastMessage", broadcastMessage)
                .add("remoteUser", "slave1");


        WebSocketMessage collaborativeMessage = Mockito.spy(new WebSocketMessage(b.build()));

        CollaborativeRoom room = Mockito.spy(new CollaborativeRoom(master));
        //When
        room.addSlave(slave1);
        room.addSlave(slave2);

        JsonObject commands = collaborativeMessage.getJsonObject("broadcastMessage");
        room.saveCommand(commands);
        Assert.assertEquals(1, room.getCommands().entrySet().size());
    }


    @After
    public void clearData() {
        for (CollaborativeRoom room : CollaborativeRoom.getAllCollaborativeRooms()) {
            room.delete();
        }
    }


}
