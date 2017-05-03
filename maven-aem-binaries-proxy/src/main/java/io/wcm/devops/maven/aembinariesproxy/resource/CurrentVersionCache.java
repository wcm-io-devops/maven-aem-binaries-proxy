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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.devops.maven.aembinariesproxy.MavenProxyConfiguration;

/**
 * Cache for the current versions of the artifacts
 */
public class CurrentVersionCache {
  /**
   * @param httpClient http client
   * @param config maven proxy configuration
   */
  public CurrentVersionCache(CloseableHttpClient httpClient, MavenProxyConfiguration config) {
    this.httpClient = httpClient;
    this.config = config;
  }

  private static final Logger log = LoggerFactory.getLogger(UrlsCache.class);

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;

  private static final long CACHE_VALIDITY = 1000L * 60 * 60;

  private static final Pattern PATTERN_VERSION_ELEMENT = Pattern.compile("<li>Version[:\\s\\d.]+</li>");
  private static final Pattern PATTERN_VERSION_STRING = Pattern.compile("\\d\\.\\d\\.\\d");

  private String currentVersion = StringUtils.EMPTY;
  private long updateDate = System.currentTimeMillis();

  /**
   * Gets current version from cache or from server if expired
   * @return current version
   */
  String get() {
    if (currentVersion.isEmpty() || (System.currentTimeMillis() - updateDate > CACHE_VALIDITY)) {
      currentVersion = getLatestVersionFromRootUrl();
    }
    return currentVersion;
  }

  private String getLatestVersionFromRootUrl() {
    String version = StringUtils.EMPTY;
    HttpGet get = new HttpGet(config.getAemBinariesRootUrl());
    HttpResponse response;
    try {
      response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
        String pageHtml = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
        Matcher m1 = PATTERN_VERSION_ELEMENT.matcher(pageHtml);
        if (m1.find()) {
          String versionTag = m1.group(0);
          Matcher m2 = PATTERN_VERSION_STRING.matcher(versionTag);
          if (m2.find()) {
            version = m2.group(0);
          }
        }
      }
    }
    catch (IOException ex) {
      log.error("Error getting latest version from aemBinariesRootPage: " + config.getAemBinariesRootUrl(), ex);
    }
    return version;
  }
}
