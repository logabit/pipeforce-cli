package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.config.CliConfig;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.command.stub.ServerInfoParams;
//import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.util.DateTimeUtil;
import com.logabit.pipeforce.common.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a new resource.
 *
 * @author sniederm
 * @since 6.0
 */
public class StatusCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 0) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        Map status = new LinkedHashMap();

        CliConfig.Instance instance = getContext().getCurrentInstance();
        ConfigCliService config = getContext().getConfigService();

        Map cliStatus = new LinkedHashMap();
        cliStatus.put("status", "OK");
        cliStatus.put("url", instance.getHubApiUrl(""));
        cliStatus.put("username", instance.getUsername());
        cliStatus.put("apiTokenCreated", instance.getApiTokenCreated());
        cliStatus.put("configLastUpdated", config.getConfigUpdated());
        cliStatus.put("home", config.getInstallationHome());
        cliStatus.put("version", config.getReleaseTagFromJar());
        cliStatus.put("lastUpdateCheck", DateTimeUtil.timestampToIso8061(config.getUpdateCheckLast()));

        status.put("cli", cliStatus);

        try {

            JsonNode info = getContext().getResolver().command(
                    new ServerInfoParams(),
                    JsonNode.class
            );

            Map infoMap = JsonUtil.objectToMap(info);
            status.put("server", infoMap);
        } catch (Exception e) {
            status.put("server", "Not reachable.");
        }

        out.printResult(status);
        return 0;
    }

    public String getUsageHelp() {
        return "pi status\n" +
                "   Shows config and status information.\n" +
                "   Example: pi status";
    }
}
