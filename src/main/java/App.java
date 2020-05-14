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

    private static void handleInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String msg = scanner.nextLine();
            if (msg.startsWith("tf")) {
                printTree(true);
            } else if (msg.startsWith("t")) {
                printTree(false);
            } else if (msg.startsWith("q")) {
                mainWatcher.close();
                break;
            } else {
                App.synchronizedPrintln("Unrecognized command");
            }
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err
                    .println("Specify connectionString and application to be executed, eg. 127.0.0.1:2171 mspaint");
            return;
        }

        String connectionString = args[0];
        String[] applicationParams = new String[args.length - 1];
        System.arraycopy(args, 1, applicationParams, 0, applicationParams.length);
        try {
            mainWatcher = new MainWatcher(connectionString, znode, applicationParams);
            new Thread(mainWatcher).start();

            handleInput();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
