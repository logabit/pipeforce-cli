package com.logabit.pipeforce.cli;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link Util} class.
 *
 * @author sn
 */
public class UtilTest {

    @Test
    public void tesConvertToLinuxPath() {

        String path = Util.convertToLinuxPath("C:\\testing\\");
        Assert.assertEquals("/testing/", path);
    }
}
