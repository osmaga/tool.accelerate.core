/*******************************************************************************
 * Copyright (c) 2016,2017 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ibm.liberty.starter.api.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;

import javax.validation.ValidationException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import com.ibm.liberty.starter.ProjectConstructionInput;
import com.ibm.liberty.starter.ProjectConstructionInputData;
import com.ibm.liberty.starter.ProjectConstructor;
import com.ibm.liberty.starter.ServiceConnector;
import com.ibm.liberty.starter.ZipWriter;
import com.ibm.liberty.starter.exception.ProjectGenerationException;

@Path("v1/data")
public class DownloadProjectEndpoint {

    private static final Logger log = Logger.getLogger(DownloadProjectEndpoint.class.getName());

    @GET
    @Produces("application/zip")
    public Response getResponse(@QueryParam("tech") String[] techs, @QueryParam("techoptions") String[] techOptions, @QueryParam("name") String name,
                                @QueryParam("deploy") final String deploy, @QueryParam("workspace") final String workspaceId, @QueryParam("build") final String build, 
                                @QueryParam("artifactId") String artifactId, @QueryParam("groupId") String groupId, @QueryParam("generationId") String generationId,
                                @QueryParam("beta") boolean beta, @Context UriInfo info) throws NullPointerException, IOException {
        log.info("GET request for /data");
        try {
            ProjectConstructionInput inputProcessor = new ProjectConstructionInput(new ServiceConnector(info.getBaseUri()));
            final ProjectConstructionInputData inputData = inputProcessor.processInput(techs, techOptions, name, deploy, workspaceId, build, artifactId, groupId, generationId, beta, true);
            ProjectConstructor projectConstructor = new ProjectConstructor(inputData);
            Map<String, byte[]> fileMap = projectConstructor.buildFileMap();
            ZipWriter zipConstructor = new ZipWriter(fileMap);
            StreamingOutput so = (OutputStream os) -> {
                zipConstructor.buildZip(os);
            };
            name += ".zip";
            return Response.ok(so, "application/zip").header("Content-Disposition", "attachment; filename=\"" + name + "\"").build();
        } catch (ValidationException e) {
            return Response.status(Status.BAD_REQUEST).entity("Validation of the input failed.").build();
        } catch (ProjectGenerationException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.severe(e.getClass().getName() + " caught " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
