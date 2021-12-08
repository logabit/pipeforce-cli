package com.logabit.pipeforce.cli;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Tests the {@link CliPathArg}.
 *
 * @author sniederm
 * @since 2.20
 */
public class CliPathArgTest {

    @Test
    public void testPathArgs() {

        // Workspace home
        String home = "/User/someUser/pipeforce";

        CliPathArg pathArg = createPathArg("src/global/app/*/pipeline/*", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/*/pipeline/*", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/*/pipeline/*", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", pathArg.getLocalPattern());
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline/hello.pi.yaml", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/hello.pi.yaml", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline/hello.pi.yaml", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/file", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/file", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/file", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", pathArg.getLocalPattern());
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", pathArg.getLocalPattern());
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline/**", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline/**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("/User/someUser/pipeforce/src/global/app/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());
    }

    private CliPathArg createPathArg(String path, String home, boolean expectDir) {

        CliPathArg orig = new CliPathArg(path, home) {

            public boolean isDir(File file) {
                return expectDir;
            }
        };

        return orig;
    }
}
