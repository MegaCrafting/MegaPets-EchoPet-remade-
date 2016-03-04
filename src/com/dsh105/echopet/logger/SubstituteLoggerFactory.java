package com.dsh105.echopet.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SubstituteLoggerFactory manages instances of {@link SubstituteLogger}.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Chetan Mehrotra
 */
public class SubstituteLoggerFactory implements ILoggerFactory {

    final ConcurrentMap<String, SubstituteLogger> loggers = new ConcurrentHashMap<String, SubstituteLogger>();

    final List<SubstituteLoggingEvent> eventList = Collections.synchronizedList(new ArrayList<SubstituteLoggingEvent>());

    public Logger getLogger(String name) {
        SubstituteLogger logger = loggers.get(name);
        if (logger == null) {
            logger = new SubstituteLogger(name, eventList);
            SubstituteLogger oldLogger = loggers.putIfAbsent(name, logger);
            if (oldLogger != null)
                logger = oldLogger;
        }
        return logger;
    }

    public List<String> getLoggerNames() {
        return new ArrayList<String>(loggers.keySet());
    }

    public List<SubstituteLogger> getLoggers() {
        return new ArrayList<SubstituteLogger>(loggers.values());
    }

    public List<SubstituteLoggingEvent> getEventList() {
        return eventList;
    }

    public void clear() {
        loggers.clear();
        eventList.clear();
    }
}
