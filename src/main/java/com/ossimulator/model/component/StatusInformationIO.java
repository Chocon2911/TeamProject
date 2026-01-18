package com.ossimulator.model.component;

import java.util.ArrayList;
import java.util.List;

public class StatusInformationIO {
    //==========================================Variable==========================================
    private List<String> openFiles;      // List open files
    private String waitingDevice;        // Waiting I/O Device

    //========================================Constructor=========================================
    public StatusInformationIO(List<String> openFiles, String waitingDevice) {
        this.openFiles = openFiles;
        this.waitingDevice = waitingDevice;
    }

    public StatusInformationIO() {
        this.openFiles = new ArrayList<>();
        this.waitingDevice = null;
    }

    //==========================================Get Set===========================================
    public List<String> getOpenFiles() { return openFiles; }
    public String getWaitingDevice() { return waitingDevice; }

    public void setOpenFiles(List<String> openFiles) { this.openFiles = openFiles; }
    public void setWaitingDevice(String waitingDevice) { this.waitingDevice = waitingDevice; }
}
