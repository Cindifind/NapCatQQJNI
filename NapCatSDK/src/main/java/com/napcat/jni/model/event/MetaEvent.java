package com.napcat.jni.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * OneBot 11 元事件
 * <p>
 * 元事件承载于 {@code post_type='meta_event'} 的事件，
 * 通过 {@link #metaEventType} 区分心跳 / 生命周期。
 * <p>
 * 使用方式：
 * <pre>{@code
 *   public void onEvent(NapCatPluginContext ctx, Event event) {
 *       if (event.isMeta()) {
 *           MetaEvent meta = event.as(MetaEvent.class);
 *           if (meta.isHeartbeat()) {
 *               HeartbeatEvent hb = event.as(HeartbeatEvent.class);
 *               ctx.getLogger().info("心跳: online={}, interval={}ms",
 *                   hb.getStatus().isOnline(), hb.getInterval());
 *           }
 *       }
 *   }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaEvent extends Event {

    /** 元事件类型：heartbeat / lifecycle */
    @JsonProperty("meta_event_type")
    protected String metaEventType;

    // ==================== 便捷判断 ====================

    public boolean isHeartbeat() { return "heartbeat".equals(metaEventType); }
    public boolean isLifecycle() { return "lifecycle".equals(metaEventType); }

    // ==================== Getter ====================

    public String getMetaEventType() { return metaEventType; }

    @Override
    public String toString() {
        return "MetaEvent{metaEventType='" + metaEventType + "', time=" + time + "}";
    }

    // ==================== 具体元事件子类 ====================

    /** 心跳事件 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeartbeatEvent extends MetaEvent {
        private HeartbeatStatus status;
        private long interval;

        public HeartbeatStatus getStatus() { return status; }
        public long getInterval() { return interval; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HeartbeatStatus {
            private boolean online;
            private boolean good;
            public boolean isOnline() { return online; }
            public boolean isGood() { return good; }
        }
    }

    /** 生命周期事件 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LifeCycleEvent extends MetaEvent {
        /** enable / disable / connect */
        @JsonProperty("sub_type")
        private String subType;

        public String getSubType() { return subType; }
        public boolean isEnable() { return "enable".equals(subType); }
        public boolean isDisable() { return "disable".equals(subType); }
        public boolean isConnect() { return "connect".equals(subType); }
    }
}
