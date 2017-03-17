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

import org.apache.commons.lang3.StringUtils;

import io.wcm.devops.maven.aembinariesproxy.MavenProxyConfiguration;

/**
 * Builds HTML index page
 */
public final class IndexPageBuilder {

  private static final String[] EXAMPLE_URLS = new String[] {
      "${groupIdPath}/${aemDispatcherArtifactId}/${aemDispatcherSampleVersion}/${aemDispatcherArtifactId}-${aemDispatcherSampleVersion}.pom",
      "${groupIdPath}/${aemDispatcherArtifactId}/${aemDispatcherSampleVersion}/${aemDispatcherArtifactId}-${aemDispatcherSampleVersion}-apache2.4-aix-powerpc.tar.gz",
      "${groupIdPath}/${aemDispatcherArtifactId}/${aemDispatcherSampleVersion}/${aemDispatcherArtifactId}-${aemDispatcherSampleVersion}-apache2.4-solaris-amd64-ssl.tar.gz",
      "${groupIdPath}/${aemDispatcherArtifactId}/${aemDispatcherSampleVersion}/${aemDispatcherArtifactId}-${aemDispatcherSampleVersion}-iis-windows-x86.zip",
      "${groupIdPath}/${aemDispatcherArtifactId}/${aemDispatcherSampleVersion}/${aemDispatcherArtifactId}-${aemDispatcherSampleVersion}-ns-linux-i686-ssl.tar.gz"
  };

  private IndexPageBuilder() {
    // static methods only
  }

  /**
   * Build HTML index page
   * @param config configuration for the proxy
   * @param version artifacts version
   * @return HTML index page
   */
  public static String build(MavenProxyConfiguration config, String version) {
    StringBuilder exampleUrlsMarkup = new StringBuilder();
    for (String exampleUrl : EXAMPLE_URLS) {
      String url = exampleUrl;
      url = StringUtils.replace(url, "${groupIdPath}", StringUtils.replace(config.getGroupId(), ".", "/"));
      url = StringUtils.replace(url, "${aemDispatcherArtifactId}", config.getAemDispatcherArtifactId());
      url = StringUtils.replace(url, "${aemDispatcherSampleVersion}", version);
      exampleUrlsMarkup.append("<li><a href=\"").append(url).append("\">").append(url).append("</a></li>");
    }

    String serviceVersion = IndexPageBuilder.class.getPackage().getImplementationVersion();

    return "<!DOCTYPE html>\n<html>"
        + "<head>"
        + "<title>Maven AEM Binaries Proxy</title>"
        + "<style>body { font-family: sans-serif; }</style>"
        + "</head>"
        + "<body>"
        + "<h1>Maven AEM Binaries Proxy</h1>"
        + "<p>This is a Maven Artifact Proxy for AEM binaries located at: "
        + "<a href=\"" + config.getAemBinariesRootUrl() + "\">" + config.getAemBinariesRootUrl() + "</a></p>"
        + "<p>Every call to this Maven repository is routed directly to the AEM distribution server.</p>"
        + "<p><strong>Please never use this Maven repository directly in your maven builds, use it only via a Repository Manager "
        + "which caches the resolved artifacts.</strong></p>"
        + "<p>If you want to setup your own proxy get the source code: "
        + "<a href=\"https://github.com/wcm-io-devops/maven-aem-binaries-proxy\">https://github.com/wcm-io-devops/maven-aem-binaries-proxy</a></p>"
        + "<hr/>"
        + "<p>Example artifacts:</p>"
        + "<ul>"
        + exampleUrlsMarkup
        + "</ul>"
        + "<p>For all files SHA1 checksums are supported (.sha1 suffix). MD5 checksums are not supported.</p>"
        + (serviceVersion != null ? "<hr/><p>Version " + IndexPageBuilder.class.getPackage().getImplementationVersion() + "</p>" : "")
        + "</body>"
        + "</html>";
  }

}
