package com.ossimulator.model.component;

public class AccountingInformation {
    //==========================================Variable==========================================
    private long cpuTimeUsed;            // CPU time used
    private long creationTime;           // process created time
    private long lastScheduledTime;      // last time used CPU
    private int uid;                     // User ID
    private int gid;                     // Group ID

    //========================================Constructor=========================================
    public AccountingInformation() {
        this.cpuTimeUsed = 0;
        this.creationTime = System.currentTimeMillis();
        this.lastScheduledTime = 0;
        this.uid = 0;
        this.gid = 0;
    }

    public AccountingInformation(long cpuTimeUsed, long creationTime, long lastScheduledTime, int uid, int gid) {
        this.cpuTimeUsed = cpuTimeUsed;
        this.creationTime = creationTime;
        this.lastScheduledTime = lastScheduledTime;
        this.uid = uid;
        this.gid = gid;
    }

    //==========================================Get Set===========================================
    public long getCpuTimeUsed() { return cpuTimeUsed; }
    public long getCreationTime() { return creationTime; }
    public long getLastScheduledTime() { return lastScheduledTime; }
    public int getUid() { return uid; }
    public int getGid() { return gid; }

    public void setCpuTimeUsed(long cpuTimeUsed) { this.cpuTimeUsed = cpuTimeUsed; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    public void setLastScheduledTime(long lastScheduledTime) { this.lastScheduledTime = lastScheduledTime; }
    public void setUid(int uid) { this.uid = uid; }
    public void setGid(int gid) { this.gid = gid; }
}
