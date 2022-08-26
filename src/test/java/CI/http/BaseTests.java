package CI.http;
import common.CommonBase;
import org.eclipse.jetty.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.Test;
public class BaseTests {

    @Test
    public void HelloWorld() {
        CommonBase.clientFakeUser.getResponse("/hello_world", HttpMethod.GET);
    }
}
