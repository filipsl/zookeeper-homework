import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainWatcher implements Watcher, Runnable {

    private final String znode;
    private final String[] applicationParams;
    private final TreePrinter treePrinter;
    private boolean running = true;
    private ZooKeeper zk;
    private Process externalApp;

    public MainWatcher(String connectionString, String znode, String[] applicationParams) throws IOException {
        this.znode = znode;
        this.applicationParams = applicationParams;
        this.zk = new ZooKeeper(connectionString, 60000, this);
        this.treePrinter = new TreePrinter(zk, znode);
    }

    @Override
    public void run() {
        try {
            if (zk.exists(znode, this) != null) {
                runExternalApp();
                App.synchronizedPrintln("Current descendants count: " + getDescendantsCount(this));
            }
            synchronized (this) {
                while (running) {
                    zk.exists(znode, this);
                    wait();
                }
            }
        } catch (InterruptedException | KeeperException e) {
            App.synchronizedPrintln("MainWatcher error");
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.NodeCreated) {
            try {
                if (watchedEvent.getPath().equals(znode)) {
                    runExternalApp();
                }
                zk.getChildren(watchedEvent.getPath(), this);
                App.synchronizedPrintln("Current descendants count: " + getDescendantsCount(null));
            } catch (KeeperException | InterruptedException e) {
                App.synchronizedPrintln("Event.EventType.NodeCreated case encountered an exception");
            }
        } else if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
            if (watchedEvent.getPath().equals(znode)) {
                killExternalApp();
            }
        } else if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
            App.synchronizedPrintln("Current descendants count: " + getDescendantsCount(null));
            try {
                List<String> children = zk.getChildren(watchedEvent.getPath(), this);
                for (String child : children) {
                    String childPath = watchedEvent.getPath() + "/" + child;
                    zk.exists(childPath, this);
                    zk.getChildren(childPath, this);
                }
            } catch (KeeperException e) {
                //Ignore this, thrown when node has no children
            } catch (Exception e) {
                App.synchronizedPrintln("Error occurred accessing children");
            }
        }

        synchronized (this) {
            notifyAll();
        }
    }

    public int getDescendantsCount(Watcher watcher) {
        int counter = 0;
        String currentNode;

        try {
            if (zk.exists(znode, watcher) != null) {
                List<String> childrenPaths = new LinkedList<>();
                childrenPaths.add(znode);
                while (!childrenPaths.isEmpty()) {
                    currentNode = childrenPaths.get(0);
                    childrenPaths.remove(0);
                    try {
                        List<String> currentNodeChildren = zk.getChildren(currentNode, watcher);
                        for (String child : currentNodeChildren) {
                            childrenPaths.add(currentNode + "/" + child);
                            counter++;
                        }
                    } catch (KeeperException e) {
                        //Ignore this, thrown when node has no children
                    } catch (Exception e) {
                        App.synchronizedPrintln("Error occurred in obtaining child count");
                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            App.synchronizedPrintln("Cannot get descendants count");
        }

        return counter;
    }

    public void close() {
        this.running = false;
        this.killExternalApp();
        synchronized (this) {
            notifyAll();
        }
    }

    private void runExternalApp() {
        try {
            if (this.externalApp == null) {
                App.synchronizedPrintln("Opening external application");
                this.externalApp = Runtime.getRuntime().exec(applicationParams);
            }
        } catch (IOException e) {
            App.synchronizedPrintln("Error occurred when opening application");
        }
    }

    private void killExternalApp() {
        if (this.externalApp != null) {
            App.synchronizedPrintln("Killing external application");
            this.externalApp.destroy();
            try {
                this.externalApp.waitFor();
                this.externalApp = null;
            } catch (InterruptedException e) {
                App.synchronizedPrintln("Error occurred when closing application");
            }
        }
    }

    public void printTree(boolean full) {
        treePrinter.printTree(full);
    }
}
