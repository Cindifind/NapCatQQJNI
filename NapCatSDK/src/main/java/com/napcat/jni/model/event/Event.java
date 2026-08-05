package com.napcat.jni.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OneBot 事件基类
 * <p>
 * 所有 OneBot 事件的根抽象，承载公共字段（time / self_id / post_type），
 * 提供类型判断便捷方法（isMessage / isNotice / isRequest / isMeta），
 * 以及向具体子类型转换的 {@link #as(Class)} 方法。
 * <p>
 * 典型用法：
 * <pre>{@code
 *   public void onEvent(NapCatPluginContext ctx, Event event) {
 *       if (event.isNotice()) {
 *           NoticeEvent notice = event.as(NoticeEvent.class);
 *           if (notice.isGroupBan()) {
 *               NoticeEvent.GroupBanEvent ban = event.as(NoticeEvent.GroupBanEvent.class);
 *               ctx.getLogger().info("{} 被禁言 {} 秒", ban.getUserId(), ban.getDuration());
 *           }
 *       } else if (event.isRequest()) {
 *           RequestEvent req = event.as(RequestEvent.class);
 *           // ...
 *       } else if (event.isMeta()) {
 *           MetaEvent meta = event.as(MetaEvent.class);
 *           // ...
 *       }
 *   }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 上报类型：message / message_sent / notice / request / meta_event */
    @JsonProperty("post_type")
    protected String postType;

    /** 事件时间戳（秒） */
    protected long time;

    /** 机器人 QQ 号 */
    @JsonProperty("self_id")
    protected long selfId;

    // ==================== 类型判断 ====================

    /** 是否为消息事件 */
    public boolean isMessage() {
        return "message".equals(postType) || "message_sent".equals(postType);
    }

    /** 是否为自身发送的消息事件 */
    public boolean isMessageSent() {
        return "message_sent".equals(postType);
    }

    /** 是否为通知事件 */
    public boolean isNotice() {
        return "notice".equals(postType);
    }

    /** 是否为请求事件 */
    public boolean isRequest() {
        return "request".equals(postType);
    }

    /** 是否为元事件 */
    public boolean isMeta() {
        return "meta_event".equals(postType);
    }

    // ==================== 类型转换 ====================

    /**
     * 转换为具体子类型（通过 Jackson 重新映射）。
     * 适用场景：从基类 Event 转为 NoticeEvent / RequestEvent / MetaEvent，
     * 或从 NoticeEvent 转为具体的 GroupBanEvent 等。
     *
     * @param cls 目标类型
     * @return 目标类型实例
     */
    public <T> T as(Class<T> cls) {
        return MAPPER.convertValue(this, cls);
    }

    // ==================== Getter ====================

    public String getPostType() { return postType; }
    public long getTime() { return time; }
    public long getSelfId() { return selfId; }

    @Override
    public String toString() {
        return "Event{postType='" + postType + "', time=" + time + ", selfId=" + selfId + "}";
    }
}
