package org.asu.ift.module6;

import org.asu.ift.module6.domain.Router;

import java.util.Arrays;
import java.util.List;

/**
 * Homework Assignment
 */
public class Main {

    public static void main(String[] args) {
        Router u = new Router("u");
        Router v = new Router("v");
        Router w = new Router("w");
        Router x = new Router("x");
        Router y = new Router("y");
        Router z = new Router("z");

        String[] allNodesArr = new String[]{"u","v","w","x","y","z"};
        final List<String> allNodes = Arrays.asList(allNodesArr);
        u.registerAllNodesInNetwork(allNodes);
        v.registerAllNodesInNetwork(allNodes);
        w.registerAllNodesInNetwork(allNodes);
        x.registerAllNodesInNetwork(allNodes);
        y.registerAllNodesInNetwork(allNodes);
        z.registerAllNodesInNetwork(allNodes);

        u.addNeighbour(v,3);
        u.addNeighbour(x,1);
        u.addNeighbour(w,7);

        v.addNeighbour(u, 3);
        v.addNeighbour(x, 1);
        v.addNeighbour(w, 1);

        w.addNeighbour(u,7);
        w.addNeighbour(x,4);
        w.addNeighbour(v,1);
        w.addNeighbour(y,5);
        w.addNeighbour(z,6);

        x.addNeighbour(u,1);
        x.addNeighbour(v,1);
        x.addNeighbour(w,4);
        x.addNeighbour(y,2);

        y.addNeighbour(x,2);
        y.addNeighbour(z,3);
        y.addNeighbour(w,5);

        z.addNeighbour(w,6);
        z.addNeighbour(y,3);

        u.start();
        v.start();
        w.start();
        x.start();
        y.start();
        z.start();


    }
}
