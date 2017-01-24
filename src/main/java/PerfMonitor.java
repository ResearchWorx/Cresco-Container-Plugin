
import com.google.gson.Gson;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.plugin.core.CPlugin;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

class PerfMonitor {
    private CPlugin plugin;

    private Timer timer;
    private boolean running = false;
    private DockerEngine de;
    private String container_id;
    private Gson gson;

    PerfMonitor(CPlugin plugin, DockerEngine de, String container_id) {
        this.plugin = plugin;
        this.de = de;
        this.container_id = container_id;
        gson = new Gson();
    }

    PerfMonitor start() {
        if (this.running) return this;
        Long interval = plugin.getConfig().getLongParam("perftimer", 5000L);

        MsgEvent initial = new MsgEvent(MsgEvent.Type.INFO, plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), "Performance Monitoring timer set to " + interval + " milliseconds.");
        initial.setParam("src_region", plugin.getRegion());
        initial.setParam("src_agent", plugin.getAgent());
        initial.setParam("src_plugin", plugin.getPluginID());
        initial.setParam("dst_region", plugin.getRegion());
        plugin.sendMsgEvent(initial);

        timer = new Timer();
        timer.scheduleAtFixedRate(new PerfMonitorTask(plugin), 500, interval);
        return this;
    }

    PerfMonitor restart() {
        if (running) timer.cancel();
        running = false;
        return start();
    }

    void stop() {
        timer.cancel();
        running = false;
    }

    private class PerfMonitorTask extends TimerTask {
        private CPlugin plugin;

        PerfMonitorTask(CPlugin plugin) {
            this.plugin = plugin;
        }

        public void run() {
            MsgEvent tick = new MsgEvent(MsgEvent.Type.KPI, plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), "Performance Monitoring tick.");
            tick.setParam("src_region", plugin.getRegion());
            tick.setParam("src_agent", plugin.getAgent());
            tick.setParam("src_plugin", plugin.getPluginID());
            tick.setParam("dst_region", plugin.getRegion());
            tick.setParam("resource_id",plugin.getConfig().getStringParam("resource_id","container_resource"));
            tick.setParam("inode_id",plugin.getConfig().getStringParam("inode_id","container_inode"));
            tick.setParam("container_image",de.containerImage);
            ResourceMetric rm = de.getResourceMetric(container_id);
            String resourceMetricJSON = gson.toJson(rm);

            tick.setParam("resource_metric", resourceMetricJSON);

            plugin.sendMsgEvent(tick);
        }
    }
}
