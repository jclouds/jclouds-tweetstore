/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.demo.tweetstore.integration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Closeables.close;
import static java.lang.String.format;
import static org.jclouds.demo.tweetstore.integration.util.Zips.zipDir;
import static org.jclouds.http.Uris.uriBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

/**
 * Basic &quot;server facade&quot; functionality to deploy a WAR to Cloud Foundry.
 * 
 * @author Andrew Phillips
 */
public class CloudFoundryServer {
    private static final String CLOUD_FOUNDRY_APPLICATION_URL_SUFFIX = ".cfapps.io";
    private static final int MAX_STATUS_CHECKS = 20;
    
    protected CloudFoundryClient client;
    protected String appName;
    
    public void writePropertiesAndStartServer(final String address, final String warfile, 
            String target, String username, String password, Properties props) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String propsfile = String.format("%1$s/WEB-INF/jclouds.properties", warfile);
        System.err.println("file: " + propsfile);
        storeProperties(propsfile, props);
        assert new File(propsfile).exists();

        CloudCredentials credentials = new CloudCredentials(username, password);
        client = new CloudFoundryClient(credentials,  uriBuilder(target).build().toURL());
        client.login();
        appName = getAppName(address);
        deploy(warfile);
        waitForInstance(appName);
        client.logout();
    }

    private void deploy(String explodedWar) throws IOException {
        File war = zipDir(explodedWar, format("%s-cloudfoundry.war", explodedWar));
        client.uploadApplication(appName, war); 
        // adapted from https://github.com/cloudfoundry/cf-java-client/blob/master/cloudfoundry-maven-plugin/src/main/java/org/cloudfoundry/maven/AbstractPush.java
        AppState appState = client.getApplication(appName).getState();
        switch (appState) {
        case STOPPED:
            client.startApplication(appName);
            break;
        case STARTED:
            client.restartApplication(appName);
            break;
        default:
            throw new IllegalStateException(format("Unexpected application state '%s'", appState));
        }
    }

    private void waitForInstance(String appName) throws TimeoutException, InterruptedException {
        int statusChecks = 0;
        CloudApplication application;
        while (statusChecks < MAX_STATUS_CHECKS) {
           application = client.getApplication(appName);
           if (application.getRunningInstances() == application.getInstances()) {
              return;
           }
           statusChecks++;
           TimeUnit.SECONDS.sleep(1);
        }
        // should have returned before we get here if the app started
        throw new TimeoutException(format("Application '%s' not started after %d checks", appName, MAX_STATUS_CHECKS));   
    }

    private static void storeProperties(String filename, Properties props)
            throws IOException {
        FileOutputStream targetFile = new FileOutputStream(filename);
        try {
            props.store(targetFile, "test");
        } finally {
            close(targetFile, true);
        }
    }

    private static String getAppName(String applicationUrl) {
        checkArgument(applicationUrl.endsWith(CLOUD_FOUNDRY_APPLICATION_URL_SUFFIX), 
                "Application URL '%s' does not end in '%s'", applicationUrl, 
                CLOUD_FOUNDRY_APPLICATION_URL_SUFFIX);
        
        return applicationUrl.substring(0, 
                applicationUrl.length() - CLOUD_FOUNDRY_APPLICATION_URL_SUFFIX.length());
    }

    public void stop() throws Exception {
        checkState(client != null, "'stop' called before 'writePropertiesAndStartServer'");
        client.login();
        client.stopApplication(appName);
        client.logout();
    }
}