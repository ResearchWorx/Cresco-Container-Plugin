
public class ResourceMetric {

    private String INodeId;
    private long runTime;
    private long cpuTotal;
    private long memCurrent;
    private long memAve;
    private long memLimit;
    private long memMax;
    private long diskReadTotal;
    private long diskWriteTotal;
    private long networkRxTotal;
    private long networkTxTotal;

    public ResourceMetric(String INodeId, long runTime, long cpuTotal, long memCurrent, long memAve, long memLimit, long memMax, long diskReadTotal, long diskWriteTotal, long networkRxTotal, long networkTxTotal) {
        this.INodeId = INodeId;
        this.runTime = runTime;
        this.cpuTotal = cpuTotal;
        this.memCurrent = memCurrent;
        this.memAve = memAve;
        this.memLimit = memLimit;
        this.memMax = memMax;
        this.diskReadTotal = diskReadTotal;
        this.diskWriteTotal = diskWriteTotal;
        this.networkRxTotal = networkRxTotal;
        this.networkTxTotal = networkTxTotal;
    }


    public double getCPU() {
        double cpu = 0.0;
        try {
            if(runTime != 0) {
                cpu = cpuTotal / runTime;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return cpu;
    }

    public double getMemory() {
        double memory = 0.0;
        try {
            memory = memAve;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return memory;
    }

    public double getDiskRead() {

        double disk = 0.0;
        try {
            if(runTime != 0) {
                disk = diskReadTotal / runTime;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return disk;

    }

    public double getWriteRead() {

        double disk = 0.0;
        try {
            if(runTime != 0) {
                disk = diskReadTotal / runTime;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return disk;

    }

    public double getNetworkRx() {
        double network = 0.0;
        try {
            if(runTime != 0) {
                network = networkRxTotal / runTime;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return network;
    }

    public double getNetworkTx() {
        double network = 0.0;
        try {
            if(runTime != 0) {
                network = networkTxTotal / runTime;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return network;
    }

    public String getINodeId() {
        return INodeId;
    }
}