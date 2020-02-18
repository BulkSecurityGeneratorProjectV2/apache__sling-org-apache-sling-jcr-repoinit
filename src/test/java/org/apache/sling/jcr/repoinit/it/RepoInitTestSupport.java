/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.repoinit.it;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.CompositeOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;
import static org.apache.sling.testing.paxexam.SlingOptions.versionResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;

public abstract class RepoInitTestSupport extends TestSupport {

    protected Session session;

    protected static final Logger log = LoggerFactory.getLogger(RepoInitTestSupport.class.getName());

    final String repositoriesURL = "https://repo1.maven.org/maven2@id=central";

    @Inject
    private SlingRepository repository;

    @Configuration
    public Option[] configuration() {
        SlingOptions.versionResolver.setVersionFromProject("org.apache.jackrabbit", "jackrabbit-api");
        final String localRepo = System.getProperty("maven.repo.local", "");
        final Option[] options = 
        remove(new Option[] {
            vmOption(System.getProperty("pax.vm.options")),
            baseConfiguration(),
            slingQuickstart(),
            testBundle("bundle.filename"),
            keepCaches(),
            systemProperty("org.ops4j.pax.url.mvn.repositories").value(repositoriesURL),
            when(localRepo.length() > 0).useOptions(
                systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
            ),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.repoinit.parser").versionAsInProject(),
            junitBundles(),
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                .asOption()
            },
        // remove our bundle under test to avoid duplication
        mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.jcr.repoinit").version(versionResolver)
        );
        final Option[] allOptions = ArrayUtils.addAll(options, additionalOptions());
        return allOptions;
    }

    protected Option[] additionalOptions() {
        return new Option[] {};
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory();
        final int httpPort = findFreePort();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort)
        );
    }

    public String getTestFileUrl(String path) {
        return getClass().getResource(path).toExternalForm();
    }

    @Before
    public void setupSession() throws Exception {
        if(session == null) {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        }
    }

    @After
    public void cleanupSession() {
        if(session != null) {
            session.logout();
        }
    }

    // TODO should come from org.apache.sling.testing.paxexam
    private static List<Option> expand(final Option[] options) {
        final List<Option> expanded = new ArrayList<>();
        if (options != null) {
            for (final Option option : options) {
                if (option != null) {
                    if (option instanceof CompositeOption) {
                        expanded.addAll(Arrays.asList(((CompositeOption) option).getOptions()));
                    } else {
                        expanded.add(option);
                    }
                }
            }
        }
        return expanded;
    }

    // TODO should come from org.apache.sling.testing.paxexam
    private static Option[] remove(final Option[] options, final Option... removables) {
        final List<Option> expanded = expand(options);
        for (final Option removable : removables) {
            if (removable instanceof CompositeOption) {
                expanded.removeAll(Arrays.asList(((CompositeOption) removable).getOptions()));
            } else {
                expanded.removeAll(Collections.singleton(removable));
            }
        }
        return expanded.toArray(new Option[0]);
    }

}
