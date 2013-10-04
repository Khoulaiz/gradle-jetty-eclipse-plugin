/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.sahlbach.gradle.plugins.jettyEclipse

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FileWatcherTimerTask extends TimerTask {

    public static Logger logger = LoggerFactory.getLogger(JettyEclipseRun)

    private File file
    private long lastModified
    private int scanIntervalInSeconds
    private Timer timer
    private FileChangeObserver observer
    private boolean readErrorReported = false

    FileWatcherTimerTask (FileChangeObserver observer, File file, int scanIntervalInSeconds) {
        this.observer = observer
        this.file = file
        this.lastModified = file.lastModified()
        this.scanIntervalInSeconds = scanIntervalInSeconds
        timer = new Timer(true)
        Date startDate = new Date(System.currentTimeMillis() + scanIntervalInSeconds * 1000)
        timer.schedule(this, startDate, scanIntervalInSeconds * 1000)
    }

    void stop() {
        timer.cancel()
    }

    @Override
    public void run() {
        boolean changed = false
        try {
            if(!file.exists() || !file.canRead()) {
                if(!readErrorReported) {
                    observer.notifyFileReadError(file)
                    readErrorReported = true
                }
                lastModified = 0
            } else {
                readErrorReported = false
                if(lastModified != file.lastModified()) {
                    changed = true
                    lastModified = file.lastModified()
                }
            }
        } catch (Exception e) {
            lastModified = 0
        }
        if(changed) {
            observer.notifyFileChanged(file)
        }
    }

}
