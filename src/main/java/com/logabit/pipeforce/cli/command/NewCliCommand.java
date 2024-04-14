package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.skeleton.template.AppConfigTemplate;
import com.logabit.pipeforce.common.skeleton.template.BPMNWorkflowTemplate;
import com.logabit.pipeforce.common.skeleton.template.FormConfigTemplate;
import com.logabit.pipeforce.common.skeleton.template.ListTemplate;
import com.logabit.pipeforce.common.skeleton.template.PipelineYAMLTemplate;
import com.logabit.pipeforce.common.skeleton.template.SchemaTemplate;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.PipelineUtil;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates a new resource.
 *
 * @author sniederm
 * @since 6.0
 */
public class NewCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String resource;
        if (args.getLength() == 1) {

            // pi new RESOURCE
            resource = args.getOptionKeyAt(0);
        } else {

            out.println("Create new...");
            List<String> items = ListUtil.asList(
                    "app", "form", "list", "schema", "pipeline", "workflow");
            int selectedResource = in.choose(items);

            resource = items.get(selectedResource);
        }

        String appName = null;
        switch (resource) {

            case "app":
                appName = createApp();
                break;
            case "form":
                appName = askForSelectedApp(null);
                createForm(appName);
                break;
            case "list":
                appName = askForSelectedApp(null);
                createList(appName);
                break;
            case "schema":
                appName = askForSelectedApp(null);
                createSchema(appName);
                break;
            case "pipeline":
                appName = askForSelectedApp(null);
                createPipeline(appName);
                break;
            case "workflow":
                appName = askForSelectedApp(null);
                createWorkflow(appName);
                break;
        }

        out.println("Tip 1: When finished, run 'pi publish' to upload and activate your changes.");
        out.println("Tip 2: Run 'code .' to open this workspace in VS Code.");
        return 0;
    }

    private void createWorkflow(String appName) {

        String workflowName;
        File bpmnFile;

        while (true) {
            workflowName = in.ask("Workflow name");

            if (!workflowName.matches("([a-z0-9]+)")) {
                out.println("Workflow name must be lower case and may not contain any special chars or spaces: " +
                        workflowName);
                out.println("Select a different name.");
                continue;
            }

            bpmnFile = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/workflow/" +
                    workflowName + ".bpmn");

            if (bpmnFile.exists()) {
                out.println("Workflow with name [" + workflowName + "] already exists: " + bpmnFile.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        BPMNWorkflowTemplate template = new BPMNWorkflowTemplate();
        String workflowContent = template.getContent(appName, workflowName);

        FileUtil.saveStringToFile(workflowContent, bpmnFile.getAbsolutePath());

        out.println("Workflow created: " + bpmnFile.getAbsolutePath());
    }

    private void createPipeline(String appName) {

        String pipelineName;
        File pipelineFile;

        while (true) {
            pipelineName = in.ask("Pipeline name");

            if (!PipelineUtil.isPipelineNameValid(pipelineName)) {
                out.println("Pipeline name is invalid. It does not match ([a-z0-9-]+): " + pipelineName);
                out.println("Select a different name.");
                continue;
            }

            pipelineFile = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/pipeline/" + pipelineName + ".pi.yaml");

            if (pipelineFile.exists()) {
                out.println("Pipeline with name [" + pipelineName + "] already exists: " + pipelineFile.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        PipelineYAMLTemplate template = new PipelineYAMLTemplate();
        String pipelineContent = template.getContent();

        FileUtil.saveStringToFile(pipelineContent, pipelineFile.getAbsolutePath());

        out.println("Pipeline created: " + pipelineFile.getAbsolutePath());
    }

    private void createList(String appName) {

        String listName;
        File listFile;

        while (true) {
            listName = in.ask("List name");

            if (!listName.matches("([a-z0-9]+)")) {
                out.println("List name must be lower case and may not contain any special chars or spaces: " + listName);
                out.println("Select a different name.");
                continue;
            }

            listFile = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/list/" + listName + ".json");

            if (listFile.exists()) {
                out.println("List with name [" + listName + "] already exists: " + listFile.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        String description = in.ask("Optional description of list", "");

        out.println("Do you like to connect a schema to your list?");
        File schemaRoot = new File(context.getPropertiesHomeFolder(), "global/app/" + appName + "/schema");
        List<File> schemaFiles = FileUtil.listFiles(schemaRoot);

        List<String> schemaFileNames = schemaFiles.stream()
                .map(file -> PathUtil.removeExtension(file.getName()))
                .collect(Collectors.toList());

        schemaFileNames.add("[Do not connect schema to list]");
        schemaFileNames.add("[Create a new schema]");
        Integer selectedItem = in.choose(schemaFileNames);
        String selectedSchemaName = schemaFileNames.get(selectedItem);

        if (selectedSchemaName.equals("[Create a new schema]")) {
            selectedSchemaName = createSchema(appName);
        }

        ListTemplate listTemplate = new ListTemplate();
        String listConfigContent = listTemplate.getContent(listName, null, description,
                selectedSchemaName.equals("[Do not connect schema to list]") ? null : appName, selectedSchemaName);

        FileUtil.saveStringToFile(listConfigContent, listFile.getAbsolutePath());

        out.println("List created: " + listFile.getAbsolutePath());
    }

    private String createSchema(String appName) {

        String schemaName;
        File schemaFile;

        while (true) {
            schemaName = in.ask("Schema name");

            if (!schemaName.matches("([a-z0-9]+)")) {
                out.println("Schema name must be lower case and may not contain any special chars or spaces: " + schemaName);
                out.println("Select a different name.");
                continue;
            }

            schemaFile = new File(getContext().getPropertiesHomeFolder(),
                    "global/app/" + appName + "/schema/" + schemaName + ".json");

            if (schemaFile.exists()) {
                out.println("Schema with name [" + schemaName + "] already exists: " + schemaFile.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        SchemaTemplate template = new SchemaTemplate();
        String schemaContent = template.getContent();

        FileUtil.saveStringToFile(schemaContent, schemaFile.getAbsolutePath());

        out.println("Schema created: " + schemaFile.getAbsolutePath());
        return schemaName;
    }

    private void createForm(String appName) {

        String formName;
        File formFile;

        while (true) {

            formName = in.ask("New form name");

            if (StringUtil.isEmpty(formName)) {
                continue;
            }

            if (!formName.matches("([a-z0-9]+)")) {
                out.println("Form name must be lower case and may not contain any special chars or spaces: " + formName);
                out.println("Select a different name.");
                continue;
            }

            formFile = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/config/" + formName + ".json");

            if (formFile.exists()) {
                out.println("Form with name [" + formName + "] already exists.");
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        String description = in.ask("Optional description of form", "");

        out.println("Select the schema to connect with this form:");

        File schemaFolder = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/schema");
        List<File> schemaFiles = FileUtil.listFiles(schemaFolder.getAbsolutePath());
        List<String> schemaFileNames = schemaFiles.stream()
                .map(file -> PathUtil.removeExtension(file.getName()))
                .collect(Collectors.toList());
        schemaFileNames.add("[Do not connect to a schema]");
        schemaFileNames.add("[Create a new schema]");
        Integer selectedItem = in.choose(schemaFileNames);
        String selectedSchemaName = schemaFileNames.get(selectedItem);

        if (selectedSchemaName.equals("[Create a new schema]")) {
            selectedSchemaName = createSchema(appName);
        }

        FormConfigTemplate formConfigTemplate = new FormConfigTemplate();
        String formConfigContent = formConfigTemplate.getContent(formName, null, description,
                selectedSchemaName.equals("[Do not connect to a schema]") ? null : appName, selectedSchemaName);

        File formConfigFile = new File(getContext().getPropertiesHomeFolder(),
                "global/app/" + appName + "/form/" + formName + ".json");
        FileUtil.saveStringToFile(formConfigContent, formConfigFile.getAbsolutePath());

        out.println("Form created: " + formConfigFile.getAbsolutePath());
    }

    private String createApp() {

        File srcFolder = getContext().getPropertiesHomeFolder();

        while (true) {

            String appName = in.ask("New app name", null);

            try {
                PathUtil.validatePathPart(appName);
            } catch (Exception e) {
                out.println("Invalid app name: " + appName + ". " + e.getMessage());
                out.println("Select a different name.");
                continue;
            }

            if (appName.split("\\.").length < 3) {
                out.println("App name must be qualified: <tld>.<domain>.<appname>");
                out.println("Example: com.logabit.myapp");
                continue;
            }

            File appFolder = new File(srcFolder, "global/app/" + appName);

            if (appFolder.exists()) {
                out.println("App with this name already exists at: " + appFolder.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            String title;
            while (true) {
                title = in.ask("Title of the app", appName);
                if (StringUtil.isEmpty(title)) {
                    continue;
                }
                break;
            }

            String description = in.ask("Description of the app", "");

            String icon;
            while (true) {
                icon = in.ask("Go to https://material.io/resources/icons and select the " +
                        "name of the icon to be used", "assignment");
                if (StringUtil.isEmpty(icon)) {
                    continue;
                }
                break;
            }

            File appConfigFolder = new File(appFolder, "config");
            FileUtil.createFolders(appConfigFolder);

            AppConfigTemplate template = new AppConfigTemplate();
            String appConfigContent = template.getContent(appName, description, icon, null);

            File appConfigFile = new File(appConfigFolder, "app.json");
            FileUtil.saveStringToFile(appConfigContent, appConfigFile.getAbsolutePath());

            out.println("App created at: " + appFolder.getAbsolutePath());
            return appName;
        }
    }

    public String getUsageHelp() {
        return "pi new [<RESOURCE_NAME>]\n" +
                "   Creates a local resource. Also see 'pi publish' to upload to server.\n" +
                "   Examples:\n" +
                "     pi new - Shows the list of wizards to select from.\n" +
                "     pi new app - Creates a new app.\n" +
                "     pi new form - Creates a new form.\n" +
                "     pi new list - Creates a new list.\n" +
                "     pi new schema - Creates a new schema.\n" +
                "     pi new workflow - Creates a new workflow.\n" +
                "     pi new pipeline - Creates a new pipeline file.";
    }

    /**
     * Lists all apps and lets the user select from this list of existing apps or creating a new one.
     *
     * @param message
     * @return
     */
    public String askForSelectedApp(String message) {

        File appsRootFolder = new File(getContext().getPropertiesHomeFolder(), "global/app");
        List<File> appFolders = Collections.EMPTY_LIST;

        if (appsRootFolder.exists()) {
            appFolders = FileUtil.listFiles(appsRootFolder);
        }

        if (message == null) {
            message = "Select the app to apply the action to:";
        }
        out.println(message);

        List<String> appNames = appFolders.stream().map(File::getName).collect(Collectors.toList());
        appNames.add("[Create new app...]");

        Integer selectedItem = in.choose(appNames, null, null);

        String selectedValue = appNames.get(selectedItem);

        if (selectedValue.equals("[Create new app...]")) {
            selectedValue = createApp();
        }

        return selectedValue;
    }
}
