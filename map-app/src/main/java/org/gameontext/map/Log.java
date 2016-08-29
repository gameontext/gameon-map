/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.map;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper for a single logger
 */
public class Log {
    private final static Logger log = Logger.getLogger("net.wasdev.gameon.map");
    private final static Logger detailLog = Logger.getLogger("map.details");

    private static final String log_format = "%s: %s";

    public static void log(Level level, Object source, String message, Object... args) {
        writeLog(log, level, source, message, args);
    }

    public static void log(Level level, Object source, String message, Throwable thrown) {
        writeLog(log, level, source, message, thrown);
    }

    public static void mapOperations(Level level, Object source, String message, Object... args) {
        writeLog(detailLog, level, source, message, args);
    }

    public static void logDetails(Level level, Object source, String message, Throwable thrown) {
        writeLog(detailLog, level, source, message, thrown);
    }

    private static void writeLog(Logger logger, Level level, Object source, String message, Object... args) {
        if (logger.isLoggable(level)) {
            String msg = String.format(log_format, getObjectInfo(source), message);
            logger.log(useLevel(level), msg, args);
        }
    }

    private static void writeLog(Logger logger, Level level, Object source, String message, Throwable thrown) {
        if (logger.isLoggable(level)) {
            String msg = String.format(log_format, getObjectInfo(source), message);
            logger.log(useLevel(level), msg, thrown);
        }
    }

    private static String getObjectInfo(Object source) {
        return source == null ? "null" : source.getClass().getSimpleName() + "("+ System.identityHashCode(source) + ")";
    }

    /**
     * This bumps enabled trace up to INFO level, so it appears in messages.log
     * @param level Original level
     * @return Original Level or INFO level, whichever is greater
     */
    private static Level useLevel(Level level) {
        if ( level.intValue() < Level.INFO.intValue() ) {
            return Level.INFO;
        }
        return level;
    }
}
