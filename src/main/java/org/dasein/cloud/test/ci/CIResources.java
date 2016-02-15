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

package org.dasein.cloud.test.ci;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ci.CIProvisionOptions;
import org.dasein.cloud.ci.CIServices;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancer;
import org.dasein.cloud.ci.ConvergedHttpLoadBalancerSupport;
import org.dasein.cloud.ci.ConvergedInfrastructure;
import org.dasein.cloud.ci.ConvergedInfrastructureState;
import org.dasein.cloud.ci.ConvergedInfrastructureSupport;
import org.dasein.cloud.ci.Topology;
import org.dasein.cloud.ci.TopologyProvisionOptions;
import org.dasein.cloud.ci.TopologyState;
import org.dasein.cloud.ci.TopologySupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;
import org.dasein.cloud.test.network.NetworkResources;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 6/3/13 4:50 PM</p>
 *
 * @author George Reese
 */
public class CIResources {
    static private final Logger logger = Logger.getLogger(CIResources.class);
    static private final Random random = new Random();

    private CloudProvider   provider;

    private final HashMap<String,String> testInfrastructures = new HashMap<String, String>();
    private final HashMap<String,String> testTopologies      = new HashMap<String, String>();
    private final HashMap<String,String> testHttpLoadBalancers      = new HashMap<String, String>();

    public CIResources(@Nonnull CloudProvider provider) {
        this.provider = provider;
    }

    public int close() {
        CIServices ciServices = provider.getCIServices();
        int count = 0;

        if( ciServices != null ) {
            ConvergedHttpLoadBalancerSupport hlbSupport = ciServices.getConvergedHttpLoadBalancerSupport();

            if( hlbSupport != null ) {
                List<String> hlbIds = new ArrayList<String>();
                for( Map.Entry<String,String> entry : testHttpLoadBalancers.entrySet() ) {
                    if ( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            ConvergedHttpLoadBalancer hlb = hlbSupport.getConvergedHttpLoadBalancer(entry.getValue());

                            if( hlb != null ) {
                                hlbSupport.removeConvergedHttpLoadBalancers(entry.getValue());
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test ConvergedHttpLoadBlancer " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }

            ConvergedInfrastructureSupport ciSupport = ciServices.getConvergedInfrastructureSupport();

            if( ciSupport != null ) {
                for( Map.Entry<String,String> entry : testInfrastructures.entrySet() ) {
                    if( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        try {
                            ConvergedInfrastructure ci = ciSupport.getConvergedInfrastructure(entry.getValue());

                            if( ci != null ) {
                                ciSupport.terminate(entry.getValue(), null);
                                count++;
                            }
                            else {
                                count++;
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Failed to de-provision test CI " + entry.getValue() + ": " + t.getMessage());
                        }
                    }
                }
            }

            TopologySupport tSupport = ciServices.getTopologySupport();

            if( tSupport != null ) {
                List<String> topologyIds = new ArrayList<String>();
                for( Map.Entry<String,String> entry : testTopologies.entrySet() ) {
                    if ( !entry.getKey().equals(DaseinTestManager.STATELESS) ) {
                        topologyIds.add(entry.getValue());
                        count++;
                    }
                }
                try {
                    tSupport.removeTopologies(topologyIds.toArray(new String[topologyIds.size()]));
                }
                catch( Throwable e ) {
                    logger.warn("Failed to de-provision test topology " + e.getMessage());
                }
            }
        }
        return count;
    }

    public @Nullable String getTestTopologyId(@Nonnull String label, boolean provisionIfNull) {
        String id = testTopologies.get(label);
        if (id == null) {
            if ( label.equals(DaseinTestManager.STATELESS) ) {
                for (Map.Entry<String, String> entry : testTopologies.entrySet()) {
                    if ( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                        id = entry.getValue();

                        if ( id != null ) {
                            return id;
                        }
                    }
                }
                id = findStatelessTopology();
            }
        }


        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        CIServices services = provider.getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            if( support != null ) {
                try {
                    NetworkResources networkResources = DaseinTestManager.getNetworkResources();
                    String testNetworkId = networkResources.getTestVLANId(DaseinTestManager.STATELESS, false, null);
                    ComputeResources computeResources = DaseinTestManager.getComputeResources();
                    String testImageId = computeResources.getTestImageId(DaseinTestManager.STATELESS, false);
                    String testProductId = computeResources.getTestVMProductId();

                    TopologyProvisionOptions withTopologyOptions = TopologyProvisionOptions.getInstance("dsn-topology"+String.valueOf(random.nextInt(10000)), "description", testProductId, true);

                    withTopologyOptions = withTopologyOptions.withAutomaticRestart(false);
                    withTopologyOptions = withTopologyOptions.withMaintenanceOption(TopologyProvisionOptions.MaintenanceOption.TERMINATE_VM_INSTANCE);

                    withTopologyOptions = withTopologyOptions.withNetworkInterface(testNetworkId, null, true); // ,accessConfigs);
                    withTopologyOptions = withTopologyOptions.withAttachedDisk("dsn-topology-disk"+String.valueOf(random.nextInt(1000)), TopologyProvisionOptions.DiskType.STANDARD_PERSISTENT_DISK, testImageId, true, true);
                    boolean result = support.createTopology(withTopologyOptions);
                    if (result) {
                        id = withTopologyOptions.getProductName();
                        testTopologies.put(DaseinTestManager.STATEFUL, id);
                        return id;
                    }
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    public @Nullable String getTestConvergedHttpLoadBalancerId(@Nonnull String label, boolean provisionIfNull) {
        String id = testHttpLoadBalancers.get(label);
        if (id == null) {
            if ( label.equals(DaseinTestManager.STATELESS) ) {
                for (Map.Entry<String, String> entry : testHttpLoadBalancers.entrySet()) {
                    if ( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                        id = entry.getValue();

                        if ( id != null ) {
                            return id;
                        }
                    }
                }
                id = findStatelessConvergedHttpLoadBalancer();
            }
        }


        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        CIServices services = provider.getCIServices();

        if( services != null ) {
            ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();
            ConvergedInfrastructureSupport ciSupport = services.getConvergedInfrastructureSupport();

            if( support != null ) {
                try {
                    String ciId = getTestCIId(DaseinTestManager.STATELESS, true);
                    ConvergedInfrastructure ci = ciSupport.getConvergedInfrastructure(ciId);
                    String ciSource = ci.getProviderConvergedInfrastructureId();
                    //horrible hack to try keep tests generic but work for google
                    if (provider.getCloudName().equals("GCE")) {
                        ciSource = ci.getTag("instanceGroupLink").toString();
                    }
                    Map<String, String> pathMap = new HashMap<String, String>();
                    String defaultBackend = "test-backend-1"+random.nextInt(1000);
                    pathMap.put("/*", defaultBackend);
                    String healthCheck1 = "test-health-check"+random.nextInt(1000);
                    String targetProxy1 = "target-proxy-"+random.nextInt(1000);
                    ConvergedHttpLoadBalancer withExperimentalConvergedHttpLoadbalancerOptions = ConvergedHttpLoadBalancer
                            .getInstance("test-http-load-balancer" + random.nextInt(1000), "test-http-load-balancer-description", defaultBackend)
                            .withHealthCheck(healthCheck1, healthCheck1 + "-description", null, 80, "/", 5, 5, 2, 2) //ONLY ONE ALLOWED
                            .withBackendService(defaultBackend, defaultBackend + "-description", 80, "http", "HTTP", new String[]{healthCheck1}, new String[]{ciSource}, 30)
                            .withUrlSet("url-map-1", "url-map-description", "*", pathMap)
                            .withTargetHttpProxy(targetProxy1, targetProxy1 + "-description")
                            .withForwardingRule(targetProxy1 + "-fr", targetProxy1 + "-fr-description", null, "TCP", "80", targetProxy1);

                    id = support.createConvergedHttpLoadBalancer(withExperimentalConvergedHttpLoadbalancerOptions);
                    if (id != null) {
                        if (!label.equals(DaseinTestManager.REMOVED)) {
                            testHttpLoadBalancers.put(DaseinTestManager.STATEFUL, id);
                        }
                        return id;
                    }
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    private @Nullable String findStatelessTopology() {
        CIServices services = provider.getCIServices();

        if( services != null ) {
            TopologySupport support = services.getTopologySupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    Topology defaultTopology = null;

                    for( Topology t : support.listTopologies(null) ) {
                        if( t.getCurrentState().equals(TopologyState.ACTIVE) ) {
                            defaultTopology = t;
                            break;
                        }
                        if( defaultTopology == null ) {
                            defaultTopology = t;
                        }
                    }
                    if( defaultTopology != null ) {
                        String id = defaultTopology.getProviderTopologyId();

                        testTopologies.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    private @Nullable String findStatelessCI() {
        CIServices services = provider.getCIServices();

        if( services != null ) {
            ConvergedInfrastructureSupport support = services.getConvergedInfrastructureSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    ConvergedInfrastructure defaultCI = null;

                    for( ConvergedInfrastructure ci : support.listConvergedInfrastructures(null) ) {
                        if( ci.getCurrentState().equals(ConvergedInfrastructureState.RUNNING) ) {
                            defaultCI = ci;
                            break;
                        }
                        if( defaultCI == null ) {
                            defaultCI = ci;
                        }
                    }
                    if( defaultCI != null ) {
                        String id = defaultCI.getProviderConvergedInfrastructureId();

                        testInfrastructures.put(DaseinTestManager.STATELESS, id);
                        return id;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    private @Nullable String findStatelessConvergedHttpLoadBalancer() {
        CIServices services = provider.getCIServices();

        if( services != null ) {
            ConvergedHttpLoadBalancerSupport support = services.getConvergedHttpLoadBalancerSupport();

            try {
                if( support != null && support.isSubscribed() ) {
                    String defaultHLB = null;

                    for( String hlb : support.listConvergedHttpLoadBalancers() ) {
                        if( defaultHLB == null ) {
                            defaultHLB = hlb;
                        }
                    }
                    if( defaultHLB != null ) {
                        testHttpLoadBalancers.put(DaseinTestManager.STATELESS, defaultHLB);
                        return defaultHLB;
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return null;
    }

    public @Nullable String getTestCIId(@Nonnull String label, boolean provisionIfNull) {
        String id = testInfrastructures.get(label);
        if (id == null) {
            if ( label.equals(DaseinTestManager.STATELESS) ) {
                for (Map.Entry<String, String> entry : testInfrastructures.entrySet()) {
                    if ( !entry.getKey().startsWith(DaseinTestManager.REMOVED) ) {
                        id = entry.getValue();

                        if ( id != null ) {
                            return id;
                        }
                    }
                }
                id = findStatelessCI();
            }
        }


        if( id != null ) {
            return id;
        }
        if( !provisionIfNull ) {
            return null;
        }
        CIServices services = provider.getCIServices();

        if( services != null ) {
            ConvergedInfrastructureSupport support = services.getConvergedInfrastructureSupport();

            if( support != null ) {
                try {
                    String testTopologyId = getTestTopologyId(DaseinTestManager.STATELESS, true);
                    String testDataCenterId = DaseinTestManager.getDefaultDataCenterId(true);
                    CIProvisionOptions options = CIProvisionOptions.getInstance("dsn-ci", "test-description", testDataCenterId, 1, testTopologyId);
                    ConvergedInfrastructure ci = support.provision(options);
                    if (ci != null) {
                        id = ci.getName();
                        testInfrastructures.put(DaseinTestManager.STATEFUL, id);
                        return id;
                    }
                }
                catch( Throwable ignore ) {
                    return null;
                }
            }
        }
        return null;
    }

    public int report() {
        boolean header = false;
        int count = 0;

        testInfrastructures.remove(DaseinTestManager.STATELESS);
        if( !testInfrastructures.isEmpty() ) {
            logger.info("Provisioned CI Resources:");
            header = true;
            count += testInfrastructures.size();
            DaseinTestManager.out(logger, null, "---> Infrastructures", testInfrastructures.size() + " " + testInfrastructures);
        }
        testTopologies.remove(DaseinTestManager.STATELESS);
        if( !testTopologies.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned CI Resources:");
                header = true;
            }
            count += testTopologies.size();
            DaseinTestManager.out(logger, null, "---> Topologies", testTopologies.size() + " " + testTopologies);
        }
        testHttpLoadBalancers.remove(DaseinTestManager.STATELESS);
        if( !testHttpLoadBalancers.isEmpty() ) {
            if( !header ) {
                logger.info("Provisioned CI Resources:");
            }
            count += testHttpLoadBalancers.size();
            DaseinTestManager.out(logger, null, "---> ConvergedHttpLoadBalancers", testHttpLoadBalancers.size() + " " + testHttpLoadBalancers);
        }
        return count;
    }
}
