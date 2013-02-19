package org.dasein.cloud.test.compute;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VMFilterOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.test.DaseinTestManager;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.Assert.*;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 2/17/13 7:46 PM</p>
 *
 * @author George Reese
 */
public class StatefulVMTests {
    static private DaseinTestManager tm;

    @BeforeClass
    static public void configure() {
        tm = new DaseinTestManager(StatefulVMTests.class);
    }

    @AfterClass
    static public void cleanUp() {
        if( tm != null ) {
            tm.close();
        }
    }

    @Rule
    public final TestName name = new TestName();

    private String testVmId = null;

    public StatefulVMTests() { }

    private @Nullable VirtualMachine awaitState(@Nonnull VirtualMachine vm, @Nonnull VmState targetState, @Nonnegative long timeout) {
        VmState currentState = vm.getCurrentState();
        VirtualMachine v = vm;

        while( System.currentTimeMillis() < timeout ) {
            if( targetState.equals(currentState) ) {
                return v;
            }
            try { Thread.sleep(15000L); }
            catch( InterruptedException ignore ) { }
            try {
                //noinspection ConstantConditions
                v = tm.getProvider().getComputeServices().getVirtualMachineSupport().getVirtualMachine(vm.getProviderVirtualMachineId());
                if( v == null && !targetState.equals(VmState.TERMINATED) ) {
                    return null;
                }
                else if( v == null ) {
                    return null;
                }
                currentState = v.getCurrentState();
            }
            catch( Throwable ignore ) {
                // ignore
            }
        }
        return v;
    }

    @Before
    public void before() {
        tm.begin(name.getMethodName());
        if( name.getMethodName().equals("filterVMs") ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        //noinspection ConstantConditions
                        testVmId = DaseinTestManager.getComputeResources().provisionVM(support, "Dasein Filter Test", "dsnfilter", null);
                    }
                    catch( Throwable t ) {
                        tm.warn("Failed to provision VM for filter test: " + t.getMessage());
                    }
                }
            }
        }
        else if( name.getMethodName().equals("terminate") ) {
            ComputeServices services = tm.getProvider().getComputeServices();

            if( services != null ) {
                VirtualMachineSupport support = services.getVirtualMachineSupport();

                if( support != null ) {
                    try {
                        //noinspection ConstantConditions
                        testVmId = DaseinTestManager.getComputeResources().provisionVM(support, "Dasein Termination Test", "dsnterm", null);
                    }
                    catch( Throwable t ) {
                        tm.warn("Failed to provision VM for termination test: " + t.getMessage());
                    }
                }
            }
        }
        else if( name.getMethodName().equals("start") ) {
                testVmId = tm.getTestVMId(false, VmState.STOPPED);
        }
        else if( name.getMethodName().equals("stop") ) {
            testVmId = tm.getTestVMId(false, VmState.RUNNING);
        }
        else if( name.getMethodName().equals("pause") ) {
            testVmId = tm.getTestVMId(false, VmState.RUNNING);
        }
        else if( name.getMethodName().equals("unpause") ) {
            testVmId = tm.getTestVMId(false, VmState.PAUSED);
        }
        else if( name.getMethodName().equals("suspend") ) {
            testVmId = tm.getTestVMId(false, VmState.RUNNING);
        }
        else if( name.getMethodName().equals("resume") ) {
            testVmId = tm.getTestVMId(false, VmState.SUSPENDED);
        }
        else {
            testVmId = tm.getTestVMId(false, null);
        }
    }

    @After
    public void after() {
        testVmId = null;
        tm.end();
    }

    @Test
    public void disableAnalytics() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    support.disableAnalytics(testVmId);
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void enableAnalytics() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    support.enableAnalytics(testVmId);
                }
                else {
                    tm.warn("No test virtual machine was found for testing enabling analytics");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void launch() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( support.isSubscribed() ) {
                    @SuppressWarnings("ConstantConditions") String id = DaseinTestManager.getComputeResources().provisionVM(support, "Dasein Test Launch", "dsnlaunch", null);

                    tm.out("Launched", id);
                    assertNotNull("Attempts to provisionVM a virtual machine MUST return a valid ID", id);
                    assertNotNull("Could not find the newly created virtual machine", support.getVirtualMachine(id));
                }
                else {
                    try {
                        //noinspection ConstantConditions
                        DaseinTestManager.getComputeResources().provisionVM(support, "Should Fail", "failure", null);
                        fail("Attempt to launch VM should not succeed when the account is not subscribed to virtual machine services");
                    }
                    catch( CloudException ok ) {
                        tm.ok("Got exception when not subscribed: " + ok.getMessage());
                    }
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void filterVMs() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                Iterable<VirtualMachine> vms = support.listVirtualMachines(VMFilterOptions.getInstance(".*[Ff][Ii][Ll][Tt][Ee][Rr].*"));
                boolean found = false;
                int count = 0;

                assertNotNull("Filtering must return at least an empty collections and may not be null", vms);
                for( VirtualMachine vm : vms ) {
                    count++;
                    if( vm.getProviderVirtualMachineId().equals(testVmId) ) {
                        found = true;
                    }
                    tm.out("VM", vm);
                }
                tm.out("Total VM Count", count);
                if( count < 1 && support.isSubscribed() ) {
                    if( testVmId == null ) {
                        tm.warn("No virtual machines were listed and thus the test may be in error");
                    }
                    else {
                        fail("Should have found test virtual machine " + testVmId + ", but none were found");
                    }
                }
                if( testVmId != null ) {
                    assertTrue("Did not find the test filter VM " + testVmId + " among the filtered VMs", found);
                }
                else {
                    tm.warn("No test VM existed for filter test, so results may not be valid");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void stop() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsStartStop(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.stop(testVmId);
                            vm = awaitState(vm, VmState.STOPPED, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.STOPPED, currentState);
                        }
                        else {
                            try {
                                support.stop(testVmId);
                                fail("Start/stop is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("STOP -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void start() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsStartStop(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.start(testVmId);
                            vm = awaitState(vm, VmState.RUNNING, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.RUNNING, currentState);
                        }
                        else {
                            try {
                                support.start(testVmId);
                                fail("Start/stop is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("START -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void pause() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsPauseUnpause(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.pause(testVmId);
                            vm = awaitState(vm, VmState.PAUSED, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.PAUSED, currentState);
                        }
                        else {
                            try {
                                support.pause(testVmId);
                                fail("Pause/unpause is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("PAUSE -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void unpause() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsPauseUnpause(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.unpause(testVmId);
                            vm = awaitState(vm, VmState.RUNNING, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.RUNNING, currentState);
                        }
                        else {
                            try {
                                support.unpause(testVmId);
                                fail("Pause/unpause is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("UNPAUSE -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void suspend() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsSuspendResume(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.suspend(testVmId);
                            vm = awaitState(vm, VmState.SUSPENDED, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.SUSPENDED, currentState);
                        }
                        else {
                            try {
                                support.suspend(testVmId);
                                fail("Suspend/resume is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("SUSPEND -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void resume() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        if( support.supportsSuspendResume(vm) ) {
                            tm.out("Before", vm.getCurrentState());
                            support.resume(testVmId);
                            vm = awaitState(vm, VmState.RUNNING, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                            VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                            tm.out("After", currentState);
                            assertEquals("Current state does not match the target state", VmState.RUNNING, currentState);
                        }
                        else {
                            try {
                                support.resume(testVmId);
                                fail("Suspend/resume is unsupported, yet the method completed without an error");
                            }
                            catch( OperationNotSupportedException expected ) {
                                tm.ok("RESUME -> Operation not supported exception");
                            }
                        }
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }

    @Test
    public void terminate() throws CloudException, InternalException {
        ComputeServices services = tm.getProvider().getComputeServices();

        if( services != null ) {
            VirtualMachineSupport support = services.getVirtualMachineSupport();

            if( support != null ) {
                if( testVmId != null ) {
                    VirtualMachine vm = support.getVirtualMachine(testVmId);

                    if( vm != null ) {
                        tm.out("Before", vm.getCurrentState());
                        support.terminate(vm.getProviderVirtualMachineId());
                        vm = awaitState(vm, VmState.TERMINATED, System.currentTimeMillis() + (CalendarWrapper.MINUTE * 20L));
                        VmState currentState = (vm == null ? VmState.TERMINATED : vm.getCurrentState());
                        tm.out("After", currentState);
                        assertEquals("Current state does not match the target state", VmState.TERMINATED, currentState);
                    }
                    else {
                        tm.warn("Test virtual machine " + testVmId + " no longer exists");
                    }
                }
                else {
                    tm.warn("No test virtual machine was found for this test");
                }
            }
            else {
                tm.ok("No virtual machine support in this cloud");
            }
        }
        else {
            tm.ok("No compute services in this cloud");
        }
    }
}
