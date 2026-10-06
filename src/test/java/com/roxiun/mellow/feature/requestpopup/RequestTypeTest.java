package com.roxiun.mellow.feature.requestpopup;

import org.junit.Assert;
import org.junit.Test;

public class RequestTypeTest {

    @Test
    public void partyInvitesOnlySendAcceptCommand() {
        Assert.assertEquals(
            "/party accept Example",
            RequestType.PARTY.buildCommand(true, "Example")
        );
        Assert.assertNull(RequestType.PARTY.buildCommand(false, "Example"));
    }
}
