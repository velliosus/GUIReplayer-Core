package edu.umd.cs.guitar.replayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.umd.cs.guitar.exception.GException;
import edu.umd.cs.guitar.model.GIDGenerator;
import edu.umd.cs.guitar.model.IO;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentListType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.Configuration;
import edu.umd.cs.guitar.model.data.FullComponentType;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.replayer.monitor.GTestMonitor;
import edu.umd.cs.guitar.replayer.monitor.StateMonitorFull;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.umd.cs.guitar.exception.ComponentNotFound;

public abstract class ReplayerMain {
    protected GReplayerConfiguration config;
     private String log4jFile;

    public ReplayerMain(GReplayerConfiguration config) {
        this.config = config;
    }

    public void execute() {
        long nStartTime = System.currentTimeMillis();
	int tests = 0;
	int failures = 0;

	log4jFile = config.LOG_FILE;
        System.setProperty("file.name", log4jFile);
        PropertyConfigurator.configure(GUITARConstants.LOG4J_PROPERTIES_FILE);

        setupEnv();

        GUITARLog.log = Logger.getLogger(ReplayerMain.class.getSimpleName());
        printInfo();
	

	for (String test : config.TESTCASE.split(",")){
	long individualStartTime = System.currentTimeMillis();
	tests++;
        TestCase tc = (TestCase) IO.readObjFromFile(
            test, TestCase.class);

        Replayer replayer;
        try {
	    if(config.REPLAYER_MODE != 0)
		replayer = new Replayer(tc, config.GUI_FILE, config.EFG_FILE, config.REPLAYER_MODE);
		
	    else
            replayer = new Replayer(tc, config.GUI_FILE, config.EFG_FILE);
            GReplayerMonitor monitor = createMonitor();

            // Adds a GUI state record monitor
            StateMonitorFull stateMonitor = new StateMonitorFull(
                config.GUI_STATE_FILE, config.DELAY);

            // Set the id generator for the state monitor
            GIDGenerator idGenerator = getIdGenerator();
            stateMonitor.setIdGenerator(idGenerator);

            replayer.addTestMonitor(stateMonitor);

            // TODO: Add additional test monitors

            replayer.setMonitor(monitor);

            replayer.execute();
        }catch(ComponentNotFound e){
		GUITARLog.log.error("Component not found : " + e.getWidget(), e);
		failures++;
	} 
	catch (GException e) {
            GUITARLog.log.error("GUITAR Exception thrown", e);
        } catch (Exception e) {
            GUITARLog.log.error("Exception thrown", e);    
	} finally {
            // Elapsed time
            long nEndTime = System.currentTimeMillis();
            long duration = nEndTime - individualStartTime;
            DateFormat df = new SimpleDateFormat("HH : mm : ss : SS");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            GUITARLog.log.info("Time Elapsed: " + df.format(duration));
            //printInfo();
        }
	}

            long finalEndTime = System.currentTimeMillis();
            long duration = finalEndTime - nStartTime;
            DateFormat df = new SimpleDateFormat("HH : mm : ss : SS");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
		GUITARLog.log.info((tests - failures) + " tests succeeded out of " + tests + " tests");
		GUITARLog.log.info("Overall success rate: " + ((tests - failures) / ((float)(tests))) * 100 + "%");
            GUITARLog.log.info("Total Time Elapsed: " + df.format(duration));
	//printInfo();
		System.exit(0);
    }

    private void setupEnv() {
        // Load configuration file
        Configuration conf = (Configuration) IO.readObjFromFile(
            config.CONFIG_FILE, Configuration.class);

        if (conf != null) {
            // Load the terminal/ignored windows from the configuration
            loadWidgetConfiguration(
                conf.getTerminalComponents().getFullComponent(),
                GUITARConstants.terminalWidgetSignature);
            loadWidgetConfiguration(
                conf.getIgnoredComponents().getFullComponent(),
                GUITARConstants.ignoredWidgetSignature);
        }
    }

    private void loadWidgetConfiguration(
        List<FullComponentType> widgetList,
        List<AttributesTypeWrapper> widgetSignatures)
    {
        for (FullComponentType widget : widgetList) {
            ComponentType component = widget.getWindow();
            if (component == null) {
                component = widget.getComponent();
            }
            AttributesType attributes = component.getAttributes();
            widgetSignatures.add(
                new AttributesTypeWrapper(component.getAttributes()));
        }
    }

    private void printInfo() {
        GUITARLog.log.info("Testcase: " + config.TESTCASE);
        GUITARLog.log.info("Log file: " + config.LOG_FILE);
        GUITARLog.log.info("GUI state file: " + config.GUI_STATE_FILE);
    }

    protected abstract GReplayerMonitor createMonitor();

    protected abstract GIDGenerator getIdGenerator();
}
