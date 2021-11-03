package org.asu.ift.module6.domain;

import com.sun.org.apache.xml.internal.utils.Hashtree2Node;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Represents a router in the network.
 */
public class Router extends Thread {

    private String id;
    private DistanceVector distanceVector = new DistanceVector();
    private List<Router> immediateNeighbours = new ArrayList<>();
    private Map<String,Integer>  immediateNeighbourCost = Collections.synchronizedMap(new HashMap<>());
    private Map<String, DistanceVector> immediateNeighboursDV = Collections.synchronizedMap(new HashMap<>()); ;
    private List<String> allNodesInNetwork = new ArrayList<>();
    private Map<String,String> nextHopRouter = Collections.synchronizedMap(new HashMap<>());

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
        immediateNeighbourCost.put(router.getRouterId(),cost); //save original costs
        distanceVector.put(router.getRouterId(),cost); // initialize distance vector for router
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
                    prettyPrintNextHopRouter();
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
    public synchronized void neighbourDVChanged(DistanceVector distanceVector){
        /*
         * Save neighbours update.
         */
        this.immediateNeighboursDV.put(distanceVector.getOriginatedRouterId(),distanceVector);

        /*
         * process new distance vector
         */
        processDistanceVectorChange();
        synchronized(lock) {
            lock.notifyAll();
        }
    }

    /**
     * Broadcast change in distance vector.
     * (Use a different thread to notify and not notify on Routers main thread.)
     */
    private synchronized void broadcastDVToNeighbours(){
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
    private synchronized void processDistanceVectorChange() {
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
                List<RouterAndCost> costs = immediateNeighbours.stream().map(
                        immediateNeighbour -> {
                            RouterAndCost routerAndCost = new RouterAndCost();
                            routerAndCost.routerId = immediateNeighbour.getRouterId();

                            if(immediateNeighbour.getRouterId().equals(destNode)){
                                routerAndCost.cost = immediateNeighbourCost.get(immediateNeighbour.getRouterId()); // Dv(v) will be 0. hence return c(x,v)
                                return routerAndCost;
                            }
                            if(immediateNeighboursDV.get(immediateNeighbour.getRouterId()) == null){ //DV from v not available. Assume Infinity
                                routerAndCost.cost = HIGH_COST;
                                return routerAndCost;
                            }

                            // c(x,v) + Dv(y)
                            routerAndCost.cost = immediateNeighbourCost.get(immediateNeighbour.getRouterId()) + immediateNeighboursDV.get(immediateNeighbour.getRouterId()).get(destNode);
                            return routerAndCost;
                        }).collect(Collectors.toList());

                /*
                 * min(c(x,v) + Dv(y))
                 */
                Collections.sort(costs);
                RouterAndCost minimumRouterAndCost = costs.get(0);
                Integer minimumCost = minimumRouterAndCost.cost;
                nextHopRouter.put(destNode,minimumRouterAndCost.routerId);
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
        builder.append("\n------------------- Routing Table for Router "+id.toUpperCase()+" -----------------------\n");
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
        builder.append("\n------------------- Distance Vector for Router "+id.toUpperCase()+" -------------------------------------\n");
        distanceVector.keySet().stream().forEach(routerId -> {
            builder.append(id+" -> "+routerId + " : cost : "+distanceVector.get(routerId)+"\n");
        });
        builder.append("-------------------------------------------------------------------------------------\n");
        System.out.println(builder);
    }

    private void prettyPrintNextHopRouter(){
        StringBuilder builder = new StringBuilder();
        builder.append("\n------------------- Forwarding Table for Router "+id.toUpperCase()+" -------------------------------------\n");
        allNodesInNetwork.stream().forEach(routerId -> {
            String nextHopRouterId = nextHopRouter.get(routerId);
            if(nextHopRouterId== null){
                nextHopRouterId = routerId;
            }
            builder.append(id+" -> "+routerId + " : Next Hop Router : "+nextHopRouterId+". Total cost on path "+distanceVector.get(routerId)+"\n");
        });
        builder.append("-------------------------------------------------------------------------------------\n");
        System.out.println(builder);
    }

    

    class RouterAndCost implements Comparable<RouterAndCost>{
        Integer cost;
        String routerId;

        @Override
        public int compareTo(RouterAndCost o) {
            return cost.compareTo(o.cost);
        }

        @Override
        public String toString(){
            return "["+routerId+"  "+cost+"]";
        }
    }
}