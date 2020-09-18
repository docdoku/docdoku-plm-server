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
package com.docdoku.plm.server.core.hooks;

import com.docdoku.plm.server.core.common.Workspace;

import javax.persistence.*;
import java.io.Serializable;

/**
 * This class is a webhook applicable on a specific {@link Workspace}.
 * A webhook is a notification function that carries event data over
 * the HTTP protocol.
 *
 * @author Morgan Guimard
 * @version 2.5, 14/10/17
 * @since V2.5
 */
@Table(name = "WEBHOOK")
@Entity
@NamedQueries({
        @NamedQuery(name = "Webhook.findByWorkspace", query = "SELECT distinct(w) FROM Webhook w WHERE w.workspace.id = :workspaceId"),
        @NamedQuery(name = "Webhook.findActiveByWorkspace", query = "SELECT distinct(w) FROM Webhook w WHERE w.workspace.id = :workspaceId AND w.active = true")
})
public class Webhook implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private boolean active;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private  Workspace workspace;

    @OneToOne(orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private WebhookApp webhookApp;

    public Webhook(WebhookApp webhookApp, String name, boolean active, Workspace workspace) {
        this.webhookApp = webhookApp;
        this.name = name;
        this.active = active;
        this.workspace = workspace;
    }

    public Webhook() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public WebhookApp getWebhookApp() {
        return webhookApp;
    }

    public void setWebhookApp(WebhookApp webhookApp) {
        this.webhookApp = webhookApp;
    }

    public String getAppName() {
        return webhookApp.getAppName();
    }
}
