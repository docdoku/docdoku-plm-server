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

package com.docdoku.plm.server.core.common;

import javax.persistence.*;
import java.io.Serializable;

/**
 * The ProvidedAccount class maps an account to a provider and the associated subject.
 *
 * @author Morgan Guimard
 */
@Table(name = "PROVIDEDACCOUNT")
@Entity
@IdClass(com.docdoku.plm.server.core.common.ProvidedAccountKey.class)
@NamedQueries({
        @NamedQuery(name = "ProvidedAccount.getProvidedAccount", query = "SELECT p FROM ProvidedAccount p WHERE p.provider.id = :id AND p.sub = :sub"),
        @NamedQuery(name = "ProvidedAccount.getProvidedAccountFromAccount", query = "SELECT p FROM ProvidedAccount p WHERE p.account = :account")
})
public class ProvidedAccount implements Serializable {

    @Id
    @JoinColumn(name = "ID")
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    private OAuthProvider provider;

    @Id
    private String sub;

    @Id
    @JoinColumn(name = "LOGIN", unique = true)
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    private Account account;

    public ProvidedAccount() {
    }

    public ProvidedAccount(Account account, OAuthProvider provider, String sub) {
        this.account = account;
        this.provider = provider;
        this.sub = sub;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public void setProvider(OAuthProvider provider) {
        this.provider = provider;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }
}
