package org.asu.ift.module6.domain;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Represents a router in the network.
 */
public class Router extends Thread {

    private String id;
    private DistanceVector distanceVector = new DistanceVector();
    private List<Router> immediateNeighbours = new ArrayList<>();
    private Map<String, DistanceVector> immediateNeighboursDV = new HashMap<>();
    private List<String> allNodesInNetwork = new ArrayList<>();

    private final Integer HIGH_COST = 999;

    Object lock = new Object();

    public Router(String id){
        this.id = id;
        distanceVector.setOriginatedRouterId(id);
        distanceVector.put(id,0);
    }

    public String getRouterId() {
        return id;
    }

    /**
     * Part of router init. Register all nodes in the network.
     * @param routerIds
     */
    public void registerAllNodesInNetwork(final List<String> routerIds){
        if(routerIds == null){
            throw new IllegalArgumentException("All nodes in network cannot be null");
        }
        this.allNodesInNetwork = routerIds;
    }

    /**
     * Part of router init. Add Neighbour and cost to neighbour.
     * @param router
     * @param cost
     */
    public void addNeighbour(Router router, Integer cost){
        if(router == null){
            throw new IllegalArgumentException("Neighbour Router cannot be null");
        }
        this.immediateNeighbours.add(router);
        this.immediateNeighboursDV.put(router.getRouterId(),null);
        distanceVector.put(router.getRouterId(),cost);
    }

    public void run(){
        System.out.println("Starting Router " + id);

        /*
         * For all nodes y in N, Dx(y) = c(x,y) , infinite if not an neighbour.
         */
        allNodesInNetwork.stream().forEach(node -> {
            Optional<String> optional = immediateNeighboursDV.keySet().stream().filter(neighbour -> neighbour.equals(node)).findAny();
            if(!optional.isPresent()){
                if(node.equals(id)){
                    distanceVector.put(node,0);
                }else {
                    distanceVector.put(node, HIGH_COST);
                }
            }
        });

        /*
         * Distance Vector changed, broadcast it.
         */
        broadcastDVToNeighbours();

        /*
         * Wait for distance vector notifications from neighbour
         */
        synchronized(lock) {
            while(true) {
                try {

                    lock.wait();

                    //print routing table after update/iteration
                    prettyPrintRT();
                    //print distance vector after update/iteration
                    prettyPrintDV();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    /**
     * Get notified about neighbours distance vector update.
     * @param distanceVector
     */
    public void neighbourDVChanged(DistanceVector distanceVector){
        System.out.println("Router " + id +" | Received update from neighbour "+distanceVector.getOriginatedRouterId());

        /*
         * Save neighbours update.
         */
        this.immediateNeighboursDV.put(distanceVector.getOriginatedRouterId(),distanceVector);

        /*
         * process new distance vector
         */
        processDistanceVector(distanceVector);

        synchronized(lock) {
            lock.notifyAll();
        }
    }

    /**
     * Broadcast change in distance vector.
     * (Use a different thread to notify and not notify on Routers main thread.)
     */
    private void broadcastDVToNeighbours(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                immediateNeighbours.stream().forEach(neighbour -> neighbour.neighbourDVChanged(distanceVector));
            }
        });
        thread.start();
    }

    /*
     * process DV change notification from neighbor
     */
    private synchronized void processDistanceVector(DistanceVector neighbourDistanceVector) {
        if(neighbourDistanceVector == null){
            throw new IllegalArgumentException("Router "+id+" Distance Vector cannot be null");
        }

        if(neighbourDistanceVector.getOriginatedRouterId() == null){
            throw new IllegalArgumentException("Router "+id+" Distance Vector originated Router cannot be null");
        }

        Map<String, Integer> newDistanceVector = new HashMap<>();
        newDistanceVector.put(id,0);

        /*
         * For each y in  N:
         */
        allNodesInNetwork.stream().forEach(destNode -> {
            if(!destNode.equals(id)) {

                /*
                 * Compute c(x,v) + Dv(y)
                 */
                List<Integer> costs = immediateNeighbours.stream().map(
                        immediateNeighbour -> {
                            if(immediateNeighbour.getRouterId().equals(destNode)){
                                return distanceVector.get(immediateNeighbour.getRouterId()); // Dv(v) will be 0. hence return c(x,v)
                            }
                            if(immediateNeighboursDV.get(immediateNeighbour.getRouterId()) == null){ //DV from v not available. Assume Infinity
                                return HIGH_COST;
                            }

                            // c(x,v) + Dv(y)
                            return distanceVector.get(immediateNeighbour.getRouterId()) + immediateNeighboursDV.get(immediateNeighbour.getRouterId()).get(destNode);

                        }).collect(Collectors.toList());

                /*
                 * min(c(x,v) + Dv(y))
                 */
                Collections.sort(costs);
                Integer minimumCost = costs.get(0);

                newDistanceVector.put(destNode, minimumCost);
            }
        });

        /*
         * if DV of node changed as part of update, broadcast the change.
         */
        if(!newDistanceVector.equals(distanceVector)) {
            distanceVector.putAll(newDistanceVector);
            broadcastDVToNeighbours();
        }
    }

    @Override
    public String toString(){
        return id;
    }

    /**
     * Pretty Print the Routing Table
     */
    private void prettyPrintRT(){
        StringBuilder builder = new StringBuilder();
        builder.append("------------------- Routing Table for Router "+id.toUpperCase()+" -----------------------\n");
        immediateNeighboursDV.keySet().stream().forEach(routerId -> {
            builder.append(""+routerId.toUpperCase() + " : "+immediateNeighboursDV.get(routerId)+"\n");
        });
        builder.append("----------------------------------------------------------------------\n");
        System.out.println(builder);

    }

    /**
     * Pretty Print the Distance Vector.
     */
    private void prettyPrintDV(){
        StringBuilder builder = new StringBuilder();
        builder.append("------------------- Distance Vector for Router "+id.toUpperCase()+" -------------------------------------\n");
        distanceVector.keySet().stream().forEach(routerId -> {
            builder.append(id+" -> "+routerId + " : cost : "+distanceVector.get(routerId)+"\n");
        });
        builder.append("-------------------------------------------------------------------------------------\n");
        System.out.println(builder);
    }
}