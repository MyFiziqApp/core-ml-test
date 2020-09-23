package com.myfiziq.myfiziq_android;

import com.myfiziq.sdk.manager.MyFiziqSdkManager;

import org.junit.Assert;
import org.junit.Test;

public class HmacTests
{
    @Test
    public void evoltPasswordIsComputedCorrectly()
    {
        String evoltPassword = MyFiziqSdkManager.computeEvoltPass("666", "2019-10-05 06:18:40");
        Assert.assertEquals("dlHS736rrci30ts4bh9g+YrQd8SxQbMiw3+3gcRrvX0=", evoltPassword);
    }
}
