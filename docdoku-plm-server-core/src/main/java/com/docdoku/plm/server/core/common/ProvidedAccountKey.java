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

import java.io.Serializable;

/**
 * Identity class of {@link ProvidedAccount} objects.
 *
 * @author Morgan Guimard
 */
public class ProvidedAccountKey implements Serializable {

    private int provider;
    private String sub;
    private String account;

    public ProvidedAccountKey() {
    }

    public ProvidedAccountKey(int provider, String sub, String account) {
        this.provider = provider;
        this.sub = sub;
        this.account = account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProvidedAccountKey that = (ProvidedAccountKey) o;

        if (provider != that.provider) return false;
        if (sub != null ? !sub.equals(that.sub) : that.sub != null) return false;
        return !(account != null ? !account.equals(that.account) : that.account != null);

    }

    @Override
    public int hashCode() {
        int result = provider;
        result = 31 * result + (sub != null ? sub.hashCode() : 0);
        result = 31 * result + (account != null ? account.hashCode() : 0);
        return result;
    }

    public int getProvider() {
        return provider;
    }

    public void setProvider(int provider) {
        this.provider = provider;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
