/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2018  huangyuhui <huanghongxun2008@126.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hmcl.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 *
 * @author huangyuhui
 */
public final class Logging {

    public static final Logger LOG;
    private static final ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream();

    static {
        LOG = Logger.getLogger("HMCL");
    }

    public static void start(File logFolder) {
        LOG.setLevel(Level.FINER);
        LOG.setUseParentHandlers(false);

        try {
            FileHandler fileHandler = new FileHandler(new File(logFolder, "hmcl.log").getAbsolutePath());
            fileHandler.setLevel(Level.FINEST);
            fileHandler.setFormatter(DefaultFormatter.INSTANCE);
            LOG.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Unable to create hmcl.log, " + e.getMessage());
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINER);
        consoleHandler.setFormatter(DefaultFormatter.INSTANCE);
        LOG.addHandler(consoleHandler);

        StreamHandler streamHandler = new StreamHandler(OUTPUT_STREAM, DefaultFormatter.INSTANCE) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        streamHandler.setLevel(Level.FINEST);
        LOG.addHandler(streamHandler);
    }

    public static void stop() {
        for (Handler handler : LOG.getHandlers())
            LOG.removeHandler(handler);
    }

    public static String getLogs() {
        return OUTPUT_STREAM.toString();
    }

    static final class DefaultFormatter extends Formatter {

        static final DefaultFormatter INSTANCE = new DefaultFormatter();
        private final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            String date = format.format(new Date(record.getMillis()));
            String log = String.format("[%s] [%s.%s/%s] %s%n",
                    date, record.getSourceClassName(), record.getSourceMethodName(),
                    record.getLevel().getName(), record.getMessage()
            );
            ByteArrayOutputStream builder = new ByteArrayOutputStream();
            if (record.getThrown() != null)
                try (PrintWriter writer = new PrintWriter(builder)) {
                    record.getThrown().printStackTrace(writer);
                }
            return log + builder.toString();
        }

    }
}
