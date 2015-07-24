package org.dasein.cloud.test.container;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.compute.container.Cluster;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.container.ContainerSupport;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.*;
import org.junit.rules.TestName;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * @author Stas Maksimov (stas.maksimov@software.dell.com)
 * @since 2015.09
 */
public class StatelessContainerTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessContainerTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }
    @Before
    public void before() {
        tm.begin(name.getMethodName());
        assumeTrue(!tm.isTestSkipped());
    }

    @After
    public void after() {
        tm.end();
    }

    @Rule
    public final TestName name = new TestName();

    @Test
    public void listClusters() throws CloudException, InternalException {
        ContainerSupport support = getContainerSupport();
        if( support == null ) { return; }

        Iterable<Cluster> clusters = support.listClusters();
        int count = 0;

        assertNotNull("The container clusters listing may not be null regardless of subscription level", clusters);

        for( Cluster cluster : clusters ) {
            count++;
            tm.out("Cluster", cluster);
        }
        tm.out("Total Cluster Count", count);
        if( count < 1 ) {
            if( !support.isSubscribed() ) {
                tm.ok("Not subscribed to container services, so no cluster exist");
            }
            else {
                tm.warn("No container clusters were returned so this test may be invalid");
            }
        }
        for( Cluster cluster : clusters ) {
            assertCluster(cluster);
        }
    }

    private void assertCluster(@Nonnull Cluster cluster) {
        assertNotNull("The cluster ID may not be null", cluster.getProviderClusterId());
        assertNotNull("The owner account may not be null", cluster.getProviderOwnerId());
        assertNotNull("The name may not be null", cluster.getName());
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

