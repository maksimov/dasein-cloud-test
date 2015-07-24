/**
 * Copyright (C) 2009-2015 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.test.container;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.container.Cluster;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.container.ContainerSupport;
import org.dasein.cloud.compute.container.SchedulerCreateOptions;
import org.dasein.cloud.test.DaseinTestManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Manages all identity resources for automated provisioning and de-provisioning during integration tests.
 * <p>Created by George Reese: 2/18/13 10:16 AM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class ContainerResources {
    static private final Logger logger = Logger.getLogger(ContainerResources.class);

    static private final Random random = new Random();

    private final HashMap<String,String> testClusters = new HashMap<String, String>();
    private final HashMap<String,String> testSchedulers = new HashMap<String, String>();
    private CloudProvider   provider;
    private ContainerSupport containerSupport;

    public ContainerResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
        ComputeServices computeServices = provider.getComputeServices();
        if( computeServices != null ) {
            containerSupport = computeServices.getContainerSupport();
        }
    }

    public @Nullable ContainerSupport getContainerSupport() {
        return containerSupport;
    }

    public int close() {
        int count = 0;

        try {
            if( getContainerSupport() != null ) {
                for (Map.Entry<String, String> entry : testClusters.entrySet()) {
                    if (!entry.getKey().equals(DaseinTestManager.STATELESS)) {
                        try {
                            getContainerSupport().removeCluster(entry.getValue());
                            count++;
                        } catch (Throwable t) {
                            logger.warn("Failed to de-provision test container cluster " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
                for (Map.Entry<String, String> entry : testSchedulers.entrySet()) {
                    if (!entry.getKey().equals(DaseinTestManager.STATELESS)) {
                        try {
                            getContainerSupport().removeScheduler(entry.getValue());
                            count++;
                        } catch (Throwable t) {
                            logger.warn("Failed to de-provision test container scheduler " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        return count;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testClusters.remove(DaseinTestManager.STATELESS);
        if( !testClusters.isEmpty() ) {
            logger.info("Provisioned Container Resources:");
            header = true;
            count += testClusters.size();
            DaseinTestManager.out(logger, null, "---> Clusters", testClusters.size() + " " + testClusters);
        }
        testSchedulers.remove(DaseinTestManager.STATELESS);
        if( !testSchedulers.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned Container Resources:");
                header = true;
            }
            count += testSchedulers.size();
            DaseinTestManager.out(logger, null, "---> Schedulers", testSchedulers.size() + " " + testSchedulers);
        }
        return count;
    }

    public @Nullable String getTestClusterId(@Nonnull String label, boolean provisionIfNull) {
        if( label.equals(DaseinTestManager.STATELESS) ) {
            for( Map.Entry<String,String> entry : testClusters.entrySet() ) {
                if( !entry.getKey().equals(DaseinTestManager.REMOVED) ) {
                    String id = entry.getValue();

                    if( id != null ) {
                        return id;
                    }
                }
            }
            return findStatelessCluster();
        }
        String id = testClusters.get(label);

        if( id != null ) {
            return id;
        }
        if( provisionIfNull ) {
            try {
                return provisionCluster(label, "dsncluster");
            }
            catch(Throwable ignore) {
            }
        }
        return null;
    }

    public @Nullable String findStatelessCluster() {
        try {
            if( containerSupport != null && containerSupport.isSubscribed() ) {
                Iterator<Cluster> clusterIterator = containerSupport.listClusters().iterator();

                if( clusterIterator.hasNext() ) {
                    String id = clusterIterator.next().getProviderClusterId();
                    testClusters.put(DaseinTestManager.STATELESS, id);
                    return id;
                }
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        return null;
    }

    public @Nonnull String provisionCluster(@Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {
        String id = containerSupport.createCluster(namePrefix + " " + System.currentTimeMillis());

        if( id == null ) {
            throw new CloudException("No container cluster was created");
        }
        synchronized( testClusters ) {
            while( testClusters.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testClusters.put(label, id);
        }
        return id;
    }

    public @Nonnull String provisionScheduler(@Nonnull String label, @Nonnull String namePrefix) throws CloudException, InternalException {

        String id = containerSupport.createScheduler(SchedulerCreateOptions.getInstance(namePrefix + " " + System.currentTimeMillis()));

        if( id == null ) {
            throw new CloudException("No container scheduler was created");
        }
        synchronized( testSchedulers ) {
            while( testSchedulers.containsKey(label) ) {
                label = label + random.nextInt(9);
            }
            testSchedulers.put(label, id);
        }
        return id;
    }

}
