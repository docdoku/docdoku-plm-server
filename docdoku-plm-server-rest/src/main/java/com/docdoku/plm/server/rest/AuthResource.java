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

package com.docdoku.plm.server.rest;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import io.swagger.annotations.*;
import org.dozer.DozerBeanMapperSingletonWrapper;
import org.dozer.Mapper;
import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.OAuthProvider;
import com.docdoku.plm.server.core.common.ProvidedAccount;
import com.docdoku.plm.server.core.exceptions.AccountAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.EntityNotFoundException;
import com.docdoku.plm.server.core.exceptions.ProvidedAccountNotFoundException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.config.AuthConfig;
import com.docdoku.plm.server.auth.AuthServices;
import com.docdoku.plm.server.rest.dto.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@Path("auth")
@Api(value = "auth", description = "Operations about authentication")
public class AuthResource {

    @Inject
    private IAccountManagerLocal accountManager;
    @Inject
    private IUserManagerLocal userManager;
    @Inject
    private IContextManagerLocal contextManager;
    @Inject
    private IOAuthManagerLocal oAuthManager;
    @Inject
    private AuthConfig authConfig;
    @Inject
    private ITokenManagerLocal tokenManager;

    private static final Logger LOGGER = Logger.getLogger(AuthResource.class.getName());
    private Mapper mapper;

    public AuthResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @POST
    @Path("/login")
    @ApiOperation(value = "Try to authenticate with credentials", response = AccountDTO.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful login"),
            @ApiResponse(code = 403, message = "Unsuccessful login"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpServletRequest request, @Context HttpServletResponse response,
            @ApiParam(required = true, value = "Login request") LoginRequestDTO loginRequestDTO)
                    throws EntityNotFoundException {

        String login = loginRequestDTO.getLogin();
        String password = loginRequestDTO.getPassword();

        Account account = accountManager.authenticateAccount(login, password);

        if (null != account && oAuthManager.isProvidedAccount(account)) {
            return Response.status(403).build();
        }

        HttpSession session = request.getSession();

        if (account != null && account.isEnabled()) {

            try {
                LOGGER.log(Level.INFO, "Authenticating response");
                request.authenticate(response);
            } catch (IOException | ServletException e) {
                LOGGER.log(Level.WARNING, "Request.authenticate failed", e);
                return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
            }

            UserGroupMapping userGroupMapping = accountManager.getUserGroupMapping(login);

            if (authConfig.isSessionEnabled()) {
                session.setAttribute("login", login);
                session.setAttribute("groups", userGroupMapping.getGroupName());
            }

            AccountDTO accountDTO = mapper.map(account, AccountDTO.class);
            accountDTO.setAdmin(UserGroupMapping.ADMIN_ROLE_ID.equals(userGroupMapping.getGroupName()));
            Response.ResponseBuilder responseBuilder = Response.ok().entity(accountDTO);
            if (authConfig.isJwtEnabled()) {
                responseBuilder.header("jwt", tokenManager.createAuthToken(authConfig.getJWTKey(), userGroupMapping));
            }

            return responseBuilder.build();

        } else {
            if (authConfig.isSessionEnabled()) {
                session.invalidate();
            }
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @POST
    @Path("/recovery")
    @ApiOperation(value = "Send password recovery request", response = Response.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Successful request"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendPasswordRecovery(
            @ApiParam(required = true, value = "Password recovery request") PasswordRecoveryRequestDTO passwordRecoveryRequestDTO)
                    throws EntityNotFoundException {
        String login = passwordRecoveryRequestDTO.getLogin();
        Account account = accountManager.getAccount(login);

        if (oAuthManager.isProvidedAccount(account)) {
            return Response.status(403).build();
        }

        userManager.createPasswordRecoveryRequest(account);
        return Response.noContent().build();
    }

    @POST
    @Path("/recover")
    @ApiOperation(value = "Recover account password", response = Response.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Successful recover request"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendPasswordRecover(
            @ApiParam(required = true, value = "Password recovery process") PasswordRecoverDTO passwordRecoverDTO)
                    throws EntityNotFoundException {
        userManager.recoverPassword(passwordRecoverDTO.getUuid(), passwordRecoverDTO.getNewPassword());
        return Response.noContent().build();
    }

    @GET
    @Path("/logout")
    @ApiOperation(value = "Log out connected user", response = Response.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Successful logout"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request) {

        if (authConfig.isSessionEnabled()) {
            request.getSession().invalidate();
        }

        try {
            request.logout();
        } catch (ServletException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/providers")
    @ApiOperation(value = "Get registered OAuth providers", response = OAuthProviderPublicDTO.class, responseContainer = "List", authorizations = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of providers. It can be an empty list."),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProviders() {
        List<OAuthProvider> providers = oAuthManager.getProviders();
        List<OAuthProviderPublicDTO> dtos = new ArrayList<>();

        for (OAuthProvider provider : providers) {
            OAuthProviderPublicDTO oAuthProviderPublicDTO = mapper.map(provider, OAuthProviderPublicDTO.class);
            oAuthProviderPublicDTO.setSigningKeys(getSigningKeys(provider.getJwkSetURL()));
            dtos.add(oAuthProviderPublicDTO);
        }

        // need to get jwks signing keys (they can be changed)

        return Response.ok(new GenericEntity<List<OAuthProviderPublicDTO>>(dtos) {
        }).build();
    }

    @GET
    @Path("/providers/{id}")
    @ApiOperation(value = "Get OAuth provider details", response = OAuthProviderPublicDTO.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful retrieval of provider."),
            @ApiResponse(code = 404, message = "Provider not found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthProviderPublicDTO getProvider(
            @ApiParam(required = true, value = "Provider id") @PathParam("id") Integer providerId)
                    throws EntityNotFoundException {
        OAuthProvider provider = oAuthManager.getProvider(providerId);
        OAuthProviderPublicDTO oAuthProviderPublicDTO = mapper.map(provider, OAuthProviderPublicDTO.class);
        oAuthProviderPublicDTO.setSigningKeys(getSigningKeys(provider.getJwkSetURL()));
        return oAuthProviderPublicDTO;
    }

    private String getSigningKeys(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        JsonReader jsonReader = null;

        try {
            com.squareup.okhttp.Response response = client.newCall(request).execute();
            jsonReader = Json.createReader(new StringReader(response.body().string()));
            JsonObject object = jsonReader.readObject();
            JsonArray keys = object.getJsonArray("keys");
            return keys.toString();
        } catch (IOException | JsonParsingException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return null;
        } finally {
            if (jsonReader != null) {
                jsonReader.close();
            }
        }
    }

    @POST
    @Path("/oauth")
    @ApiOperation(value = "Try to authenticate with OAuth", response = AccountDTO.class, authorizations = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful login"),
            @ApiResponse(code = 403, message = "Unsuccessful login"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response oAuthLogin(@Context HttpServletRequest request, @Context HttpServletResponse response,
            @ApiParam(required = true, value = "OAuth login request") OAuthLoginRequestDTO oAuthLoginRequestDTO)
                    throws EntityNotFoundException {

        // Get provider configuration to fill settings
        Integer providerId = oAuthLoginRequestDTO.getProviderId();
        OAuthProvider provider = oAuthManager.getProvider(providerId);

        if (!provider.isEnabled()) {
            return Response.status(403).build();
        }

        IDTokenClaimsSet validate;
        Account account;

        // Validate the token
        try {
            validate = validateToken(provider, oAuthLoginRequestDTO);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Bad configuration for provider URIs", e);
            return Response.status(500).build();
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Bad configuration for provider algorithm", e);
            return Response.status(500).build();
        } catch (BadJOSEException | JOSEException e) {
            LOGGER.log(Level.SEVERE, "Something gone wrong with token decoding", e);
            return Response.status(403).build();
        }

        // Retrieve required fields in claims
        String sub = (String) validate.getClaim("sub");
        String email = (String) validate.getClaim("email");

        try {
            // find provided account with given provider and given sub
            // If exists, use this account
            ProvidedAccount providedAccount = oAuthManager.getProvidedAccount(providerId, sub);
            account = providedAccount.getAccount();
        } catch (ProvidedAccountNotFoundException e) {
            // If not : create an account.
            // get an available login
            // String login = findAvailableLogin(sub);
            String login = oAuthManager.findAvailableLogin(sub);

            try {
                account = accountManager.createAccount(login, sub, email, "en", UUID.randomUUID().toString(), "CET");
                oAuthManager.createProvidedAccount(account, sub, provider);
            } catch (CreationException | AccountAlreadyExistsException e1) {
                // should not happen as we check for login availability just before
                // we could re throw the exception, and let the exception mapper do the job
                // just for debug...
                LOGGER.log(Level.SEVERE, "Something gone wrong with automatic account set up", e);
                return Response.serverError().build();
            }

        }

        // send 200 with account details + plm access token
        Response.ResponseBuilder responseBuilder = Response.ok().entity(mapper.map(account, AccountDTO.class));
        UserGroupMapping userGroupMapping = AuthServices.getUserGroupMapping(account.getLogin());

        if (authConfig.isJwtEnabled()) {
            responseBuilder.header("jwt", tokenManager.createAuthToken(authConfig.getJWTKey(), userGroupMapping));
        }

        return responseBuilder.build();
    }

    private IDTokenClaimsSet validateToken(OAuthProvider provider, OAuthLoginRequestDTO oAuthLoginRequestDTO)
            throws MalformedURLException, ParseException, BadJOSEException, JOSEException {
        Issuer iss = new Issuer(provider.getIssuer());
        ClientID clientID = new ClientID(provider.getClientID());
        Nonce nonce = new Nonce(oAuthLoginRequestDTO.getNonce());
        URL jwkSetURL = new URL(provider.getJwkSetURL());
        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(provider.getJwsAlgorithm());
        IDTokenValidator validator = new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);
        JWT idToken = JWTParser.parse(oAuthLoginRequestDTO.getIdToken());
        return validator.validate(idToken, nonce);
    }

}
