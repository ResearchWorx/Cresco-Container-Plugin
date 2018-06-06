import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.researchworx.cresco.library.plugin.core.CPlugin;
import com.researchworx.cresco.library.utilities.CLogger;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

/**
 * Created by vcbumg2 on 1/19/17.
 */
public class DockerEngine {

    private CLogger logger;
    private Plugin plugin;

    private DockerClient docker;
    private List<String> container_ids;

    public String containerImage;

    private DistributionSummary cpuUsage;
    private AtomicLong runTime = new AtomicLong(0);

    private DistributionSummary memCurrent;

    private AtomicLong memLimit = new AtomicLong(0);
    private AtomicLong memMax = new AtomicLong(0);

    private DistributionSummary bRead;
    private DistributionSummary bWrite;
    private DistributionSummary bSync;
    private DistributionSummary bAsync;
    private DistributionSummary bTotal;

    private Gson gson;

    //private AtomicLong rxBytes = new AtomicLong(0);
    //private AtomicLong rxPackets = new AtomicLong(0);
    private AtomicLong rxDropped = new AtomicLong(0);
    private AtomicLong rxErrors = new AtomicLong(0);
    //private AtomicLong txBytes = new AtomicLong(0);
    //private AtomicLong txPackets = new AtomicLong(0);
    private AtomicLong txDropped = new AtomicLong(0);
    private AtomicLong txErrors = new AtomicLong(0);


    private DistributionSummary rxBytes;
    private DistributionSummary rxPackets;
    private DistributionSummary txBytes;
    private DistributionSummary txPackets;



    Boolean isMetricInit = false;


    public ResourceMetric getResourceMetric(String container_id) {
        ResourceMetric metric = null;
        try {


            ContainerInfo info = docker.inspectContainer(container_id);
            //long runTime = (System.currentTimeMillis() - info.state().startedAt().getTime())/1000;
            runTime.set(((System.currentTimeMillis() - info.state().startedAt().getTime())/1000));


            ContainerStats stats = docker.stats(container_id);
            //USER_HZ is typically 1/100
            //long cpuTotal = stats.cpuStats().cpuUsage().totalUsage() / 100;

            long workloadCpuDelta = (stats.cpuStats().cpuUsage().totalUsage() - stats.precpuStats().cpuUsage().totalUsage()) /100;
            long systemCpuDelta = (stats.cpuStats().systemCpuUsage() - stats.precpuStats().systemCpuUsage()) / 100;

            cpuUsage.record(((((double)workloadCpuDelta /(double)systemCpuDelta) * 100)));

            memCurrent.record(stats.memoryStats().usage());
            memLimit.set(stats.memoryStats().limit());
            memMax.set(stats.memoryStats().maxUsage());


            List<Object> blockIo = stats.blockIoStats().ioServiceBytesRecursive();

            long tbRead = 0;
            long tbWrite = 0;
            long tbSync = 0;
            long tbAsync = 0;
            long tbTotal = 0;

            for(Object obj : blockIo) {
                LinkedHashMap<String, String> lhmap = (LinkedHashMap<String, String>) obj;
                String op = lhmap.get("op");

                long biocount = Long.parseLong(String.valueOf(lhmap.get("value")));

                switch (op) {
                    case "Read":
                        tbRead = biocount + tbRead;
                        break;
                    case "Write":
                        tbWrite = biocount + tbWrite;
                        break;
                    case "Sync":
                        tbSync = biocount + tbSync;
                        break;
                    case "Async":
                        tbAsync = biocount + tbAsync;
                        break;
                    case "Total":
                        tbTotal = biocount + tbTotal;
                        break;
                }
            }

            bRead.record(tbRead);
            bWrite.record(tbWrite);
            bSync.record(tbSync);
            bAsync.record(tbAsync);
            bTotal.record(tbTotal);

            Map<String, NetworkStats> networkIo = stats.networks();

            long trxBytes = 0;
            long trxPackets = 0;
            long trxDropped = 0;
            long trxErrors = 0;
            long ttxBytes = 0;
            long ttxPackets = 0;
            long ttxDropped = 0;
            long ttxErrors = 0;

            for (Map.Entry<String, NetworkStats> entry : networkIo.entrySet()) {
                trxBytes += entry.getValue().rxBytes();
                trxPackets += entry.getValue().rxPackets();
                trxDropped += entry.getValue().rxDropped();
                trxErrors += entry.getValue().rxErrors();
                ttxBytes += entry.getValue().txBytes();
                ttxPackets += entry.getValue().txPackets();
                ttxDropped += entry.getValue().txDropped();
                ttxErrors += entry.getValue().txErrors();
            }

            rxBytes.record(trxBytes);
            rxPackets.record(trxPackets);
            rxDropped.set(trxDropped);
            rxErrors.set(trxErrors);
            txBytes.record(ttxBytes);
            txPackets.record(ttxPackets);
            txDropped.set(ttxDropped);
            txErrors.set(ttxErrors);

            //long runTime, long cpuTotal, long memCurrent, long memAve, long memLimit,
            // long memMax, long diskReadTotal, long diskWriteTotal, long networkRxTotal, long networkTxTotal

            metric = new ResourceMetric(runTime.get(), cpuUsage.mean(), memCurrent.count(),(long)memCurrent.mean() , memLimit.get(), memMax.get(), (long)bRead.mean(), (long)bWrite.mean(), (long)rxBytes.mean(), (long)txBytes.mean());
            //samples++;

            /*
            for (Map.Entry<String, CMetric> entry : plugin.metricMap.entrySet()) {
                String key = entry.getKey();
                CMetric value = entry.getValue();
                logger.error(writeMetricMap(value).toString());
                // ...
            }
            */

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return metric;
    }

    public String getContainerInfoMap() {

        String returnStr = null;
        try {

            Map<String,List<Map<String,String>>> info = new HashMap<>();
            info.put("network",getMetricGroupList("network"));
            info.put("disk",getMetricGroupList("disk"));
            info.put("memory",getMetricGroupList("memory"));
            info.put("processor",getMetricGroupList("processor"));
            info.put("system",getMetricGroupList("system"));

            returnStr = gson.toJson(info);
            //logger.info(returnStr);


        } catch(Exception ex) {
            logger.error(ex.getMessage());
        }

        return returnStr;
    }

    public List<Map<String,String>> getMetricGroupList(String group) {
        List<Map<String,String>> returnList = null;
        try {
            returnList = new ArrayList<>();

            for (Map.Entry<String, CMetric> entry : plugin.metricMap.entrySet()) {
                CMetric metric = entry.getValue();
                if(metric.group.equals(group)) {
                    returnList.add(writeMetricMap(metric));
                }
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage());
        }
        return returnList;
    }

    public Map<String,String> writeMetricMap(CMetric metric) {

        Map<String,String> metricValueMap = null;

        try {
            metricValueMap = new HashMap<>();

            if (Meter.Type.valueOf(metric.className) == Meter.Type.GAUGE) {

                metricValueMap.put("name",metric.name);
                metricValueMap.put("class",metric.className);
                metricValueMap.put("type",metric.type.toString());
                metricValueMap.put("value",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).gauge().value()));



            } else if (Meter.Type.valueOf(metric.className) == Meter.Type.TIMER) {
                TimeUnit timeUnit = plugin.crescoMeterRegistry.get(metric.name).timer().baseTimeUnit();
                metricValueMap.put("name",metric.name);
                metricValueMap.put("class",metric.className);
                metricValueMap.put("type",metric.type.toString());
                metricValueMap.put("mean",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).timer().mean(timeUnit)));
                metricValueMap.put("max",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).timer().max(timeUnit)));
                metricValueMap.put("totaltime",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).timer().totalTime(timeUnit)));
                metricValueMap.put("count",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).timer().count()));

            } else if (Meter.Type.valueOf(metric.className) == Meter.Type.DISTRIBUTION_SUMMARY) {
                metricValueMap.put("name",metric.name);

                //metricValueMap.put("class",metric.className);
                //todo fix this once dashboard can deal with dist summary
                metricValueMap.put("class","TIMER");

                metricValueMap.put("type",metric.type.toString());
                metricValueMap.put("mean",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).summary().mean()));
                metricValueMap.put("max",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).summary().max()));
                metricValueMap.put("totaltime",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).summary().totalAmount()));
                metricValueMap.put("count",String.valueOf(plugin.crescoMeterRegistry.get(metric.name).summary().count()));
                metricValueMap.put("baseunit",plugin.crescoMeterRegistry.get(metric.name).summary().getId().getBaseUnit());
                metricValueMap.put("description",plugin.crescoMeterRegistry.get(metric.name).summary().getId().getDescription());

            } else  if (Meter.Type.valueOf(metric.className) == Meter.Type.COUNTER) {
                metricValueMap.put("name",metric.name);
                metricValueMap.put("class",metric.className);
                metricValueMap.put("type",metric.type.toString());
                try {
                    metricValueMap.put("count", String.valueOf(plugin.crescoMeterRegistry.get(metric.name).functionCounter().count()));
                } catch (Exception ex) {
                    metricValueMap.put("count", String.valueOf(plugin.crescoMeterRegistry.get(metric.name).counter().count()));
                }

            } else {
                logger.error("NO WRITER FOUND " + metric.className);
            }

        } catch(Exception ex) {
            logger.error(ex.getMessage());
        }
        return metricValueMap;
    }

    private void initMetrics() {

        plugin.metricMap.put("run.time",new CMetric("run.time","run.time","system","GAUGE"));

        plugin.crescoMeterRegistry.gauge("run.time", runTime);

        cpuUsage = DistributionSummary
                .builder("cpu.usage")
                .description("CPU Usage") // optional
                .baseUnit("percent") // optional (1)
                //.tags("region", "test") // optional
                //.scale(100) // optional (2)
                .register(plugin.crescoMeterRegistry);

        plugin.metricMap.put(cpuUsage.getId().getName(),new CMetric(cpuUsage.getId().getName(),cpuUsage.getId().getDescription(),"processor",cpuUsage.getId().getType().name()));


        memCurrent = DistributionSummary
                .builder("mem.current")
                .description("a description of what this summary does") // optional
                .baseUnit("bytes") // optional (1)
                //.tags("region", "test") // optional
                //.scale(100) // optional (2)
                .register(plugin.crescoMeterRegistry);

        plugin.metricMap.put(memCurrent.getId().getName(),new CMetric(memCurrent.getId().getName(),memCurrent.getId().getDescription(),"memory",memCurrent.getId().getType().name()));

        plugin.crescoMeterRegistry.gauge("mem.limit", memLimit);
        plugin.metricMap.put("mem.limit",new CMetric("mem.limit","mem.limit","memory","GAUGE"));


        plugin.crescoMeterRegistry.gauge("mem.max", memMax);
        plugin.metricMap.put("mem.max",new CMetric("mem.max","mem.max","memory","GAUGE"));


        bRead = DistributionSummary
                .builder("disk.bytes.read")
                .description("Number of bytes read") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(bRead.getId().getName(),new CMetric(bRead.getId().getName(),bRead.getId().getDescription(),"disk",bRead.getId().getType().name()));

        bWrite = DistributionSummary
                .builder("disk.bytes.write")
                .description("Number of bytes written") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(bWrite.getId().getName(),new CMetric(bWrite.getId().getName(),bWrite.getId().getDescription(),"disk",bWrite.getId().getType().name()));

        bSync = DistributionSummary
                .builder("disk.bytes.sync")
                .description("Number of bytes sync") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(bSync.getId().getName(),new CMetric(bSync.getId().getName(),bSync.getId().getDescription(),"disk",bSync.getId().getType().name()));

        bAsync = DistributionSummary
                .builder("disk.bytes.async")
                .description("Number of bytes async") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(bAsync.getId().getName(),new CMetric(bAsync.getId().getName(),bAsync.getId().getDescription(),"disk",bAsync.getId().getType().name()));

        bTotal = DistributionSummary
                .builder("disk.bytes.total")
                .description("Number of bytes total") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(bTotal.getId().getName(),new CMetric(bTotal.getId().getName(),bTotal.getId().getDescription(),"disk",bTotal.getId().getType().name()));

        rxBytes = DistributionSummary
                .builder("net.rx.bytes")
                .description("Number of RX bytes") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(rxBytes.getId().getName(),new CMetric(rxBytes.getId().getName(),rxBytes.getId().getDescription(),"network",rxBytes.getId().getType().name()));

        rxPackets = DistributionSummary
                .builder("net.rx.packets")
                .description("Number of RX packets") // optional
                .baseUnit("packets") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(rxPackets.getId().getName(),new CMetric(rxPackets.getId().getName(),rxPackets.getId().getDescription(),"network",rxPackets.getId().getType().name()));

        txBytes = DistributionSummary
                .builder("net.tx.bytes")
                .description("Number of TX bytes") // optional
                .baseUnit("bytes") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(txBytes.getId().getName(),new CMetric(txBytes.getId().getName(),txBytes.getId().getDescription(),"network",txBytes.getId().getType().name()));

        txPackets = DistributionSummary
                .builder("net.tx.packets")
                .description("Number of TX packets") // optional
                .baseUnit("packets") // optional (1)
                .register(plugin.crescoMeterRegistry);
        plugin.metricMap.put(txPackets.getId().getName(),new CMetric(txPackets.getId().getName(),txPackets.getId().getDescription(),"network",txPackets.getId().getType().name()));


        plugin.crescoMeterRegistry.gauge("net.rx.dropped", rxDropped);
        plugin.metricMap.put("net.rx.dropped",new CMetric("net.rx.dropped","net.rx.dropped","network","GAUGE"));

        plugin.crescoMeterRegistry.gauge("net.rx.errors", rxErrors);
        plugin.metricMap.put("net.rx.errors",new CMetric("net.rx.errors","net.rx.errors","network","GAUGE"));

        plugin.crescoMeterRegistry.gauge("net.tx.dropped", txDropped);
        plugin.metricMap.put("net.tx.dropped",new CMetric("net.tx.dropped","net.tx.dropped","network","GAUGE"));

        plugin.crescoMeterRegistry.gauge("net.tx.errors", txErrors);
        plugin.metricMap.put("net.tx.errors",new CMetric("net.tx.errors","net.tx.errors","network","GAUGE"));

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

            containerImage = image;
            updateImage(image);

            ContainerConfig containerConfig = buildContainer(image,envList,portList);
            ContainerCreation creation = docker.createContainer(containerConfig);
            container_id = creation.id();
            container_ids.add(container_id);

        }
        catch(Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            logger.error("createContainer error: " + errors);
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

            //todo for whatever reason this causes
            //Invocation Exception: [initialize] method invoked on incorrect target [null]
            //docker.pull(imageName);
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

            logger.error("missed update image : " + imageName);
            isUpdated = true;
        }
        catch(Exception ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            //return errors.toString();

            logger.error("updateImage error: " + errors);
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

    /*
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
    */
    public DockerEngine(Plugin plugin) {
        try {
            this.plugin = plugin;
            this.logger = new CLogger(plugin.getMsgOutQueue(), plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), CLogger.Level.Info);
            // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
            docker = DefaultDockerClient.fromEnv().build();

            // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
            //final DockerClient docker = DefaultDockerClient.fromEnv().build();

            container_ids = new ArrayList<>();
            gson = new Gson();

            initMetrics();
        }
        catch(Exception ex) {
            //ex.printStackTrace();
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            //return errors.toString();

            System.out.println("DockerEngine error: " + errors);
            logger.error("DockerEngine error: " + errors);
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
