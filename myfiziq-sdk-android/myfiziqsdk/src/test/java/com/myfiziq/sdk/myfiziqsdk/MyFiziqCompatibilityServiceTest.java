package com.myfiziq.sdk.myfiziqsdk;

import com.myfiziq.sdk.manager.MyFiziqCompatibilityService;
import com.myfiziq.sdk.manager.compatibility.AndroidSdkCompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.CompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.CpuArchitectureCompatibilityRequirement;
import com.myfiziq.sdk.manager.compatibility.OpenGlCompatibilityRetirement;
import com.myfiziq.sdk.manager.compatibility.RamCompatibilityRequirement;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)                             // Don't forget to run the test with PowerMock using this annotation
@PrepareForTest(MyFiziqCompatibilityService.class)          // Don't forget to put the class under test in this annotation, or else PowerMock won't work
public class MyFiziqCompatibilityServiceTest
{
    @Test
    public void whenDeviceIsCompatible() throws Exception
    {
        mockCompatibility(AndroidSdkCompatibilityRequirement.class, true);
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, true);
        mockCompatibility(RamCompatibilityRequirement.class, true);
        mockCompatibility(OpenGlCompatibilityRetirement.class, true);

        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        boolean actual = service.isDeviceCompatible();

        Assert.assertTrue(actual);
    }

    @Test
    public void whenSdkIsIncompatible() throws Exception
    {
        mockCompatibility(AndroidSdkCompatibilityRequirement.class, false);
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, true);
        mockCompatibility(RamCompatibilityRequirement.class, true);
        mockCompatibility(OpenGlCompatibilityRetirement.class, true);

        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        boolean actual = service.isDeviceCompatible();

        Assert.assertFalse(actual);
    }

    @Test
    public void whenCpuIsIncompatible() throws Exception
    {
        mockCompatibility(AndroidSdkCompatibilityRequirement.class, true);
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, false);
        mockCompatibility(RamCompatibilityRequirement.class, true);
        mockCompatibility(OpenGlCompatibilityRetirement.class, true);

        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        boolean actual = service.isDeviceCompatible();

        Assert.assertFalse(actual);
    }

    @Test
    public void whenRamIsIncompatible() throws Exception
    {
        mockCompatibility(AndroidSdkCompatibilityRequirement.class, true);
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, true);
        mockCompatibility(RamCompatibilityRequirement.class, false);
        mockCompatibility(OpenGlCompatibilityRetirement.class, true);

        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        boolean actual = service.isDeviceCompatible();

        Assert.assertFalse(actual);
    }

    @Test
    public void whenOpenGlIsIncompatible() throws Exception
    {
        mockCompatibility(AndroidSdkCompatibilityRequirement.class, true);
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, true);
        mockCompatibility(RamCompatibilityRequirement.class, true);
        mockCompatibility(OpenGlCompatibilityRetirement.class, false);

        MyFiziqCompatibilityService service = new MyFiziqCompatibilityService();
        boolean actual = service.isDeviceCompatible();

        Assert.assertFalse(actual);
    }

    @Test
    public void whenCpuCompatibleTrue() throws Exception
    {
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, true);

        boolean actual = MyFiziqCompatibilityService.isCpuCompatible();
        Assert.assertTrue(actual);
    }

    @Test
    public void whenCpuCompatibleFalse() throws Exception
    {
        mockCompatibility(CpuArchitectureCompatibilityRequirement .class, false);

        boolean actual = MyFiziqCompatibilityService.isCpuCompatible();
        Assert.assertFalse(actual);
    }

    private <T extends CompatibilityRequirement> void mockCompatibility(Class<T> clazz, boolean compatible) throws Exception
    {
        // Create a new instance of the class we're mocking
        T requirement = PowerMockito.mock(clazz);

        // When the "isCompatible" method is called, return our newly created mocked object
        PowerMockito.when(requirement.isCompatible()).thenReturn(compatible);

        // When a new instance of the class we're mocking is created in the class under test, return our mocked object with stubbed compatibility values
        PowerMockito.whenNew(clazz).withAnyArguments().thenReturn(requirement);
    }
}
