package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.ListUtil;
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
                    "app", "form", "list", "object", "pipeline", "workflow");
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
            case "object":
                appName = askForSelectedApp(null);
                createObject(appName);
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

        out.println("Hint: When finished, run 'pi publish' to upload and activate your changes.");
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

        String workflowId = appName + "_" + workflowName; // Add appId to give workflow engine chance to detect app name
        String workflowContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" " +
                "xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
                "id=\"Definitions_1ltbcnm\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"" + workflowId + "\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n" +
                "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"" + workflowId + "\">\n" +
                "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n" +
                "        <dc:Bounds x=\"179\" y=\"79\" width=\"36\" height=\"36\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "    </bpmndi:BPMNPlane>\n" +
                "  </bpmndi:BPMNDiagram>\n" +
                "</bpmn:definitions>";

        FileUtil.saveStringToFile(workflowContent, bpmnFile.getAbsolutePath());

        out.println("Workflow created: " + bpmnFile.getAbsolutePath());
    }

    private void createPipeline(String appName) {

        String pipelineName;
        File pipelineFile;

        while (true) {
            pipelineName = in.ask("Pipeline name");

            if (!pipelineName.matches("([a-z0-9]+)")) {
                out.println("Pipeline name must be lower case and may not contain any special chars or spaces: " + pipelineName);
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

        String pipelineContent = "" +
                "pipeline:\n" +
                "  - log:        \n" +
                "      message: \"Hello World\"";

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

        out.println("Do you like to load existing objects into your list?");
        File objectsRoot = new File(context.getPropertiesHomeFolder(), "global/app/" + appName + "/object");
        List<File> objectFolders = FileUtil.listFiles(objectsRoot);
        List<String> objectNames = objectFolders.stream().map(File::getName).collect(Collectors.toList());
        objectNames.add("[Do not show existing object in list]");
        objectNames.add("[Create a new object schema]");
        Integer selectedItem = in.choose(objectNames);
        String selectedObject = objectNames.get(selectedItem);

        if (selectedObject.equals("[Create a new object schema]")) {
            selectedObject = createObject(appName);
        }

        String inputPipeline = "";
        String schemaPipeline = "";
        if (!selectedObject.equals("[Do not show existing object in list]")) {
            inputPipeline = "property.value.expression?from=global/app/" + appName + "/object/" + selectedObject + "/v1/instance/*";
            schemaPipeline = "property.list?filter=global/app/" + appName + "/object/" + selectedObject + "/v1/schema";
        }

        String listConfigContent = "{\n" +
                "  \"title\": \"" + listName + "\",\n" +
                "  \"description\": \"" + description + "\",\n" +
                "  \"input\": \"" + inputPipeline + "\",\n" +
                "  \"schema\": \"" + schemaPipeline + "\"\n" +
                "}";

        FileUtil.saveStringToFile(listConfigContent, listFile.getAbsolutePath());

        out.println("List created: " + listFile.getAbsolutePath());
    }

    private String createObject(String appName) {

        String objectName;
        File objectFolder;

        while (true) {
            objectName = in.ask("Object name");

            if (!objectName.matches("([a-z0-9]+)")) {
                out.println("Object name must be lower case and may not contain any special chars or spaces: " + objectName);
                out.println("Select a different name.");
                continue;
            }

            objectFolder = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/object/" + objectName);

            if (objectFolder.exists()) {
                out.println("Object with name [" + objectName + "] already exists: " + objectFolder.getAbsolutePath());
                out.println("Select a different name.");
                continue;
            }

            break;
        }

        String objectSchemaContent = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"someNumber\": {\n" +
                "      \"title\": \"Some Number\",\n" +
                "      \"type\": \"number\",\n" +
                "      \"description\": \"This is a number property.\"\n" +
                "    },\n" +
                "    \"someText\": {\n" +
                "      \"title\": \"Some Text\",\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"This is a text property.\"\n" +
                "    },\n" +
                "    \"someBoolean\": {\n" +
                "      \"title\": \"Some Bool\",\n" +
                "      \"type\": \"boolean\",\n" +
                "      \"description\": \"This is a boolean (yes/no) property.\"\n" +
                "    },\n" +
                "    \"someSingleList\": {\n" +
                "      \"title\": \"Some Single List\",\n" +
                "      \"type\": \"string\",\n" +
                "      \"description\": \"This is a single-select list of text items.\",\n" +
                "      \"enum\": [\"item1\", \"item2\"]\n" +
                "    },\n" +
                "    \"someMultiList\": {\n" +
                "      \"title\": \"Some Multi List\",\n" +
                "      \"type\": \"array\",\n" +
                "      \"description\": \"This is a multi-select list.\",\n" +
                "      \"items\": {\n" +
                "        \"type\": \"string\",\n" +
                "        \"enum\": [\"item1\", \"item2\", \"item3\"]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";


        File objectSchemaFile = new File(objectFolder, "v1/schema.json");

        FileUtil.saveStringToFile(objectSchemaContent, objectSchemaFile.getAbsolutePath());

        out.println("Object schema created: " + objectSchemaFile.getAbsolutePath());
        return objectName;
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

        out.println("Select the object schema to connect with this form:");

        File objectFolder = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/object");
        List<File> objectFolders = FileUtil.listFiles(objectFolder.getAbsolutePath());
        List<String> objectNames = objectFolders.stream().map(File::getName).collect(Collectors.toList());
        objectNames.add("[Do not connect to object schema]");
        objectNames.add("[Create a new object schema]");
        Integer selectedItem = in.choose(objectNames);
        String selectedObject = objectNames.get(selectedItem);

        if (selectedObject.equals("[Create a new object schema]")) {
            selectedObject = createObject(appName);
        }

        String schemaPathPipeline = "";
        String outputPath = "";
        if (!selectedObject.equals("[Do not connect to object schema]")) {
            schemaPathPipeline = "property.list?filter=global/app/" + appName + "/object/" + selectedObject + "/v1/schema";
            outputPath = "global/app/" + appName + "/object/" + selectedObject + "/v1/instance/%23%7Bvar.property.uuid%7D";
        }

        String formConfigContent = "{\n" +
                "  \"title\": \"" + formName + "\",\n" +
                "  \"description\": \"" + description + "\",\n" +
                "  \"schema\": \"" + schemaPathPipeline + "\",\n" +
                "  \"output\": \"" + outputPath + "\"\n" +
                "}";

        File formConfigFile = new File(getContext().getPropertiesHomeFolder(), "global/app/" + appName + "/form/" + formName + ".json");
        FileUtil.saveStringToFile(formConfigContent, formConfigFile.getAbsolutePath());

        out.println("Form created: " + formConfigFile.getAbsolutePath());
    }

    private String createApp() {

        File srcFolder = getContext().getPropertiesHomeFolder();

        while (true) {

            String appName = in.ask("New app name", null);

            if (!appName.matches("([a-z0-9]+)")) {
                out.println("App name must be lower case and may not contain any special chars or spaces: " + appName);
                out.println("Select a different name.");
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
            FileUtil.createFolders(new File(appFolder, "form"));
            FileUtil.createFolders(new File(appFolder, "function"));
            FileUtil.createFolders(new File(appFolder, "list"));
            FileUtil.createFolders(new File(appFolder, "object"));
            FileUtil.createFolders(new File(appFolder, "pipeline"));
            FileUtil.createFolders(new File(appFolder, "resource/public"));
            FileUtil.createFolders(new File(appFolder, "setup"));
            FileUtil.createFolders(new File(appFolder, "test"));
            FileUtil.createFolders(new File(appFolder, "workflow"));

            String appConfigContent = "{\n" +
                    "  \"title\": \"" + title + "\",\n" +
                    "  \"description\": \"" + description + "\",\n" +
                    "  \"icon\": \"" + icon + "\",\n" +
                    "  \"tags\": [\n" +
                    "  ],\n" +
                    "  \"show\": \"CAN_APP_" + appName.toUpperCase() + "\",\n" +
                    "  \"editions\": [\"basic\", \"enterprise\"]\n" +
                    "}";

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
                "     pi new object - Creates a new object.\n" +
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

            // If only one app exists, return this.
            if (appFolders.size() == 1) {
                return appFolders.get(0).getName();
            }
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
