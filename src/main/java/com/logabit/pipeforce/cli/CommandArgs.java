package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.Set;

/**
 * Parses the command args typed in by the user. Doesnt contain the part "pi command".
 * Only options and switches: pi COMMAND OPT1=VAL1 OP2=VAL2 -s1=v1 -s2=v2
 *
 * @author sniederm
 * @since 7.0
 */
public class CommandArgs {

    public static final CommandArgs EMPTY = new CommandArgs();

    private LinkedMap<String, String> options = new LinkedMap<>();

    private LinkedMap<String, String> switches = new LinkedMap<>();

    private String[] originalArgs;

    private int length;

    public CommandArgs(String... args) {

        // param1=value1 param2=value2 -o1:v1 -o2:v2

        if (args == null || args.length == 0) {
            this.length = 0;
            return;
        }

        this.originalArgs = args;
        this.length = args.length;

        for (String arg : args) {

            // --name=value
            if (arg.startsWith("--")) {
                arg = arg.substring(1);
            }

            // -n=value
            if (arg.startsWith("-")) {
                String[] split = StringUtil.split(arg.substring(1), ":");
                this.switches.put(split[0], split.length == 2 ? split[1] : null);
                continue;
            }

            // foo=bar
            String[] split = StringUtil.split(arg, "=");
            this.options.put(split[0], split.length == 2 ? StringUtil.removeQuotes(split[1]) : null);
        }
    }

    public LinkedMap<String, String> getSwitches() {
        return switches;
    }

    public String getSwitch(String key) {
        return this.switches.get(key);
    }

    public LinkedMap<String, String> getOptions() {
        return options;
    }

    public String getOptionKeyAt(int index) {

        Set<String> keys = this.options.keySet();
        for (String key : keys) {
            if (this.options.indexOf(key) == index) {
                return key;
            }
        }

        return null;
    }

    public String getOptionValueAt(int index) {

        Set<String> keys = this.options.keySet();
        for (String key : keys) {
            if (this.options.indexOf(key) == index) {
                return options.get(key);
            }
        }

        return null;
    }

    public String getOptionValue(String key) {
        return this.options.get(key);
    }

    public int getLength() {
        return length;
    }

    public String[] getOriginalArgs() {
        return originalArgs;
    }
}
