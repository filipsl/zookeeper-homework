import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.util.*;

public class TreePrinter {
    private final ZooKeeper zk;
    private final String znode;

    public TreePrinter(ZooKeeper zk, String znode) {
        this.zk = zk;
        this.znode = znode;
    }

    public void printTree(boolean full) {
        String currentNode;
        Stack<List<String>> nodeListsStack = new Stack<>();
        try {
            if (zk.exists(znode, false) != null) {
                List<String> childrenPaths = new LinkedList<>();
                childrenPaths.add(znode);
                nodeListsStack.push(childrenPaths);

                while (!nodeListsStack.empty()) {
                    List<String> currentChildrenPaths = nodeListsStack.pop();
                    if(!currentChildrenPaths.isEmpty()){
                        currentNode = currentChildrenPaths.get(0);
                        currentChildrenPaths.remove(0);
                        printWithIntent(currentNode, nodeListsStack.size(), full);
                        nodeListsStack.push(currentChildrenPaths);
                        try {
                            List<String> currentNodeChildren = zk.getChildren(currentNode, false);
                            List<String> newChildrenPaths = new LinkedList<>();
                            for (String child : currentNodeChildren) {
                                newChildrenPaths.add(currentNode + "/" + child);
                            }
                            if (!newChildrenPaths.isEmpty()) {
                                nodeListsStack.push(newChildrenPaths);
                            }
                        } catch (KeeperException e) {
                            //Ignore this, thrown when node has no children
                        } catch (Exception e) {
                            App.synchronizedPrintln("Error occurred in obtaining child count");
                        }

                    }

//                    while (!currentChildrenPaths.isEmpty()) {
////                        currentNode = currentChildrenPaths.get(0);
////                        currentChildrenPaths.remove(0);
////                        printWithIntent(currentNode, nodeListsStack.size()-1);
//
//                    }
                }
            }
        } catch (KeeperException | InterruptedException e) {
            App.synchronizedPrintln("Cannot get descendants count");
        }
    }

    private void printWithIntent(String msg, int intent, boolean full) {
        for (int i = 0; i < intent; i++) {
            App.synchronizedPrint("|   ");
        }
        if(!full){
            int last_slash = msg.lastIndexOf("/");
            msg = msg.substring(last_slash);
        }
        App.synchronizedPrint(msg + "\n");
    }
}
