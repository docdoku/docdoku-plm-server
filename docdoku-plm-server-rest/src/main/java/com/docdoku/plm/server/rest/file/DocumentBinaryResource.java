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
package com.docdoku.plm.server.rest.file;

import io.swagger.annotations.*;
import com.docdoku.plm.server.core.common.BinaryResource;
import com.docdoku.plm.server.core.document.DocumentIteration;
import com.docdoku.plm.server.core.document.DocumentIterationKey;
import com.docdoku.plm.server.core.document.DocumentRevision;
import com.docdoku.plm.server.core.document.DocumentRevisionKey;
import com.docdoku.plm.server.core.exceptions.*;
import com.docdoku.plm.server.core.exceptions.NotAllowedException;
import com.docdoku.plm.server.core.security.UserGroupMapping;
import com.docdoku.plm.server.core.services.*;
import com.docdoku.plm.server.core.sharing.SharedDocument;
import com.docdoku.plm.server.core.sharing.SharedEntity;
import com.docdoku.plm.server.core.util.HashUtils;
import com.docdoku.plm.server.config.AuthConfig;
import com.docdoku.plm.server.helpers.Streams;
import com.docdoku.plm.server.rest.exceptions.FileConversionException;
import com.docdoku.plm.server.rest.exceptions.PreconditionFailedException;
import com.docdoku.plm.server.rest.exceptions.RequestedRangeNotSatisfiableException;
import com.docdoku.plm.server.rest.file.util.BinaryResourceDownloadMeta;
import com.docdoku.plm.server.rest.file.util.BinaryResourceDownloadResponseBuilder;
import com.docdoku.plm.server.rest.file.util.BinaryResourceUpload;
import com.docdoku.plm.server.rest.interceptors.Compress;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@Api(hidden = true, value = "documentBinary", description = "Operations about document files")
@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.GUEST_ROLE_ID})
@RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID, UserGroupMapping.GUEST_ROLE_ID})
public class DocumentBinaryResource {

    private static final Logger LOGGER = Logger.getLogger(DocumentBinaryResource.class.getName());
    private static final String UTF8_ENCODING = "UTF-8";

    @Inject
    private IBinaryStorageManagerLocal storageManager;
    @Inject
    private IDocumentManagerLocal documentService;
    @Inject
    private IContextManagerLocal contextManager;
    @Inject
    private IOnDemandConverterManagerLocal onDemandConverterManager;
    @Inject
    private IShareManagerLocal shareService;
    @Inject
    private IPublicEntityManagerLocal publicEntityManager;
    @Inject
    private AuthConfig authConfig;
    @Inject
    private Locale userLocale;
    @Inject
    private ITokenManagerLocal tokenManager;

    public DocumentBinaryResource() {
    }

    @POST
    @ApiOperation(value = "Upload document file",
            response = Response.class,
            authorizations = {@Authorization(value = "authorization")})
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Upload success"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "upload", paramType = "formData", dataType = "file", required = true)
    })
    @Path("/{iteration}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    public Response uploadDocumentFiles(
            @Context HttpServletRequest request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") final String workspaceId,
            @ApiParam(required = true, value = "Document master id") @PathParam("documentId") final String documentId,
            @ApiParam(required = true, value = "Workspace version") @PathParam("version") final String version,
            @ApiParam(required = true, value = "Document iteration") @PathParam("iteration") final int iteration)
            throws EntityNotFoundException, EntityAlreadyExistsException, UserNotActiveException, AccessRightException,
            NotAllowedException, CreationException, WorkspaceNotEnabledException {

        try {
            String fileName = null;
            DocumentIterationKey docPK = new DocumentIterationKey(workspaceId, documentId, version, iteration);
            Collection<Part> formParts = request.getParts();

            for (Part formPart : formParts) {
                fileName = uploadAFile(formPart, docPK);
            }

            if (fileName == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            if (formParts.size() == 1) {
                return BinaryResourceUpload.tryToRespondCreated(request.getRequestURI() + URLEncoder.encode(fileName, "UTF-8"));
            }

            return Response.noContent().build();
        } catch (IOException | ServletException | StorageException e) {
            return BinaryResourceUpload.uploadError(e);
        }
    }

    @GET
    @ApiOperation(value = "Download document file",
            response = File.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Download success"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Path("/{iteration}/{fileName}")
    @Compress
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDocumentFile(
            @Context Request request,
            @ApiParam(required = true, value = "Workspace id") @PathParam("workspaceId") final String workspaceId,
            @ApiParam(required = true, value = "Document master id") @PathParam("documentId") final String documentId,
            @ApiParam(required = true, value = "Workspace version") @PathParam("version") final String version,
            @ApiParam(required = true, value = "Document iteration") @PathParam("iteration") final int iteration,
            @ApiParam(required = true, value = "File name") @PathParam("fileName") final String fileName,
            @ApiParam(required = false, value = "Type") @QueryParam("type") String type,
            @ApiParam(required = false, value = "Output") @QueryParam("output") String output,
            @ApiParam(required = false, value = "Range") @HeaderParam("Range") String range,
            @ApiParam(required = false, value = "Shared entity uuid") @QueryParam("uuid") final String uuid,
            @ApiParam(required = false, value = "Password for private resource") @HeaderParam("password") String password,
            @ApiParam(required = false, value = "Shared entity token") @QueryParam("token") String accessToken)
            throws EntityNotFoundException, UserNotActiveException, AccessRightException, NotAllowedException,
            PreconditionFailedException, RequestedRangeNotSatisfiableException, WorkspaceNotEnabledException {

        BinaryResource binaryResource;
        String decodedFileName = fileName;
        try {
            decodedFileName = URLDecoder.decode(fileName, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Cannot decode filename", e);
        }

        String fullName = workspaceId + "/documents/" + documentId + "/" + version + "/" + iteration + "/" + decodedFileName;

        boolean isWorkingCopy = false;

        if (uuid != null && !uuid.isEmpty()) {
            SharedEntity sharedEntity = shareService.findSharedEntityForGivenUUID(uuid);

            if (accessToken != null && !accessToken.isEmpty()) {

                String decodedUUID = tokenManager.validateSharedResourceToken(authConfig.getJWTKey(), accessToken);

                if (null == decodedUUID || !decodedUUID.equals(sharedEntity.getUuid())) {
                    throw new NotAllowedException("NotAllowedException73");
                }

            } else {
                // Check uuid & access right
                checkUuidValidity(sharedEntity, workspaceId, documentId, version, iteration, password);
            }

            binaryResource = publicEntityManager.getBinaryResourceForSharedEntity(fullName);

            // sharedEntity is always a SharedDocument

            if (sharedEntity instanceof SharedDocument) {

                SharedDocument document = (SharedDocument) sharedEntity;

                DocumentRevision documentRevision = document.getDocumentRevision();

                DocumentIteration workingIteration = documentRevision.getWorkingCopy();

                isWorkingCopy = documentRevision.getLastIteration().equals(workingIteration);

            }

        } else {
            // Check access right

            if (accessToken != null && !accessToken.isEmpty()) {
                String decodedEntityKey = tokenManager.validateEntityToken(authConfig.getJWTKey(), accessToken);
                boolean tokenValid = new DocumentRevisionKey(workspaceId, documentId, version).toString().equals(decodedEntityKey);
                if (!tokenValid) {
                    throw new NotAllowedException("NotAllowedException73");
                }
                binaryResource = publicEntityManager.getBinaryResourceForSharedEntity(fullName);
            } else {
                if (!canAccess(new DocumentIterationKey(workspaceId, documentId, version, iteration))) {
                    throw new NotAllowedException("NotAllowedException73");
                }
                binaryResource = getBinaryResource(fullName);
                DocumentRevision docRevision = documentService.getDocumentRevision(new DocumentRevisionKey(workspaceId, documentId, version));
                DocumentIteration workingIteration = docRevision.getWorkingCopy();
                if (workingIteration != null) {
                    isWorkingCopy = workingIteration.getIteration() == iteration;
                }
            }
        }

        BinaryResourceDownloadMeta binaryResourceDownloadMeta = new BinaryResourceDownloadMeta(binaryResource, output, type);

        // Check cache precondition
        Response.ResponseBuilder rb = request.evaluatePreconditions(binaryResourceDownloadMeta.getLastModified(), binaryResourceDownloadMeta.getETag());
        if (rb != null) {
            return rb.build();
        }

        InputStream binaryContentInputStream = null;

        boolean isToBeCached = !isWorkingCopy;

        try {

            if (output != null && !output.isEmpty()) {
                binaryContentInputStream = getConvertedBinaryResource(binaryResource, output, userLocale);
                if (range == null || range.isEmpty()) {
                    binaryResourceDownloadMeta.setLength(0);
                }
            } else {
                binaryContentInputStream = storageManager.getBinaryResourceInputStream(binaryResource);
            }

            return BinaryResourceDownloadResponseBuilder.prepareResponse(binaryContentInputStream, binaryResourceDownloadMeta, range, isToBeCached);

        } catch (StorageException | FileConversionException e) {
            Streams.close(binaryContentInputStream);
            return BinaryResourceDownloadResponseBuilder.downloadError(e, fullName);
        }

    }

    private String uploadAFile(Part formPart, DocumentIterationKey docPK)
            throws EntityNotFoundException, EntityAlreadyExistsException, AccessRightException, NotAllowedException, CreationException, UserNotActiveException, StorageException, IOException, WorkspaceNotEnabledException {

        String fileName = Normalizer.normalize(formPart.getSubmittedFileName(), Normalizer.Form.NFC);
        // Init the binary resource with a null length
        BinaryResource binaryResource = documentService.saveFileInDocument(docPK, fileName, 0);
        OutputStream outputStream = storageManager.getBinaryResourceOutputStream(binaryResource);
        long length = BinaryResourceUpload.uploadBinary(outputStream, formPart);
        documentService.saveFileInDocument(docPK, fileName, length);
        return fileName;
    }

    /**
     * Try to convert a binary resource to a specific format
     *
     * @param binaryResource The binary resource
     * @param outputFormat   The wanted output
     * @return The binary resource stream in the wanted output
     * @throws FileConversionException
     */
    private InputStream getConvertedBinaryResource(BinaryResource binaryResource, String outputFormat, Locale locale) throws FileConversionException {
        try {
            return onDemandConverterManager.getDocumentConvertedResource(outputFormat, binaryResource, locale);
        } catch (Exception e) {
            throw new FileConversionException(e);
        }
    }

    private boolean canAccess(DocumentIterationKey docIKey) throws AccessRightException, NotAllowedException, WorkspaceNotFoundException, UserNotFoundException, DocumentRevisionNotFoundException, UserNotActiveException, WorkspaceNotEnabledException {
        DocumentRevision publicDocumentRevision = publicEntityManager.getPublicDocumentRevision(docIKey.getDocumentRevision());
        return publicDocumentRevision != null || contextManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID) && documentService.canAccess(docIKey);

    }

    private BinaryResource getBinaryResource(String fullName)
            throws NotAllowedException, AccessRightException, UserNotActiveException, EntityNotFoundException, WorkspaceNotEnabledException {
        BinaryResource binaryResource = publicEntityManager.getPublicBinaryResourceForDocument(fullName);
        if (binaryResource != null) {
            return binaryResource;
        } else {
            return documentService.getBinaryResource(fullName);
        }
    }

    private void checkUuidValidity(SharedEntity sharedEntity, String workspaceId, String documentId, String version, int iteration, String password)
            throws NotAllowedException {
        if (!(sharedEntity instanceof SharedDocument)) {
            throw new NotAllowedException("NotAllowedException73");
        }

        checkUuidExpiredDate(sharedEntity);
        checkUuidPassword(sharedEntity, password);

        String shareEntityWorkspaceId = sharedEntity.getWorkspace().getId();
        DocumentRevision documentRevision = ((SharedDocument) sharedEntity).getDocumentRevision();
        DocumentIteration lastCheckedInIteration = documentRevision.getLastCheckedInIteration();
        if (!shareEntityWorkspaceId.equals(workspaceId) ||
                !documentRevision.getDocumentMasterId().equals(documentId) ||
                !documentRevision.getVersion().equals(version) ||
                (null != lastCheckedInIteration && lastCheckedInIteration.getIteration() < iteration)) {
            throw new NotAllowedException("NotAllowedException73");
        }
    }

    private void checkUuidPassword(SharedEntity sharedEntity, String password) throws NotAllowedException {
        String entityPassword = sharedEntity.getPassword();
        if (entityPassword != null && !entityPassword.isEmpty()) {
            try {
                if (password == null || password.isEmpty() || !entityPassword.equals(HashUtils.md5Sum(password))) {
                    throw new NotAllowedException("NotAllowedException73");
                }
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new NotAllowedException("NotAllowedException73");
            }
        }
    }

    private void checkUuidExpiredDate(SharedEntity sharedEntity) throws NotAllowedException {
        // Check shared entity expired
        if (sharedEntity.getExpireDate() != null && sharedEntity.getExpireDate().getTime() < new Date().getTime()) {
            shareService.deleteSharedEntityIfExpired(sharedEntity);
            throw new NotAllowedException("NotAllowedException73");
        }
    }

}
