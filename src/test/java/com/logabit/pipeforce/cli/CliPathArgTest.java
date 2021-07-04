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

        // Current working dir
        String cwd = "/User/someUser/pipeforce";

        CliPathArg pathArg = createPathArg("global/app/*/pipeline/*", cwd, home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/*/pipeline/*", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/*/pipeline/*", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("**", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", pathArg.getLocalPattern());
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("global/app/myapp/", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("global/app/myapp", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("global/app/myapp/pipeline/hello.pi.yaml", cwd, home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/hello.pi.yaml", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline/hello.pi.yaml", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/file", cwd, home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/file", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/file", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        cwd = "/User/someUser/pipeforce/src";

        pathArg = createPathArg("**", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", pathArg.getLocalPattern());
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("global/app/myapp", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("global/app/myapp/", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        cwd = "/User/someUser/pipeforce/src/global/app";

        pathArg = createPathArg("**", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("myapp", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("myapp/pipeline", cwd, home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("myapp/pipeline/**", cwd, home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/**", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/myapp/pipeline/**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("", cwd, home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app", pathArg.getLocalPattern());
        Assert.assertEquals("global/app/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        cwd = "/some/other/path"; // Not allowed outside PIPEFORCE home

        try {
            pathArg = createPathArg("/User/someUser/pipeforce/src/global", cwd, home, true);
            Assert.fail("Not thrown");
        } catch (Exception e) {

        }

        cwd = "/User/someUser/pipeforce/config"; // Not allowed outside PIPEFORCE home (even not in config folder)

        try {
            pathArg = createPathArg("/User/someUser/pipeforce/src/global", cwd, home, true);
            Assert.fail("Not thrown");
        } catch (Exception e) {

        }
    }

    private CliPathArg createPathArg(String path, String cwd, String home, boolean expectDir) {

        CliPathArg orig = new CliPathArg(path, cwd, home) {

            public boolean isDir(File file) {
                return expectDir;
            }
        };

        return orig;
    }
}
