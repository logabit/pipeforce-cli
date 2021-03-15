package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all (stream) outputs like writing to the console or saving / reading files.
 *
 * @author sniederm
 * @since 2.0
 */
public class OutputCliService implements CliContextAware {

    private AnimationRunnable runnable;
    private CliContext context;

    public void printResult(Object result) {

        if (result instanceof Exception) {
            Exception e = (Exception) result;
            Map error = ListUtil.asLinkedMap(
                    "status", "error",
                    "message", e.getMessage() + "",
                    "errorType", e.getClass().getSimpleName(),
                    "errorTimestamp", System.currentTimeMillis(),
                    "errorUuid", UUID.randomUUID().toString());

            wrapToRootAndPrint(error);
            e.printStackTrace();
            return;
        }

        String outputSwitch = context.getArgs().getSwitch("-o");
        if (StringUtil.isEqual(outputSwitch, "plain")) {
            System.out.println(result); // Plain text as it is
        } else {
            Map status = ListUtil.asLinkedMap("value", result);
            wrapToRootAndPrint(status);
        }
        return;
    }

    private void wrapToRootAndPrint(Map map) {

        Map resultRoot = ListUtil.asLinkedMap("result", map);
        System.out.println(JsonUtil.objectToYamlString(resultRoot));
    }

    /**
     * Starts a indetermined progress on the console.
     *
     * @param message
     */
    public void showProgress(String message) {

        if (this.runnable != null) {
            return;
        }

        this.runnable = new AnimationRunnable(message);
        Thread th = new Thread(runnable, "Progress-Animation");
        th.start();
    }

    /**
     * Stop a indetermined progress on the console.
     *
     * @param
     */
    public void stopProgress() {

        if (this.runnable == null) {
            return;
        }

        this.runnable.doStop();
        while (!runnable.isStopped()) {
            try {
                Thread.sleep(10); // Wait to be stopped to not write to wrong console output line
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.runnable = null;
    }

    public void println(String s) {
        System.out.println(s);
    }

    public void println() {
        System.out.println();
    }

    public void print(String s) {
        System.out.print(s);
    }

    /**
     * For easier mocking in tests, use this to create file output streams.
     *
     * @param file
     * @return
     */
    public OutputStream createOutputStream(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveByteArrayToFile(byte[] data, File file) {
        FileUtil.saveByteArrayToFile(data, file);
    }

    public void saveStringToFile(String value, File file) {
        FileUtil.saveStringToFile(value, file);
    }

    public String readFileToString(String path) {
        return FileUtil.readFileToString(path);
    }

    @Override
    public void setContext(CliContext context) {
        this.context = context;
    }

    /**
     * Animation Runnable
     */
    public class AnimationRunnable implements Runnable {

        private final String message;
        private boolean doStop = false;
        private boolean stopped = false;
        private String lastLine = "";

        public AnimationRunnable(String message) {
            this.message = message;
        }

        public synchronized void doStop() {
            this.doStop = true;
        }

        public synchronized boolean isStopped() {
            return stopped;
        }

        @Override
        public void run() {

            for (int i = 0; i < 20; i++) {

                if (doStop) {
                    deleteLine();
                    System.out.print("\r"); // Put cursor at the very beginning
                    stopped = true;
                    return;
                }

                animate(message);

                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void print(String line) {

            //clear the last line
            deleteLine();

            // Output new line from the very beginning
            System.out.print("\r" + line);
            lastLine = line;
        }

        public void deleteLine() {
            String temp = "";
            for (int i = 0; i < lastLine.length(); i++) {
                temp += " ";
            }
            if (temp.length() > 1) {
                System.out.print("\r" + temp);
            }
        }

        private byte anim;

        public void animate(String line) {
            switch (anim) {
                case 1:
                    print(line + "  \\  ");
                    break;
                case 2:
                    print(line + "  |  ");
                    break;
                case 3:
                    print(line + "  /  ");
                    break;
                default:
                    anim = 0;
                    print(line + "  -  ");
            }
            anim++;
        }
    }
}
