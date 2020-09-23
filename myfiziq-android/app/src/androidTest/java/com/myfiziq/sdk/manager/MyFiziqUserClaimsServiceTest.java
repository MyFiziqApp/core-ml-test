package com.myfiziq.sdk.manager;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;


public class MyFiziqUserClaimsServiceTest
{
    // This matches the iOS implementation
    // See: https://github.com/MyFiziqApp/myfiziq-sdk-loginview/blob/8f7efb32613df5a58c26fdb1c5daebf179e7b653/MyFiziqSDKLoginView/Classes/Public/MyFiziqLogin.m#L102
    @Test
    public void userClaimsUsernameIsComputedCorrectly()
    {
        String partnerUserId = "106";

        MyFiziqUserClaimsService claimsService = new MyFiziqUserClaimsService();
        String actual = claimsService.generateCustomUsernameFromId(partnerUserId);

        Assert.assertEquals("106-7282cef2-08538ccb@myfiziq.com", actual);
    }

    // This matches the iOS implementation
    // See: https://github.com/MyFiziqApp/myfiziq-sdk-loginview/blob/8f7efb32613df5a58c26fdb1c5daebf179e7b653/MyFiziqSDKLoginView/Classes/Public/MyFiziqLogin.m#L132
    @Test
    public void userClaimsPasswordIsComputedCorrectly()
    {
        String partnerUserId = "106";
        List<String> claims = Collections.singletonList("8768f0a4-3933-4dca-bae6-8141287576fc");
        String salt = "e0ff54f8-b05b-bdb1-897a-2d8ccc323dc8";

        MyFiziqUserClaimsService claimsService = new MyFiziqUserClaimsService();
        String actual = claimsService.generateCustomPasswordForId(partnerUserId, claims, salt);

        Assert.assertEquals("U2DimehBLeX9co/XcqiwZE6ioeU823iaXCFGbceuJ7E=", actual);
    }
}
