package com.logabit.test;

import com.logabit.pipeforce.common.test.util.CheckstyleUtil;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.junit.Test;

import java.io.IOException;

/**
 * Runs the checkstyle rules as part of our unit tests.
 */
public class CheckstyleTest {

    @Test
    public void testCheckstyle() throws IOException, CheckstyleException {

        CheckstyleUtil.run(this, "src/main",
                "classpath:/checkstyle.xml");
    }

}
