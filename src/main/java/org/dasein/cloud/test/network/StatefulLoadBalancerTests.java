/**
 * Copyright (C) 2009-2013 enstratius, Inc.
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

package org.dasein.cloud.test.network;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.network.LbEndpointType;
import org.dasein.cloud.network.LoadBalancer;
import org.dasein.cloud.network.LoadBalancerEndpoint;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.cloud.test.compute.ComputeResources;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Implements test cases against stateful load balancer functions.
 * <p>Created by George Reese: 3/8/13 4:34 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatefulLoadBalancerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulLoadBalancerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testDataCenterId;
    private String testLoadBalancerId;
    private String testVirtualMachineId;

    public StatefulLoadBalancerTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
        if( name.getMethodName().equals("removeLoadBalancer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.REMOVED, true);
        }
        else if( name.getMethodName().equals("addIP") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
        }
        else if( name.getMethodName().equals("removeIP") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
            NetworkServices services = tm.getProvider().getNetworkServices();

            if( services != null ) {
                LoadBalancerSupport support = services.getLoadBalancerSupport();

                if( support != null ) {
                    try {
                        support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
            }
        }
        else if( name.getMethodName().equals("addServer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
            LoadBalancer lb = null;

            NetworkServices net = tm.getProvider().getNetworkServices();

            try {
                if( net != null ) {
                    LoadBalancerSupport support = net.getLoadBalancerSupport();

                    if( support != null ) {
                        lb = support.getLoadBalancer(testLoadBalancerId);
                    }
                }
                if( lb != null ) {
                    testDataCenterId = null;
                    String[] ids = lb.getProviderDataCenterIds();

                    if( ids.length > 0 ) {
                        testDataCenterId = ids[ids.length-1];
                    }
                    ComputeResources c = DaseinTestManager.getComputeResources();

                    if( c != null ) {
                        ComputeServices services = tm.getProvider().getComputeServices();

                        if( services != null ) {
                            VirtualMachineSupport support = services.getVirtualMachineSupport();

                            if( support != null ) {
                                testVirtualMachineId = c.provisionVM(support, DaseinTestManager.STATEFUL + "3", "dsnlbvm", "dsnlb", testDataCenterId);
                            }
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        else if( name.getMethodName().equals("addDataCenter") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
            if( testLoadBalancerId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    LoadBalancerSupport support = services.getLoadBalancerSupport();

                    if( support != null ) {
                        try {
                            if( support.isDataCenterLimited() ) {
                                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                                if( lb != null ) {
                                    ArrayList<DataCenter> regionDataCenters = new ArrayList<DataCenter>();
                                    String[] dcs = lb.getProviderDataCenterIds();

                                    regionDataCenters.addAll(tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()));

                                    if( dcs.length >= regionDataCenters.size() ) {
                                        support.removeDataCenters(testLoadBalancerId, dcs[0]);
                                        testDataCenterId = dcs[0];
                                    }
                                    else {
                                        for( DataCenter dc : regionDataCenters ) {
                                            boolean included = false;

                                            for( String id : dcs ) {
                                                if( id.equals(dc.getProviderDataCenterId()) ) {
                                                    included = true;
                                                    break;
                                                }
                                            }
                                            if( included ) {
                                                testDataCenterId = dc.getProviderDataCenterId();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }

                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeDataCenter") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
            if( testLoadBalancerId != null ) {
                NetworkServices services = tm.getProvider().getNetworkServices();

                if( services != null ) {
                    LoadBalancerSupport support = services.getLoadBalancerSupport();

                    if( support != null ) {
                        try {
                            if( support.isDataCenterLimited() ) {
                                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                                if( lb != null ) {
                                    String[] dcs = lb.getProviderDataCenterIds();

                                    if( dcs.length < 1 ) {
                                        testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
                                        support.addDataCenters(testLoadBalancerId, testDataCenterId);
                                    }
                                    else {
                                        testDataCenterId = dcs[0];
                                    }
                                }
                            }
                            else {
                                testDataCenterId = tm.getProvider().getDataCenterServices().listDataCenters(tm.getContext().getRegionId()).iterator().next().getProviderDataCenterId();
                            }
                        }
                        catch( Throwable ignore ) {
                            // ignore
                        }

                    }
                }
            }
        }
        else if( name.getMethodName().equals("removeServer") ) {
            testLoadBalancerId = tm.getTestLoadBalancerId(DaseinTestManager.STATEFUL, true);
            NetworkServices net = tm.getProvider().getNetworkServices();

            try {
                if( net != null ) {
                    LoadBalancerSupport support = net.getLoadBalancerSupport();

                    if( support != null ) {
                        ArrayList<String> ids = new ArrayList<String>();

                        for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                            if( endpoint.getEndpointType().equals(LbEndpointType.VM) ) {
                                ids.add(endpoint.getEndpointValue());
                            }
                        }
                        if( ids.size() > 0 ) {
                            testVirtualMachineId = ids.iterator().next();
                        }
                        else {
                            ComputeResources c = DaseinTestManager.getComputeResources();

                            if( c != null ) {
                                ComputeServices services = tm.getProvider().getComputeServices();

                                if( services != null ) {
                                    VirtualMachineSupport vmSupport = services.getVirtualMachineSupport();

                                    if( vmSupport != null ) {
                                        testVirtualMachineId = c.provisionVM(vmSupport, DaseinTestManager.STATEFUL, "dsnlbvm", "dsnlb", testDataCenterId);
                                        if( testVirtualMachineId != null ) {
                                            support.addServers(testLoadBalancerId, testVirtualMachineId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
    }

    @After
    public void after() {
        try {
            testLoadBalancerId = null;
            testDataCenterId = null;
            testVirtualMachineId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createLoadBalancer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        NetworkResources network = DaseinTestManager.getNetworkResources();

        if( network == null ) {
            fail("Failed to initialize network capabilities for tests");
        }
        String id = network.provisionLoadBalancer("provision", "dsncrlbtest");

        tm.out("New Load Balancer", id);
        assertNotNull("The newly created load balancer ID was null", id);

        LoadBalancer lb = support.getLoadBalancer(id);

        assertNotNull("The newly created load balancer is not null", lb);
    }

    @Test
    public void addDataCenter() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null && testDataCenterId != null ) {
            if( support.isDataCenterLimited() ) {
                support.addDataCenters(testLoadBalancerId, testDataCenterId);

                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer no longer exists", lb);
                boolean ok = false;

                for( String dc : lb.getProviderDataCenterIds() ) {
                    if( dc.equals(testDataCenterId) ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new data center among the load balancer data centers", ok);
            }
            else {
                try {
                    support.addDataCenters(testLoadBalancerId, testDataCenterId);
                    fail("Should not be able to add data centers in a cloud that is not data center limited");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud is not data center limited");
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void addIP() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean ips = false;

        for( LbEndpointType t :support.listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.IP) ) {
                ips = true;
                break;
            }
        }
        if( testLoadBalancerId != null ) {
            if( support.supportsAddingEndpoints() && ips ) {
                support.addIPEndpoints(testLoadBalancerId, "196.91.70.2");

                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.IP) && endpoint.getEndpointValue().equals("196.91.70.2") ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new IP address among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.addIPEndpoints(testLoadBalancerId, "196.91.70.2");
                    fail("Should not be able to add endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding IP addresses post-create");
                }
            }
        }
        else {
            if( !ips ) {
                tm.ok("Load balancers in this cloud do not support IP endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void addServer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean vms = false;

        for( LbEndpointType t :support.listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.VM) ) {
                vms = true;
                break;
            }
        }
        if( testLoadBalancerId != null && testVirtualMachineId != null ) {
            if( support.supportsAddingEndpoints() && vms ) {
                support.addServers(testLoadBalancerId, testVirtualMachineId);

                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer no longer exists", lb);
                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.VM) && endpoint.getEndpointValue().equals(testVirtualMachineId) ) {
                        ok = true;
                    }
                }
                assertTrue("Failed to find the new server among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.addServers(testLoadBalancerId, testVirtualMachineId);
                    fail("Should not be able to add endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding virtual machines post-create");
                }
            }
        }
        else {
            if( !vms ) {
                tm.ok("Load balancers in this cloud do not support virtual machine endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeDataCenter() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null && testDataCenterId != null ) {
            if( support.isDataCenterLimited() ) {
                support.removeDataCenters(testLoadBalancerId, testDataCenterId);

                LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

                assertNotNull("The test load balancer no longer exists", lb);
                boolean ok = false;

                for( String dc : lb.getProviderDataCenterIds() ) {
                    if( dc.equals(testDataCenterId) ) {
                        ok = true;
                    }
                }
                assertFalse("The test data center is still present among the load balancer data centers", ok);
            }
            else {
                try {
                    support.removeDataCenters(testLoadBalancerId, testDataCenterId);
                    fail("Should not be able to remove data centers in a cloud that is not data center limited");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud is not data center limited");
                }
            }
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeIP() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean ips = false;

        for( LbEndpointType t :support.listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.IP) ) {
                ips = true;
                break;
            }
        }
        if( testLoadBalancerId != null ) {
            if( support.supportsAddingEndpoints() && ips ) {
                support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");

                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.IP) && endpoint.getEndpointValue().equals("197.41.20.2") ) {
                        ok = true;
                    }
                }
                assertFalse("The test IP endpoint still exists among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.removeIPEndpoints(testLoadBalancerId, "197.41.20.2");
                    fail("Should not be able to remove endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support removing IP addresses post-create");
                }
            }
        }
        else {
            if( !ips ) {
                tm.ok("Load balancers in this cloud do not support IP endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeServer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        boolean vms = false;

        for( LbEndpointType t :support.listSupportedEndpointTypes() ) {
            if( t.equals(LbEndpointType.VM) ) {
                vms = true;
                break;
            }
        }
        if( testLoadBalancerId != null && testVirtualMachineId != null ) {
            if( support.supportsAddingEndpoints() && vms ) {
                support.removeServers(testLoadBalancerId, testVirtualMachineId);

                boolean ok = false;

                for( LoadBalancerEndpoint endpoint : support.listEndpoints(testLoadBalancerId) ) {
                    if( endpoint.getEndpointType().equals(LbEndpointType.VM) && endpoint.getEndpointValue().equals(testVirtualMachineId) ) {
                        ok = true;
                    }
                }
                assertFalse("The test virtual machine is still present among the load balancer endpoints", ok);
            }
            else {
                try {
                    support.removeServers(testLoadBalancerId, testVirtualMachineId);
                    fail("Should not be able to remove endpoints in this cloud, but the operation completed");
                }
                catch( OperationNotSupportedException expected ) {
                    tm.ok("Caught OperationNotSupportedException because this cloud does not support adding/removing virtual machines post-create");
                }
            }
        }
        else {
            if( !vms ) {
                tm.ok("Load balancers in this cloud do not support virtual machine endpoints");
            }
            else if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }

    @Test
    public void removeLoadBalancer() throws CloudException, InternalException {
        NetworkServices services = tm.getProvider().getNetworkServices();

        if( services == null ) {
            tm.ok("Network services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        LoadBalancerSupport support = services.getLoadBalancerSupport();

        if( support == null ) {
            tm.ok("Load balancers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return;
        }
        if( testLoadBalancerId != null ) {
            LoadBalancer lb = support.getLoadBalancer(testLoadBalancerId);

            tm.out("Before", lb);
            assertNotNull("The load balancer is null prior to the test", lb);
            support.removeLoadBalancer(testLoadBalancerId);
            lb = support.getLoadBalancer(testLoadBalancerId);

            tm.out("After", lb);
            assertNull("The load balancer still exists after the test", lb);
        }
        else {
            if( support.isSubscribed() ) {
                fail("No test load balancer for " + name.getMethodName());
            }
            else {
                tm.ok("Load balancer support is not subscribed so this test is not entirely valid");
            }
        }
    }
}
