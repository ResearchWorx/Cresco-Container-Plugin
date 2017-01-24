
public class ResourceMetric {

    private String INodeId;
    private long runTime;
    private double cpuAve;
    private long memCurrent;
    private long memAve;
    private long memLimit;
    private long memMax;
    private long diskReadTotal;
    private long diskWriteTotal;
    private long networkRxTotal;
    private long networkTxTotal;
    private double workloadUtil;

    public ResourceMetric(String INodeId, long runTime, double cpuAve, long memCurrent, long memAve, long memLimit, long memMax, long diskReadTotal, long diskWriteTotal, long networkRxTotal, long networkTxTotal) {
        this.INodeId = INodeId;
        this.runTime = runTime;
        this.cpuAve = cpuAve;
        this.memCurrent = memCurrent;
        this.memAve = memAve;
        this.memLimit = memLimit;
        this.memMax = memMax;
        this.diskReadTotal = diskReadTotal;
        this.diskWriteTotal = diskWriteTotal;
        this.networkRxTotal = networkRxTotal;
        this.networkTxTotal = networkTxTotal;
        workloadUtil = -1;
    }

    public ResourceMetric(String INodeId, double workloadUtil) {
        this.INodeId = INodeId;
        this.workloadUtil = workloadUtil;
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

    public double getWorkloadUtil() {
        return workloadUtil;
    }

    public double getCpuAve() {
        return cpuAve;
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
                disk = diskWriteTotal / runTime;
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

    public void addWorkloadCost(double addWorkloadUtil) {
        try {
            if(workloadUtil == -1) {
                workloadUtil = addWorkloadUtil;
            }
            else {
                workloadUtil = (workloadUtil + addWorkloadUtil) / 2;
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addCpuAve(double addCpuAve) {
        try {
            cpuAve = (cpuAve + addCpuAve) / 2;
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
        return String.format("id=" + getINodeId() + "runtime=" + getRuntime() + " workloadcost=" + getWorkloadUtil() + " cpuave=" + getCpuAve() + " memave=" + getMemAve() + " memmax=" + getMemMax() +  " diskRead=" + getDiskRead() + " diskWrite=" + getDiskWrite() + " neworkRx=" + getNetworkRx() + " networkTx=" + getNetworkTx());
    }

}