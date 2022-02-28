package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.common.util.PathUtil;
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
        String home = "/User/someUser/pipeforce/src";

        CliPathArg pathArg = createPathArg("src/global/app/*/pipeline/*", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/*/pipeline/*", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/*/pipeline/*", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline/hello.pi.yaml", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/hello.pi.yaml", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/pipeline/hello.pi.yaml", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/file", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/file", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/file", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/**", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/pipeline", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/myapp/pipeline/**", home, false);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/myapp/pipeline/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/myapp/pipeline/**", pathArg.getRemotePattern());
        Assert.assertTrue(pathArg.isPattern());

        pathArg = createPathArg("src/global/app/", home, true);
        Assert.assertEquals("/User/someUser/pipeforce/src/global/app/**", PathUtil.toUnixPath(pathArg.getLocalPattern(), false));
        Assert.assertEquals("global/app/**", pathArg.getRemotePattern());
        Assert.assertFalse(pathArg.isPattern());

        try {
            pathArg = createPathArg("/User/someUser/pipeforce/src/global/app/", home, true);
            Assert.fail("Exception expected, not thrown.");
        } catch (Exception e) {
        }
    }

    private CliPathArg createPathArg(String path, String home, boolean expectDir) {

        CliPathArg orig = new CliPathArg(path, new File(home)) {

            public boolean isDir(File file) {
                return expectDir;
            }
        };

        return orig;
    }
}
