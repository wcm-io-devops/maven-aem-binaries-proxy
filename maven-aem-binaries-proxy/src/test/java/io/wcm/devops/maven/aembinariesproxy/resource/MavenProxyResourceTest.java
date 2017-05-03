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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;

import io.dropwizard.testing.junit.ResourceTestRule;

public class MavenProxyResourceTest {

  // test with the following dispatcher versions
  private static final String[] DISPATCHER_VERSIONS = {
      "4.2.2",
      "LATEST"
  };
  private static final String[] DISPATCHER_QUALIFIERS = {
      "apache2.4-aix-powerpc.tar.gz",
      "apache2.4-solaris-amd64-ssl.tar.gz",
      "iis-windows-x86.zip",
      "ns-solaris-amd64.tar.gz"
  };

  @Rule
  public ResourceTestRule context = new ResourceTestRule.Builder()
  .addResource(new MavenProxyResource(TestContext.getConfiguration(), TestContext.getHttpClient()))
  .build();

  @Test
  public void testGetIndex() {
    String path = "/";
    Response response = context.client().target(path).request().get();
    assertResponse(path, response, MediaType.TEXT_HTML);
  }

  @Test
  public void testGetPomAemBinary() {
    for (String version : DISPATCHER_VERSIONS) {
      String path = "/com/adobe/dispatcher/" + version + "/dispatcher-" + version + ".pom";
      Response response = context.client().target(path).request().get();
      assertResponse(path, response, MediaType.APPLICATION_XML);
      assertTrue("Content length " + path, response.getLength() > 0);
      assertSHA1(path, response);
    }
  }

  @Test
  public void testGetBinaryAemBinary() {
    for (String version : DISPATCHER_VERSIONS) {
      for (String target : DISPATCHER_QUALIFIERS) {
        String path = "/com/adobe/dispatcher/" + version + "/dispatcher-" + version + "-" + target;
        Response response = context.client().target(path).request().get();
        assertResponse(path, response, MediaType.APPLICATION_OCTET_STREAM);
        assertSHA1(path, response);
      }
    }
  }

  private void assertResponse(String path, Response response, String mediaType) {
    System.out.println("Integration test: " + path);
    assertEquals("HTTP status " + path, HttpStatus.SC_OK, response.getStatus());
    assertEquals("Media type " + path, mediaType, response.getMediaType().toString());
    assertTrue(response.hasEntity());
  }

  private void assertSHA1(String path, Response dataResponse) {
    String sha1Path = path + ".sha1";
    Response sha1Response = context.client().target(sha1Path).request().get();
    assertResponse(sha1Path, sha1Response, MediaType.TEXT_PLAIN);

    try (InputStream is = dataResponse.readEntity(InputStream.class)) {
      byte[] data = IOUtils.toByteArray(is);
      String sha1 = sha1Response.readEntity(String.class);
      assertEquals("SHA-1 " + path, sha1, DigestUtils.sha1Hex(data));
    }
    catch (IOException ex) {
      throw new RuntimeException("Error checking SHA-1 of " + path, ex);
    }
  }

}
