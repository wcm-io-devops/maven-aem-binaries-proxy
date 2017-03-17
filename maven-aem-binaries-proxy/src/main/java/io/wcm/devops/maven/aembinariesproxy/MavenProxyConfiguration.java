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
package io.wcm.devops.maven.aembinariesproxy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;

/**
 * Configuration for Maven AEM Binaries Proxy.
 */
@SuppressWarnings("javadoc")
public class MavenProxyConfiguration extends Configuration {

  @NotEmpty
  private String groupId;
  @NotEmpty
  private String aemDispatcherArtifactId;
  @NotEmpty
  private String aemBinariesRootUrl;
  @NotEmpty
  private String aemBinariesUrlParts;

  @Valid
  @NotNull
  private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  @JsonProperty
  public String getGroupId() {
    return this.groupId;
  }

  @JsonProperty
  public String getAemDispatcherArtifactId() {
    return this.aemDispatcherArtifactId;
  }

  @JsonProperty
  public String getAemBinariesRootUrl() {
    return this.aemBinariesRootUrl;
  }

  @JsonProperty
  public String getAemBinariesUrlParts() {
    return this.aemBinariesUrlParts;
  }

  @JsonProperty("httpClient")
  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

}
