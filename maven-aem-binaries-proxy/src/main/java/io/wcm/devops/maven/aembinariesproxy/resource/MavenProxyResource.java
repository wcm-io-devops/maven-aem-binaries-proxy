/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.devops.maven.aembinariesproxy.resource;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;

import io.wcm.devops.maven.aembinariesproxy.MavenProxyConfiguration;

/**
 * Proxies AEM binaries.
 */
@Path("/")
public class MavenProxyResource {

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;
  private final UrlsCache urlsCache;
  private final CurrentVersionCache currentVersionCache;

  private static final Logger log = LoggerFactory.getLogger(MavenProxyResource.class);

  private static final String SHA1_EXTENSION = ".sha1";
  private static final String POM = "pom";

  private static final String VERSION_LATEST = "LATEST";

  /**
   * @param config Configuration
   * @param httpClient http client
   */
  public MavenProxyResource(MavenProxyConfiguration config, CloseableHttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
    this.urlsCache = new UrlsCache(httpClient, config);
    this.currentVersionCache = new CurrentVersionCache(httpClient, config);
  }

  /**
   * Shows index page
   * @return HTML index page
   */
  @GET
  @Timed
  @Produces(MediaType.TEXT_HTML)
  public String getIndex() {
    return IndexPageBuilder.build(config, currentVersionCache.get());
  }

  /**
   * Maps POM requests simulating a Maven 2 directory structure.
   * @param groupIdPath groupId as path
   * @param artifactId artifactId
   * @param version version
   * @param artifactIdFilename artifactIdFilename
   * @param versionFilename versionFilename
   * @param fileExtension fileExtension
   * @return response response
   * @throws IOException
   */
  @GET
  @Path("{groupIdPath:[a-zA-Z0-9\\-\\_]+(/[a-zA-Z0-9\\-\\_]+)*}"
      + "/{artifactId:[a-zA-Z0-9\\-\\_\\.]+}"
      + "/{version:\\d+(\\.\\d+)*|LATEST}"
      + "/{artifactIdFilename:[a-zA-Z0-9\\-\\_\\.]+}"
      + "-{versionFilename:\\d+(\\.\\d+)*|LATEST}"
      + ".{fileExtension:pom(\\.sha1)?}")
  @Timed
  public Response getPom(
      @PathParam("groupIdPath") String groupIdPath,
      @PathParam("artifactId") String artifactId,
      @PathParam("version") String version,
      @PathParam("artifactIdFilename") String artifactIdFilename,
      @PathParam("versionFilename") String versionFilename,
      @PathParam("fileExtension") String fileExtension) throws IOException {

    String groupId = mapGroupId(groupIdPath);
    if (StringUtils.equalsIgnoreCase(version, VERSION_LATEST)) {
      version = currentVersionCache.get();
      versionFilename = version;
    }
    if (!validateBasicParams(groupId, artifactId, version, artifactIdFilename, versionFilename)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    String xml = PomBuilder.build(groupId, artifactId, version, POM);

    if (StringUtils.equals(fileExtension, POM)) {
      return Response.ok(xml)
          .type(MediaType.APPLICATION_XML)
          .build();
    }
    if (StringUtils.equals(fileExtension, POM + SHA1_EXTENSION)) {
      return Response.ok(DigestUtils.sha1Hex(xml))
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  /**
   * Maps all requests to AEM binaries simulating a Maven 2 directory structure.
   * @param groupIdPath groupId as path
   * @param artifactId artifactId
   * @param version version
   * @param artifactIdFilename artifactIdFilename
   * @param versionFilename versionFilename
   * @param classifierString classifier string
   * @param type type
   * @return response response
   * @throws IOException
   */
  @GET
  @Path("{groupIdPath:[a-zA-Z0-9\\-\\_]+(/[a-zA-Z0-9\\-\\_]+)*}"
      + "/{artifactId:[a-zA-Z0-9\\-\\_\\.]+}"
      + "/{version:\\d+(\\.\\d+)*|LATEST}"
      + "/{artifactIdFilename:[a-zA-Z0-9\\-\\_\\.]+}"
      + "-{versionFilename:\\d+(\\.\\d+)*|LATEST}"
      + "-{classifierString:[a-zA-Z]+[0-9\\.]*[a-zA-Z0-9\\-]+}"
      + ".{type:[a-z]+(\\.[a-z]+)*(\\.sha1)?}")
  @Timed
  public Response getBinary(
      @PathParam("groupIdPath") String groupIdPath,
      @PathParam("artifactId") String artifactId,
      @PathParam("version") String version,
      @PathParam("artifactIdFilename") String artifactIdFilename,
      @PathParam("versionFilename") String versionFilename,
      @PathParam("classifierString") String classifierString,
      @PathParam("type") String type) throws IOException {

    String groupId = mapGroupId(groupIdPath);
    if (StringUtils.equalsIgnoreCase(version, VERSION_LATEST)) {
      version = currentVersionCache.get();
      versionFilename = version;
    }
    if (!validateBasicParams(groupId, artifactId, version, artifactIdFilename, versionFilename)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    boolean getChecksum = false;
    if (StringUtils.endsWith(type, SHA1_EXTENSION)) {
      getChecksum = true;
    }

    String url = buildBinaryUrl(version, classifierString, StringUtils.removeEnd(type, SHA1_EXTENSION));
    return getBinary(url, version, getChecksum);
  }

  private Response getBinary(String url, String version, boolean getChecksum) throws IOException {
    log.info("Proxy file: {}", url);
    HttpGet get = new HttpGet(url);
    HttpResponse response = httpClient.execute(get);
    if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
      byte[] data = EntityUtils.toByteArray(response.getEntity());

      if (getChecksum) {
        return Response.ok(DigestUtils.sha1Hex(data))
            .type(MediaType.TEXT_PLAIN)
            .build();
      }
      else {
        return Response.ok(data)
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .header(CONTENT_LENGTH, response.containsHeader(CONTENT_LENGTH) ? response.getFirstHeader(CONTENT_LENGTH).getValue() : null)
            .build();
      }
    }
    else {
      EntityUtils.consumeQuietly(response.getEntity());
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }


  private String mapGroupId(String groupIdPath) {
    return StringUtils.replace(groupIdPath, "/", ".");
  }

  /**
   * Validate that groupId/artifactId are correct and version is consistent within the path.
   */
  private boolean validateBasicParams(
      String groupId,
      String artifactId,
      String version,
      String artifactIdFilename,
      String versionFilename) {
    if (!StringUtils.equals(artifactId, artifactIdFilename)) {
      return false;
    }
    if (!StringUtils.equals(version, versionFilename)) {
      return false;
    }
    if (!StringUtils.equals(groupId, config.getGroupId())) {
      return false;
    }
    if (!(StringUtils.equals(artifactId, config.getAemDispatcherArtifactId()))) {
      return false;
    }
    if (!validateVersion(version)) {
      return false;
    }
    return true;
  }

  private String buildBinaryUrl(String version, String classifierString, String type) throws IOException {

    String aemBinariesRootUrl = config.getAemBinariesRootUrl();
    String artifactKey = config.getAemDispatcherArtifactId() + "-" + classifierString + "-" + version + "." + type;
    String urlPathPart = urlsCache.get(artifactKey);
    URL url = new URL(aemBinariesRootUrl);
    String urlProtocolHost = StringUtils.substringBefore(aemBinariesRootUrl, url.getPath());
    return urlProtocolHost + urlPathPart;
  }

  private boolean validateVersion(String version) {
    String versionFromRootUrl = currentVersionCache.get();
    if (StringUtils.equals(version, versionFromRootUrl)) {
      return true;
    }
    else {
      return false;
    }
  }

}
