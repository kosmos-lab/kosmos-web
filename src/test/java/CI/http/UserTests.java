package CI.http;

import common.CommonBase;
import de.kosmos_lab.web.server.WebServer;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UserTests {

    @Test
    public void testLogin() {
        Assert.assertTrue(CommonBase.clientAdmin.refreshToken());
        Assert.assertTrue(CommonBase.clientUser.refreshToken());
        Assert.assertTrue(CommonBase.clientUser2.refreshToken());
        Assert.assertTrue(CommonBase.clientUser3.refreshToken());
        Assert.assertFalse(CommonBase.clientFakeUser.refreshToken());
    }

    @Test
    public void testUserView() {
        ContentResponse response = CommonBase.clientAdmin.getResponse("/user/view", HttpMethod.GET);
        Assert.assertNotNull(response, "response should not have been null");
        Assert.assertEquals( response.getStatus(),WebServer.STATUS_OK, "admin should have been able to see this");
        response = CommonBase.clientUser.getResponse("/user/view", HttpMethod.GET);
        Assert.assertNotNull(response, "response should not have been null");
        Assert.assertEquals(response.getStatus(),WebServer.STATUS_FORBIDDEN, "user should not be able to see this");
        response = CommonBase.clientFakeUser.getResponse("/user/view", HttpMethod.GET);
        Assert.assertNull(response, "response should have been null for fake user");
        //Assert.assertEquals(WebServer.STATUS_FORBIDDEN,response.getStatus());
    }
}
