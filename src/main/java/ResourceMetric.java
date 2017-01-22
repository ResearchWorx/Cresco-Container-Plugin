public class ResourceMetric {

    private String INodeId;
    private long runTime;
    private long cpuCurrent;
    private long cpuTotal;
    private long memCurrent;
    private long memAve;
    private long memLimit;
    private long memMax;
    private long diskReadTotal;
    private long diskWriteTotal;
    private long networkRxTotal;
    private long networkTxTotal;
    private double[] points;

    public ResourceMetric(String INodeId, long runTime, long cpuCurrent, long cpuTotal, long memCurrent, long memAve, long memLimit, long memMax, long diskReadTotal, long diskWriteTotal, long networkRxTotal, long networkTxTotal) {
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

    public void setINodeId(String newINodeId) {
        try {
            INodeId = newINodeId;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public long getRuntime() {
        return runTime;
    }

    public long getCPUtotal() {
        return cpuTotal;
    }

    public long getCpuCurrent() {
        return cpuCurrent;
    }

    public long getCPUAve() {
        long cpu = 0;
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

    public long getMemAve() {
        return memAve;
    }

    public long getMemLimit() {
        return memLimit;
    }

    public long getMemMax() {
        return memMax;
    }

    public long getMemCurrent() {
        return memCurrent;
    }

    public long getDiskRead() {

        long disk = 0;
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

    public long getDiskWrite() {

        long disk = 0;
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

    public long getNetworkRx() {
        long network = 0;
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

    public long getNetworkTx() {
        long network = 0;
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

    public void addCPU(long addCpuTotal) {
        try {
            cpuTotal = (cpuTotal + addCpuTotal) / 2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addMemory(long addMemAve) {
        try {
            memAve = (memAve + addMemAve)/2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    public void addDiskRead(long addDiskReadTotal) {

        try {
            diskReadTotal = (diskReadTotal + addDiskReadTotal) / 2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addDiskWrite(long addDiskWriteTotal) {

        try {
            diskWriteTotal = (diskWriteTotal + addDiskWriteTotal) / 2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addNetworkRx(long addNetworkRxTotal) {
        try {
            networkRxTotal = (networkRxTotal + addNetworkRxTotal) / 2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addNetworkTx(long addNetworkTxTotal) {
        try {
                networkTxTotal = (networkTxTotal + addNetworkTxTotal) / 2;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getINodeId() {
        return INodeId;
    }

    @Override
    public String toString() {
        return String.format("id=" + getINodeId() + "runtime=" + getRuntime() + " cpu=" + getCPUAve() + " mem=" + getMemAve() + " diskRead=" + getDiskRead() + " diskWrite=" + getDiskWrite() + " neworkRx=" + getNetworkRx() + " networkTx=" + getNetworkTx());
    }

}