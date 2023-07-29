package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ReflectionUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shows the help message.
 *
 * @author sniederm
 * @since 6.0
 */
public class HelpCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 3) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        if (args.getLength() == 0) {

            // pi help
            showCliOptions();
            return 0;
        }

        if (args.getLength() == 1) {

            // pi help command
            showCommandDocs(null);
            return 0;
        }

        if (args.getLength() == 2) {

            // pi help command COMMAND_NAME
            showCommandDocs(args.getOptionKeyAt(1));
            return 0;
        }


        out.println("Also see: https://devdocs.pipeforce.io");
        return 0;
    }

    private void showCommandDocs(String commandName) {

        String url = getContext().getCurrentInstance().getHubApiUrl("command/pipeline.schema.get");
        Map result = getContext().getRestTemplate().getForObject(url, Map.class);

        result = (Map) result.get("properties");
        result = (Map) result.get("pipeline");
        result = (Map) result.get("items");
        result = (Map) result.get("properties");

        Map finalResult = new LinkedHashMap();
        if (!StringUtil.isEmpty(commandName)) {

            Set commandNames = result.keySet();
            for (Object name : commandNames) {
                if (name.toString().contains(commandName)) {
                    finalResult.put(name, result.get(name));
                }
            }

        } else {
            finalResult = result;
        }

        out.println(JsonUtil.objectToYamlString(finalResult));
    }

    private void showCliOptions() {

        Reflections reflections = new Reflections("com.logabit.pipeforce.cli.command");
        Set<Class<? extends ICliCommand>> commands = reflections.getSubTypesOf(ICliCommand.class);
        List<Class<? extends ICliCommand>> commandsList = new ArrayList<>(commands);

        commandsList.sort(Comparator.comparing(Class::getSimpleName));

        out.println("Available CLI commands:");
        for (Class<? extends ICliCommand> command : commandsList) {

            if (Modifier.isAbstract(command.getModifiers()) || Modifier.isInterface(command.getModifiers())) {
                continue;
            }

            ICliCommand cmd = (ICliCommand) ReflectionUtil.newInstance(command);

            String usageText = cmd.getUsageHelp();
            if (usageText != null) {
                out.println(usageText);
            }
        }
    }

    public String getUsageHelp() {
        return "pi help [command] [<COMMAND_NAME>]\n" +
                "   Shows the documentation messages.\n" +
                "   Examples: \n" +
                "     pi help - Shows the CLI options.\n" +
                "     pi help command - Shows all available command docs.\n" +
                "     pi help command log - Shows docs of the log command.";
    }
}
