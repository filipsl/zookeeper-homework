import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class App {

    public static String znode = "/z";
    public static MainWatcher mainWatcher;


    public static synchronized void synchronizedPrintln(String msg) {
        System.out.println(msg);
    }

    public static synchronized void synchronizedPrint(String msg) {
        System.out.print(msg);
    }

    private static void printTree(boolean full) {
        mainWatcher.printTree(full);
    }

    private static int getDescendantsCount(){
        return mainWatcher.getDescendantsCount(null);
    }

    private static void handleInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String msg = scanner.nextLine();
            if (msg.startsWith("tf")) {
                printTree(true);
            } else if (msg.startsWith("t")) {
                printTree(false);
            } else if (msg.startsWith("d")) {
                App.synchronizedPrintln("Current descendants count: " + getDescendantsCount());
            } else if (msg.startsWith("q")) {
                mainWatcher.close();
                break;
            } else if (msg.startsWith("h")) {
                App.synchronizedPrintln("Help: t - print tree (tf - for tree with full paths), d - descendants count, q - quit");
            } else {
                App.synchronizedPrintln("Unrecognized command");
            }
        }
    }

    public static void main(String[] args) {

        //Disable log4j
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.OFF);


        if (args.length < 2) {
            System.err
                    .println("Specify connectionString and application to be executed, eg. 127.0.0.1:2171 mspaint");
            return;
        }
        synchronizedPrintln("Starting app");


        String connectionString = args[0];
        String[] applicationParams = new String[args.length - 1];
        System.arraycopy(args, 1, applicationParams, 0, applicationParams.length);
        try {
            mainWatcher = new MainWatcher(connectionString, znode, applicationParams);
            new Thread(mainWatcher).start();
            synchronizedPrintln("App started! Type h for help");
            handleInput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
