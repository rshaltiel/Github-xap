package com.gigaspaces.internal.cluster;

import java.util.Map;

public class UndeployedPuMetaData {
    private String puName;
    private String unDeployedAt;
    private boolean isPersistent;
    private Map<Integer, String> lastPrimaryPerPartition;
    private Map<String, String> spaceInstancesHosts;
    private String schema;
    private int numOfInstances;
    private int numOfBackups;
    private boolean gracefulShutdown;
    
    public UndeployedPuMetaData() {
    }

    public UndeployedPuMetaData(String puName, String unDeployedAt, boolean isPersistent, Map<Integer,
                                String> lastPrimaryPerPartition, Map<String, String> spaceInstancesHosts,
                                String schema, int numOfInstances, int numOfBackups, boolean gracefulShutdown) {
        this.puName = puName;
        this.unDeployedAt = unDeployedAt;
        this.isPersistent = isPersistent;
        this.lastPrimaryPerPartition = lastPrimaryPerPartition;
        this.spaceInstancesHosts = spaceInstancesHosts;
        this.schema = schema;
        this.numOfInstances = numOfInstances;
        this.numOfBackups = numOfBackups;
        this.gracefulShutdown = gracefulShutdown;
    }

    public String getPuName() {
        return puName;
    }

    public void setPuName(String puName) {
        this.puName = puName;
    }

    public String getUnDeployedAt() {
        return unDeployedAt;
    }

    public void setUnDeployedAt(String unDeployedAt) {
        this.unDeployedAt = unDeployedAt;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public Map<Integer, String> getLastPrimaryPerPartition() {
        return lastPrimaryPerPartition;
    }

    public void setLastPrimaryPerPartition(Map<Integer, String> lastPrimaryPerPartition) {
        this.lastPrimaryPerPartition = lastPrimaryPerPartition;
    }

    public void setPersistent(boolean persistent) {
        isPersistent = persistent;
    }

    public Map<String, String> getSpaceInstancesHosts() {
        return spaceInstancesHosts;
    }

    public void setSpaceInstancesHosts(Map<String, String> spaceInstancesHosts) {
        this.spaceInstancesHosts = spaceInstancesHosts;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getNumOfInstances() {
        return numOfInstances;
    }

    public void setNumOfInstances(int numOfInstances) {
        this.numOfInstances = numOfInstances;
    }

    public int getNumOfBackups() {
        return numOfBackups;
    }

    public void setNumOfBackups(int numOfBackups) {
        this.numOfBackups = numOfBackups;
    }

    public boolean isGracefulShutdown() {
        return gracefulShutdown;
    }

    public void setGracefulShutdown(boolean gracefulShutdown) {
        this.gracefulShutdown = gracefulShutdown;
    }

    @Override
    public String toString() {
        return "UndeployedPuMetaData{" +
                "puName='" + puName + '\'' +
                ", unDeployedAt='" + unDeployedAt + '\'' +
                ", isPersistent=" + isPersistent +
                ", lastPrimaryPerPartition=" + lastPrimaryPerPartition +
                ", spaceInstancesHosts=" + spaceInstancesHosts +
                ", schema='" + schema + '\'' +
                ", numOfInstances=" + numOfInstances +
                ", numOfBackups=" + numOfBackups +
                ", gracefulShutDown=" + gracefulShutdown +
                '}';
    }
}