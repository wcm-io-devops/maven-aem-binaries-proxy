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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * Cache for the urls of the artifacts
 */
public class UrlsCache {

  /**
   * @param httpClient http client
   * @param config maven proxy configuration
   */
  public UrlsCache(CloseableHttpClient httpClient, MavenProxyConfiguration config) {
    this.httpClient = httpClient;
    this.config = config;
  }

  private static final Logger log = LoggerFactory.getLogger(UrlsCache.class);

  private final MavenProxyConfiguration config;
  private final CloseableHttpClient httpClient;

  private Map<String, String> urls = new ConcurrentHashMap<String, String>();

  private static final Pattern PATTERN_HREFS = Pattern.compile("href=\"(.*?)\"");

  private long updateDate = System.currentTimeMillis();

  private final long CACHE_VALIDITY = 1000l * 60 * 60 * 24;

  void putAll(Map<String, String> urlsFromIndex) {
    urls.putAll(urlsFromIndex);
  }

  String get(String key) {
    if (urls.isEmpty() || (System.currentTimeMillis() - updateDate > CACHE_VALIDITY)) {
      fillCache();
    }
    return urls.get(key);
  }

  void fillCache() {
    urls.clear();
    Matcher m1 = PATTERN_HREFS.matcher(getAemBinariesRootPage());
    while (m1.find()) {
      String href = m1.group(1);
      String artifactPart = StringUtils.substringAfterLast(href, "/");
      urls.put(artifactPart, href);
    }
  }

  private String getAemBinariesRootPage() {
    String pageHtml = StringUtils.EMPTY;
    HttpGet get = new HttpGet(config.getAemBinariesRootUrl());
    HttpResponse response;
    try {
      response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK) {
        pageHtml = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
      }
    }
    catch (IOException ex) {
      log.error("Error getting getAemBinariesRootPage: " + config.getAemBinariesRootUrl(), ex);
    }
    return pageHtml;
  }

}
