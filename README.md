<img src="http://wcm.io/images/favicon-16@2x.png"/> Maven AEM Binaries Proxy
======
[![Build Status](https://travis-ci.org/wcm-io-devops/maven-aem-binaries-proxy.png?branch=develop)](https://travis-ci.org/wcm-io-devops/maven-aem-binaries-proxy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm.devops.maven/io.wcm.devops.maven.aem-binaries-proxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm.devops.maven/io.wcm.devops.maven.aem-binaries-proxy)

Maven proxy to download AEM binaries (that are available in the public) as Maven artifacts.

Currently this proxy only supports AEM dispatcher binaries that are available at https://www.adobeaemcloud.com/content/companies/public/adobe/dispatcher/dispatcher.html


Steps to build and start the proxy:

- Go to maven-aem-binaries-proxy directory
- Build server with `mvn clean install`
- Start server with<br/>
`java -jar target/io.wcm.devops.maven.aem-binaries-proxy-<version>.jar server config.yml`
- Go to [http://localhost:8080](http://localhost:8080) for further instructions
