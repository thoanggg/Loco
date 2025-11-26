// SỬA LỖI: Cập nhật package để khớp với cấu trúc của bạn
package com.myapp.locoagent;

/**
 * POJO (Plain Old Java Object)
 * Dùng để Jackson tự động chuyển JSON request (nhận được) thành đối tượng Java.
 */
public class LogRequest {
    private String logChannel;
    private int eventCount;
    private String xpathQuery;

    // Getters and Setters (bắt buộc cho Jackson)
    public String getLogChannel() {
        return logChannel;
    }

    public void setLogChannel(String logChannel) {
        this.logChannel = logChannel;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public String getXpathQuery() {
        return xpathQuery;
    }

    public void setXpathQuery(String xpathQuery) {
        this.xpathQuery = xpathQuery;
    }
}