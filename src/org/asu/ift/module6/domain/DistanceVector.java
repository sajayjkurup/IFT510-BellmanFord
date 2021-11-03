package org.asu.ift.module6.domain;

import java.util.HashMap;

/**
 * Represents distance vector to Router (identified by id) and the cost to that router
 */
public class DistanceVector extends HashMap<String, Integer> {

    private String originatedRouterId;

    public String getOriginatedRouterId() {
        return originatedRouterId;
    }

    public void setOriginatedRouterId(String originatedRouterId) {
        this.originatedRouterId = originatedRouterId;
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" [ ");
        keySet().stream().forEach(key -> {
            stringBuilder.append(key+"="+get(key)+" ");
        });
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

}
