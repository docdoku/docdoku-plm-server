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

package com.docdoku.plm.server.rest.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;


@ApiModel(value = "OAuthProviderPublicDTO", description = "This class is the representation of an {@link com.docdoku.plm.server.core.common.OAuthProvider} entity")
public class OAuthProviderPublicDTO implements Serializable {

    @ApiModelProperty(value = "Id of the auth provider")
    private Integer id;

    @ApiModelProperty(value = "Name of the auth provider")
    private String name;

    @ApiModelProperty(value = "Enabled state of the auth provider")
    private boolean enabled;

    @ApiModelProperty(value = "Base url of the auth provider")
    private String issuer;

    @ApiModelProperty(value = "Provider client ID")
    private String clientID;

    @ApiModelProperty(value = "Provider algorithm")
    private String jwsAlgorithm;

    @ApiModelProperty(value = "Provider jwk set url")
    private String jwkSetURL;

    @ApiModelProperty(value = "Redirect uri")
    private String redirectUri;

    @ApiModelProperty(value = "Authority")
    private String authority;

    @ApiModelProperty(value = "Scope")
    private String scope;

    @ApiModelProperty(value = "Response type")
    private String responseType;

    @ApiModelProperty(value = "Authorization end point")
    private String authorizationEndpoint;

    @ApiModelProperty(value = "Signing keys")
    private String signingKeys;

    public OAuthProviderPublicDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(String jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    public String getJwkSetURL() {
        return jwkSetURL;
    }

    public void setJwkSetURL(String jwkSetURL) {
        this.jwkSetURL = jwkSetURL;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getSigningKeys() {
        return signingKeys;
    }

    public void setSigningKeys(String signingKeys) {
        this.signingKeys = signingKeys;
    }
}
