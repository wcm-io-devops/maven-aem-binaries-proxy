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
package io.wcm.devops.maven.aembinariesproxy.health;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import io.wcm.devops.maven.aembinariesproxy.MavenProxyConfiguration;

/**
 * Checks if the AEM binaries root url is available via HTTP.
 */
public class AemBinariesDistHealthCheck extends HealthCheck {

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;

  private static final Logger log = LoggerFactory.getLogger(AemBinariesDistHealthCheck.class);

  /**
   * @param config maven proxy configuration
   * @param httpClient http client
   */
  public AemBinariesDistHealthCheck(MavenProxyConfiguration config, CloseableHttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  @Override
  protected Result check() throws Exception {
    String url = config.getAemBinariesRootUrl();

    log.info("Validate file: {}", url);
    HttpGet get = new HttpGet(url);
    HttpResponse response = httpClient.execute(get);
    try {
      if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
        return Result.healthy();
      }
      else {
        return Result.unhealthy("Got status code " + response.getStatusLine().getStatusCode()
            + " when accessing URL " + url);
      }
    }
    finally {
      EntityUtils.consumeQuietly(response.getEntity());
    }
  }

}
