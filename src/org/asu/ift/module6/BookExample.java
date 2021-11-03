package org.asu.ift.module6;

import org.asu.ift.module6.domain.Router;

import java.util.Arrays;
import java.util.List;

/**
 * Testing the example in the book. Fig 5.6
 */
public class BookExample {

    public static void main(String[] args) {

        Router x = new Router("x");
        Router y = new Router("y");
        Router z = new Router("z");

        //String[] allNodesArr = new String[]{"u","v","w","x","y","z"};
        String[] allNodesArr = new String[]{"x","y","z"};
        final List<String> allNodes = Arrays.asList(allNodesArr);
        x.registerAllNodesInNetwork(allNodes);
        y.registerAllNodesInNetwork(allNodes);
        z.registerAllNodesInNetwork(allNodes);

        x.addNeighbour(y,2);
        x.addNeighbour(z,7);

        y.addNeighbour(x, 2);
        y.addNeighbour(z, 1);

        z.addNeighbour(x,7);
        z.addNeighbour(y,1);

        z.start();
        y.start();
        x.start();
    }
}
