import com.google.auto.service.AutoService;
import com.researchworx.cresco.library.plugin.core.CPlugin;

@AutoService(CPlugin.class)
public class Plugin extends CPlugin {

    public DockerEngine de;

    @Override
    public void setExecutor() {
        setExec(new Executor(this));
    }

    public void start() {

        de = new DockerEngine();

        logger.info("Performance monitoring plugin initialized");
        perfMonitor = new PerfMonitor(this);
        perfMonitor.start();
        setExec(new Executor(this));
    }

    @Override
    public void cleanUp() {

        perfMonitor.stop();

    }
}
