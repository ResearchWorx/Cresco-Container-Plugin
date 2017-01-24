import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.*;

import java.util.*;

/**
 * Created by vcbumg2 on 1/19/17.
 */
public class DockerEngine {

    private DockerClient docker;
    private List<String> container_ids;

    private long memAve = -1;
    //private long workloadCpuAve = -1;
    //private long systemCpuAve = -1;
    private double cpuAve = -1;
    private int samples = 0;

    public ResourceMetric getResourceMetric(String container_id) {
        ResourceMetric metric = null;
        try {

            ContainerInfo info = docker.inspectContainer(container_id);
            long runTime = (System.currentTimeMillis() - info.state().startedAt().getTime())/1000;

            ContainerStats stats = docker.stats(container_id);
            //USER_HZ is typically 1/100
            //long cpuTotal = stats.cpuStats().cpuUsage().totalUsage() / 100;

            long workloadCpuDelta = (stats.cpuStats().cpuUsage().totalUsage() - stats.precpuStats().cpuUsage().totalUsage()) /100;
            long systemCpuDelta = (stats.cpuStats().systemCpuUsage() - stats.precpuStats().systemCpuUsage()) / 100;

            //System.out.println("containerDelta=" + workloadCpuDelta);
            //System.out.println("system delta=" + systemCpuDelta);

            /*
            if(workloadCpuAve == -1) {
                workloadCpuAve = workloadCpuDelta;
            }
            else {
                workloadCpuAve = (workloadCpuAve + workloadCpuDelta)/2;
            }

            systemCpuAve = systemCpuDelta;
            */

            if(cpuAve == -1) {
                    cpuAve = ((((double)workloadCpuDelta /(double)systemCpuDelta) * 100) + cpuAve);

            }
            else {
                    cpuAve = ((((double)workloadCpuDelta /(double)systemCpuDelta) * 100) + cpuAve)/2;
            }
            //System.out.println("cpuAve=" + cpuAve);
            /*
            if(systemCpuAve == -1) {
                systemCpuAve = systemCpuDelta;
            }
            else {
                systemCpuAve = (systemCpuAve + systemCpuDelta)/2;
            }
            */
            //long cpuDeltaAve = cpuDelta/100;
            //System.out.println(cpuTotal + " " + cpuDeltaAve + " " + systemDelta);
            /*
            if(cpuAve == -1) {
                if(systemDelta == 0) {
                    cpuAve = 0.0;
                }
                else {
                    cpuAve = ((((double)cpuDelta /(double)systemDelta) * 100) + cpuAve);
                }

            }
            else {
                if(systemDelta == 0) {
                    cpuAve = (0.0 + cpuAve)/2;
                }
                else {
                    cpuAve = ((((double)cpuDelta /(double)systemDelta) * 100) + cpuAve)/2;
                }
            }
            */

            long memCurrent = stats.memoryStats().usage();


            if(memAve == -1) {
                memAve = stats.memoryStats().usage();
            }
            else {
                memAve = (memAve + stats.memoryStats().usage())/2;
            }


            long memLimit = stats.memoryStats().limit();
            long memMax = stats.memoryStats().maxUsage();

            List<Object> blockIo = stats.blockIoStats().ioServiceBytesRecursive();

            long bRead = 0;
            long bWrite = 0;
            long bSync = 0;
            long bAsync = 0;
            long bTotal = 0;

            for(Object obj : blockIo) {
                LinkedHashMap<String, String> lhmap = (LinkedHashMap<String, String>) obj;
                String op = lhmap.get("op");

                long biocount = Long.parseLong(String.valueOf(lhmap.get("value")));

                switch (op) {
                    case "Read":
                        bRead = biocount + bRead;
                        break;
                    case "Write":
                        bWrite = biocount + bWrite;
                        break;
                    case "Sync":
                        bSync = biocount + bSync;
                        break;
                    case "Async":
                        bAsync = biocount + bAsync;
                        break;
                    case "Total":
                        bTotal = biocount + bTotal;
                        break;
                }
            }

            Map<String, NetworkStats> networkIo = stats.networks();

            long rxBytes = 0;
            long rxPackets = 0;
            long rxDropped = 0;
            long rxErrors = 0;
            long txBytes = 0;
            long txPackets = 0;
            long txDropped = 0;
            long txErrors = 0;

            for (Map.Entry<String, NetworkStats> entry : networkIo.entrySet()) {
                rxBytes += entry.getValue().rxBytes();
                rxPackets += entry.getValue().rxPackets();
                rxDropped += entry.getValue().rxDropped();
                rxErrors += entry.getValue().rxErrors();
                txBytes += entry.getValue().txBytes();
                txPackets += entry.getValue().txPackets();
                txDropped += entry.getValue().txDropped();
                txErrors += entry.getValue().txErrors();
            }
            //long runTime, long cpuTotal, long memCurrent, long memAve, long memLimit,
            // long memMax, long diskReadTotal, long diskWriteTotal, long networkRxTotal, long networkTxTotal

            metric = new ResourceMetric(container_id + "-" + String.valueOf(runTime), runTime, cpuAve, memCurrent, memAve, memLimit, memMax, bRead, bWrite, rxBytes, txBytes);
            samples++;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return metric;
    }


    public void getStats(String container_id) {
        try {

            ContainerInfo info = docker.inspectContainer(container_id);
            //long startTime = info.state().startedAt().getTime();
            long runTime = (System.currentTimeMillis() - info.state().startedAt().getTime())/1000;
            System.out.println("RunTime = " + runTime);

            ContainerStats stats = docker.stats(container_id);

            //System.out.println("User Usage: " + stats.cpuStats().cpuUsage().usageInUsermode().toString());
            //System.out.println("Kernel Usage: " + stats.cpuStats().cpuUsage().usageInKernelmode().toString());
            //System.out.println("Total Usage: " + stats.cpuStats().cpuUsage().totalUsage());
            //System.out.println("System Usage: " + stats.cpuStats().systemCpuUsage());

            long runTimeSec = 0;
            try {
                long cpuTime = stats.cpuStats().cpuUsage().totalUsage() / 100;
                runTimeSec = cpuTime / runTime;
            }
            catch(Exception ee) {
                //divide zero eat
            }
            System.out.println("CPUS Usage/sec: " + runTimeSec);

            System.out.println("--");

            //System.out.println("Usage MEM: " + stats.memoryStats().usage());
            long mUsed = 0;
            try {
                mUsed =  stats.memoryStats().usage()/runTime;
            }
            catch(Exception ee) {
                //divide zero eat
            }
            long mMax = stats.memoryStats().maxUsage();
            long mLimit = stats.memoryStats().limit();

            System.out.println("Used MEM: " + mUsed);
            System.out.println("Limit MEM: " + mLimit);
            System.out.println("Max MEM: " + mMax);
            //System.out.println("Fail MEM: " + stats.memoryStats().failcnt());
            //System.out.println("Cache MEM: " + stats.memoryStats().stats().cache());

            List<Object> blockIo = stats.blockIoStats().ioServiceBytesRecursive();

            long bRead = 0;
            long bWrite = 0;
            long bSync = 0;
            long bAsync = 0;
            long bTotal = 0;

            for(Object obj : blockIo) {
                LinkedHashMap<String, String> lhmap = (LinkedHashMap<String, String>) obj;
                String op = lhmap.get("op");

                long biocount = Long.parseLong(String.valueOf(lhmap.get("value")));

                switch (op) {
                    case "Read":
                        bRead = biocount;
                        break;
                    case "Write":
                        bWrite = biocount;
                        break;
                    case "Sync":
                        bSync = biocount;
                        break;
                    case "Async":
                        bAsync = biocount;
                        break;
                    case "Total":
                        bTotal = biocount;
                        break;
                }
            }
            System.out.println("--");
            System.out.println("Disk writeBytes: " + bWrite);
            System.out.println("Disk readBytes: " + bRead);


            Map<String, NetworkStats> networkIo = stats.networks();

                long rxBytes = 0;
                long rxPackets = 0;
                long rxDropped = 0;
                long rxErrors = 0;
                long txBytes = 0;
                long txPackets = 0;
                long txDropped = 0;
                long txErrors = 0;

                for (Map.Entry<String, NetworkStats> entry : networkIo.entrySet()) {
                    rxBytes += entry.getValue().rxBytes();
                    rxPackets += entry.getValue().rxPackets();
                    rxDropped += entry.getValue().rxDropped();
                    rxErrors += entry.getValue().rxErrors();
                    txBytes += entry.getValue().txBytes();
                    txPackets += entry.getValue().txPackets();
                    txDropped += entry.getValue().txDropped();
                    txErrors += entry.getValue().txErrors();
                }
                System.out.println("--");
                System.out.println("Network rxBytes: " + rxBytes);
                System.out.println("Network txBytes: " + txBytes);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    public ContainerConfig buildContainer(String image, List<String> envList, List<String> portList) {
        ContainerConfig containerConfig = null;

        try {
            if((envList == null) && (portList == null)) {
                HostConfig hostConfig = HostConfig.builder().build();
                containerConfig = ContainerConfig.builder()
                        .hostConfig(hostConfig)
                        .image(image)
                        .build();
            }
            else if((envList != null) && (portList == null)) {
                HostConfig hostConfig = HostConfig.builder().build();
                containerConfig = ContainerConfig.builder()
                        .hostConfig(hostConfig)
                        .image(image)
                        .env(envList)
                        .build();
            }
            else if((envList == null) && (portList != null)) {

                Set<String> ports = new HashSet<>(portList);
                final Map<String, List<PortBinding>> portBindings = new HashMap<>();
                for (String port : portList) {
                    List<PortBinding> hostPorts = new ArrayList<>();
                    hostPorts.add(PortBinding.of("0.0.0.0", port));
                    portBindings.put(port, hostPorts);
                }

                HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

                containerConfig = ContainerConfig.builder()
                        .hostConfig(hostConfig)
                        .exposedPorts(ports)
                        .image(image)
                        .build();
            }
            else if((envList != null) && (portList != null)) {

                Set<String> ports = new HashSet<>(portList);
                final Map<String, List<PortBinding>> portBindings = new HashMap<>();
                for (String port : portList) {
                    List<PortBinding> hostPorts = new ArrayList<>();
                    hostPorts.add(PortBinding.of("0.0.0.0", port));
                    portBindings.put(port, hostPorts);
                }

                HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

                containerConfig = ContainerConfig.builder()
                        .hostConfig(hostConfig)
                        .exposedPorts(ports)
                        .env(envList)
                        .image(image)
                        .build();
            }

        }
        catch(Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return containerConfig;
    }

    public String createContainer(String image, List<String> envList, List<String> portList) {
        String container_id = null;
        try {

            updateImage(image);

            ContainerConfig containerConfig = buildContainer(image,envList,portList);
            ContainerCreation creation = docker.createContainer(containerConfig);
            container_id = creation.id();
            container_ids.add(container_id);

        }
        catch(Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return container_id;
    }

    public boolean rmContainer(String container_id) {
        boolean isRemoved = false;
        try {
            Thread.sleep(1000);

            if(docker.inspectContainer(container_id).state().running()) {
                // Kill container
                System.out.println(docker.inspectContainer(container_id).state().toString());
                System.out.println(docker.inspectContainer(container_id).state().running().toString());

                docker.killContainer(container_id);
            }
            // Remove container
            docker.removeContainer(container_id);

        }
        catch(Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return isRemoved;
    }

    String containerExeCmd(String container_id, String[] command) {
        String returnString = null;
        try {
            // Exec command inside running container with attached STDOUT and STDERR
            //final String[] command = {"sh", "-c", "ls"};
            ExecCreation execCreation = docker.execCreate(
                    //container_id, command, DockerClient.ExecCreateParam.attachStdout(),
                    container_id, command, DockerClient.ExecCreateParam.attachStdout(),
                    DockerClient.ExecCreateParam.attachStderr());
            LogStream output = docker.execStart(execCreation.id());
            returnString = output.readFully();

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return returnString;
    }

    public boolean updateImage(String imageName) {
        boolean isUpdated = false;
        try {
            docker.pull(imageName);
            /*
            while(!isUpdated) {
                for(Image di : docker.listImages()) {
                    System.out.println(di.id());
                    if(di.id().equals(imageName)) {
                        isUpdated = true;
                    }
                }
            }
            */

            isUpdated = true;
        }
        catch(Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return isUpdated;
    }

    boolean startContainer(String container_id) {
        boolean isStarted = false;
        try{

            // Start container
            docker.startContainer(container_id);

            // Inspect container
            //final ContainerInfo info = docker.inspectContainer(id);
            /*
            ContainerInfo info = docker.inspectContainer(container_id);

            while((!info.state().running()) || (info.state().oomKilled()) || (info.state().paused())) {
                Thread.sleep(1000);
                info = docker.inspectContainer(id);
           }
            */

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return isStarted;
    }

    public DockerEngine(String email, String username, String password, String serveraddress) {
        try {

            // Pull an image from a private repository
            // Server address defaults to "https://index.docker.io/v1/"
            RegistryAuth registryAuth = RegistryAuth.builder().email(email).username(username)
                    //.password(password).serverAddress("https://myprivateregistry.com/v1/").build();
                    .password(password).serverAddress(serveraddress).build();

            //docker.pull("foobar/busybox-private:latest", registryAuth);

            // You can also set the RegistryAuth for the DockerClient instead of passing everytime you call pull()
            docker = DefaultDockerClient.fromEnv().registryAuth(registryAuth).build();
            container_ids = new ArrayList<>();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public DockerEngine() {
        try {
            // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
            docker = DefaultDockerClient.fromEnv().build();
            container_ids = new ArrayList<>();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void shutdown() {

        try {
            for(String container_id : container_ids) {
                rmContainer(container_id);
            }

            // Close the docker client
            docker.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
