package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.cli.service.ConfigCliService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;

/**
 * Tests the {@link CliContext}.
 *
 * @author sniederm
 * @since 2.7
 */
public class CliContextTest {

//    @Test
//    public void testGetRelativeToHome() {
//
//        CliContext cliContext = new CliContext();
//        ConfigCliService configServiceMock = Mockito.mock(ConfigCliService.class);
//        Mockito.when(configServiceMock.getHome()).thenReturn("/Users/someUser/pipeforce");
//        cliContext.setConfigService(configServiceMock);
//
//        String key = cliContext.getRelativeToHome(new File("/Users/someUser/pipeforce/propertystore/global/app/myapp/**"));
//        Assert.assertEquals("propertystore/global/app/myapp/**", key);
//
//        try {
//            cliContext.getRelativeToHome(new File("/var/lib/propertystore/global/app/myapp/**"));
//            Assert.fail("Expected but not thrown since file is not inside pipeforce home");
//        } catch (Exception e) {
//        }
//    }

//    @Test
//    public void testGetRelativeSrc() {
//
//        CliContext cliContextOrig = new CliContext();
//        CliContext spy = Mockito.spy(cliContextOrig);
//        Mockito.doReturn(new File("/Users/someUser/pipeforce/src/global")).when(spy).getCurrentWorkDir();
//
//        ConfigCliService configServiceMock = Mockito.mock(ConfigCliService.class);
//        Mockito.when(configServiceMock.getHome()).thenReturn("/Users/someUser/pipeforce");
//        spy.setConfigService(configServiceMock);
//
//        // Relative to src
//        String key = spy.getRelativeToSrc("app/myapp/**");
//        Assert.assertEquals("global/app/myapp/**", key);
//
//        // Relative to current work dir which must be inside src folder
//        key = spy.getRelativeToSrc("app/myapp/**");
//        Assert.assertEquals("global/app/myapp/**", key);
//
//        // Relative to current work dir which must be inside src folder
//        key = spy.getRelativeToSrc("");
//        Assert.assertEquals("global", key);
//
//        // Relative to current work dir which must be inside src folder
//        key = spy.getRelativeToSrc("**");
//        Assert.assertEquals("global/**", key);
//
//        Mockito.doReturn(new File("/Users/someUser/pipeforce")).when(spy).getCurrentWorkDir();
//
//        key = spy.getRelativeToSrc("src/global/app/myapp/**");
//        Assert.assertEquals("global/app/myapp/**", key);
//    }
}
