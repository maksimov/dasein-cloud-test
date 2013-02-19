package org.dasein.cloud.test.cloud;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.test.DaseinTestManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;

/**
 * General authentication tests to verify the ability of a Dasein Cloud implementation to authenticate with the cloud
 * provider.
 * <p>Created by George Reese: 2/18/13 6:35 PM</p>
 * @author George Reese
 * @version 2013.04 initial version
 * @since 2013.04
 */
public class StatelessAuthenticationTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatelessAuthenticationTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private CloudProvider provider;

    public StatelessAuthenticationTests() { }

    @Before
    public void before() {
        tm.begin(name.getMethodName());

        provider = DaseinTestManager.constructProvider();
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new RuntimeException("This is not possible");
        }
        if( name.getMethodName().equals("invalidPassword") ) {
            ctx.setAccessPrivate("ThisCannotPossiblyBeASecretKey".getBytes());
            provider.connect(ctx);
        }
        else if( name.getMethodName().equals("invalidAccount") ) {
            ctx.setAccountNumber("MyWibblesAreTribbles");
            ctx.setAccessPublic("MyWibblesAreTribbles".getBytes());
            provider.connect(ctx);
        }
        else if( name.getMethodName().equals("reconnect") ) {
            provider.connect(ctx);

            String id = provider.testContext();

            ctx.setAccountNumber(id);
        }
    }

    @After
    public void after() {
        tm.end();
    }

    @Test
    public void authenticate() throws CloudException, InternalException {
        String id = tm.getProvider().testContext();

        tm.out("Account" + id);
        assertNotNull("Connection test failed", id);
    }

    @Test
    public void reconnect() throws CloudException, InternalException {
        String id = provider.testContext();

        //noinspection ConstantConditions
        assertEquals("New account number fails connection", id, provider.getContext().getAccountNumber());
    }

    @Test
    public void invalidPassword() throws CloudException, InternalException {
        assertNull("Connection succeeded with bad API secret", provider.testContext());
    }

    @Test
    public void invalidAccount() throws CloudException, InternalException {
        assertNull("Connection succeeded with fake account", provider.testContext());
    }
}
