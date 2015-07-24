package org.dasein.cloud.test.container;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.container.Cluster;
import org.dasein.cloud.compute.container.ContainerSupport;
import org.dasein.cloud.compute.container.Scheduler;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Stas Maksimov (stas.maksimov@software.dell.com)
 * @since 2015.09
 */
public class StatefulContainerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulContainerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testClusterId;

    public StatefulContainerTests() {
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());

        if( name.getMethodName().equals("removeCluster") ) {
            testClusterId = tm.getTestClusterId(DaseinTestManager.REMOVED, true);
        }
    }

    @After
    public void after() {
        try {
            testClusterId = null;
        }
        finally {
            tm.end();
        }
    }

    @Test
    public void createCluster() throws CloudException, InternalException {
        ContainerSupport support = getContainerSupport();
        if( support == null ) { return; }

        ContainerResources resources = DaseinTestManager.getContainerResources();
        assertNotNull("The tests failed to initialize a proper set of container services", resources);

        String clusterId = resources.provisionCluster("provision", "dsncluster");

        tm.out("New Cluster", clusterId);
        assertNotNull("The newly created cluster ID may not be null", clusterId);

        Cluster cluster = support.getCluster(clusterId);

        assertNotNull("Could not retrieve cluster by the reported ID " + clusterId, cluster);
        assertEquals("The IDs for the requested cluster and the created cluster do not match", clusterId, cluster.getProviderClusterId());
    }

    @Test
    public void removeCluster() throws CloudException, InternalException {
        ContainerSupport support = getContainerSupport();
        if( support == null ) { return; }

        if( testClusterId != null ) {
            Cluster cluster = support.getCluster(testClusterId);

            assertNotNull("The test cluster does not exist prior to running this test", cluster);
            tm.out("Before", cluster);

            support.removeCluster(testClusterId);

            cluster = support.getCluster(testClusterId);
            tm.out("After", cluster);
            assertNull("The test cluster still exists after the removal", cluster);
        }
        else {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to container support");
            }
            else {
                fail("No test cluster exists for running the test " + name.getMethodName());
            }
        }
    }

    @Test
    public void createScheduler() throws CloudException, InternalException {
        ContainerSupport support = getContainerSupport();
        if( support == null ) { return; }

        ContainerResources resources = DaseinTestManager.getContainerResources();
        assertNotNull("The tests failed to initialize a proper set of container services", resources);

        String schedulerId = resources.provisionScheduler("provision", "dsnsched");

        tm.out("New Scheduler", schedulerId);
        assertNotNull("The newly created scheduler ID may not be null", schedulerId);

        Scheduler scheduler = support.getScheduler(schedulerId);

        assertNotNull("Could not retrieve scheduler by the reported ID " + schedulerId, scheduler);
        assertEquals("The IDs for the requested scheduler and the created scheduler do not match", schedulerId, scheduler.getProviderSchedulerId());
    }

    private ContainerSupport getContainerSupport() {
        ComputeServices services = tm.getProvider().getComputeServices();
        if( services == null ) {
            tm.ok("Compute services are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
            return null;
        }

        ContainerSupport support = services.getContainerSupport();
        if( support == null ) {
            tm.ok("Containers are not supported in " + tm.getContext().getRegionId() + " of " + tm.getProvider().getCloudName());
        }
        return support;
    }

}
