package com.logabit.pipeforce.cli;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link CommandArgs}.
 *
 * @author sniederm
 * @since 2.10
 */
public class CommandArgsTest {

    @Test
    public void testArgsParsing() {

        CommandArgs args = new CommandArgs("param1=value1", "-o3", "param2=value2", "param3", "-o1:v1", "-o2:v2");
        Assert.assertEquals("value1", args.getOptionValue("param1"));
        Assert.assertEquals("value2", args.getOptionValue("param2"));
        Assert.assertEquals(null, args.getOptionValue("param3"));
        Assert.assertEquals("v1", args.getSwitch("o1"));
        Assert.assertEquals("v2", args.getSwitch("o2"));
        Assert.assertEquals(null, args.getSwitch("o3"));

        args = new CommandArgs();
        Assert.assertTrue(args.getOptions().isEmpty());
        Assert.assertTrue(args.getSwitches().isEmpty());

        args = new CommandArgs("param1=", "-o1:");
        Assert.assertEquals(null, args.getOptionValue("param1"));
        Assert.assertEquals(null, args.getSwitch("-o1"));
    }
}
