package com.ossimulator.model.component;

public class MemoryPointer {
    //==========================================Variable==========================================
    private int baseAddress;             // Base address
    private int limitAddress;            // Limit address
    private int pageTablePointer;         // Page table pointer

    //========================================Constructor=========================================
    public MemoryPointer(int baseAddress, int limitAddress) {
        this.baseAddress = baseAddress;
        this.limitAddress = limitAddress;
        this.pageTablePointer = 0;
    }

    public MemoryPointer(int baseAddress, int limitAddress, int pageTablePointer) {
        this.baseAddress = baseAddress;
        this.limitAddress = limitAddress;
        this.pageTablePointer = pageTablePointer;
    }

    //==========================================Get Set===========================================
    public int getBaseAddress() { return baseAddress; }
    public int getLimitAddress() { return limitAddress; }
    public int getPageTablePointer() { return pageTablePointer; }

    public void setBaseAddress(int baseAddress) { this.baseAddress = baseAddress; }
    public void setLimitAddress(int limitAddress) { this.limitAddress = limitAddress; }
    public void setPageTablePointer(int pageTablePointer) { this.pageTablePointer = pageTablePointer; }
}
