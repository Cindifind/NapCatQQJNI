package com.napcat.jni.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OneBot 11 请求事件
 * <p>
 * 请求事件承载于 {@code post_type='request'} 的事件，
 * 通过 {@link #requestType} 区分加好友 / 加群。
 * <p>
 * 使用方式：
 * <pre>{@code
 *   public void onEvent(NapCatPluginContext ctx, Event event) {
 *       if (event.isRequest()) {
 *           RequestEvent req = event.as(RequestEvent.class);
 *           if (req.isFriendRequest()) {
 *               FriendRequestEvent fr = event.as(FriendRequestEvent.class);
 *               ctx.getActions().setFriendAddRequest(fr.getFlag(), true, "");
 *           }
 *       }
 *   }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestEvent extends Event {

    /** 请求类型：friend / group */
    @JsonProperty("request_type")
    protected String requestType;

    /** 请求者 QQ */
    @JsonProperty("user_id")
    protected long userId;

    /** 验证信息 */
    protected String comment;

    /** 请求 flag（用于处理请求） */
    protected String flag;

    // ==================== 便捷判断 ====================

    public boolean isFriendRequest() { return "friend".equals(requestType); }
    public boolean isGroupRequest() { return "group".equals(requestType); }

    // ==================== Getter ====================

    public String getRequestType() { return requestType; }
    public long getUserId() { return userId; }
    public String getComment() { return comment; }
    public String getFlag() { return flag; }

    @Override
    public String toString() {
        return "RequestEvent{requestType='" + requestType + "', userId=" + userId +
                ", comment='" + comment + "', flag='" + flag + "'}";
    }

    // ==================== 具体请求事件子类 ====================

    /** 加好友请求 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FriendRequestEvent extends RequestEvent {
    }

    /** 加群请求 / 邀请 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupRequestEvent extends RequestEvent {
        /** 群号 */
        @JsonProperty("group_id")
        private long groupId;
        /** 子类型：add=加群请求, invite=加群邀请 */
        @JsonProperty("sub_type")
        private String subType;

        public long getGroupId() { return groupId; }
        public String getSubType() { return subType; }
        /** add=加群请求 */
        public boolean isAdd() { return "add".equals(subType); }
        /** invite=加群邀请 */
        public boolean isInvite() { return "invite".equals(subType); }
    }
}
