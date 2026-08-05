package com.napcat.jni.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.napcat.jni.model.message.MessageSegment;

import java.util.List;
import java.util.Map;

/**
 * OneBot 11 消息事件实体类
 * <p>
 * 封装 OneBot 11 协议的消息事件字段，提供强类型 getter 访问。
 * 继承自 {@link Event}，用于 {@link com.napcat.jni.plugin.NapCatPlugin#onMessage} 回调参数。
 * <p>
 * 示例：
 * <pre>{@code
 *   public void onMessage(NapCatPluginContext ctx, MessageEvent event) {
 *       String text = event.getRawMessage();
 *       long senderId = event.getUserId();
 *       if (event.isGroupMessage()) {
 *           long groupId = event.getGroupId();
 *           ctx.getActions().sendGroupText(String.valueOf(groupId), "收到: " + text);
 *       }
 *   }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageEvent extends Event {

    /** 消息类型：private 或 group */
    @JsonProperty("message_type")
    private String messageType;

    /** 消息子类型：friend / group / normal */
    @JsonProperty("sub_type")
    private String subType;

    /** 消息 ID */
    @JsonProperty("message_id")
    private long messageId;

    /** 消息序列号 */
    @JsonProperty("message_seq")
    private long messageSeq;

    /** 真实 ID */
    @JsonProperty("real_id")
    private long realId;

    /** 真实序列号 */
    @JsonProperty("real_seq")
    private String realSeq;

    /** 发送者 QQ 号 */
    @JsonProperty("user_id")
    private long userId;

    /** 群号（仅群消息存在） */
    @JsonProperty("group_id")
    private Long groupId;

    /** 群名称（仅群消息可能存在） */
    @JsonProperty("group_name")
    private String groupName;

    /** 目标 ID（临时会话等场景） */
    @JsonProperty("target_id")
    private Long targetId;

    /** 发送者信息 */
    private Sender sender;

    /** 消息内容（数组格式） */
    private List<MessageSegment> message;

    /** 消息格式：array 或 string */
    @JsonProperty("message_format")
    private String messageFormat;

    /** 原始消息文本 */
    @JsonProperty("raw_message")
    private String rawMessage;

    /** 字体 ID */
    private long font;

    /** 临时会话来源 */
    @JsonProperty("temp_source")
    private Integer tempSource;

    /** 消息发送类型 */
    @JsonProperty("message_sent_type")
    private String messageSentType;

    /** 原始消息对象（高级用法，通常无需访问） */
    private Map<String, Object> raw;

    // ==================== 便捷判断方法 ====================

    /** 是否为群消息 */
    public boolean isGroupMessage() {
        return "group".equals(messageType);
    }

    /** 是否为私聊消息 */
    public boolean isPrivateMessage() {
        return "private".equals(messageType);
    }

    /** 是否为好友消息 */
    public boolean isFriendMessage() {
        return "private".equals(messageType) && "friend".equals(subType);
    }

    /** 是否为临时会话消息 */
    public boolean isTempMessage() {
        return "private".equals(messageType) && "group".equals(subType);
    }

    /** 是否为自身发送的消息 */
    public boolean isSelfMessage() {
        return userId == selfId;
    }

    /**
     * 获取对话目标 ID（群消息返回群号，私聊返回用户号）
     *
     * @return 对话 peer ID
     */
    public String getPeerId() {
        if (isGroupMessage() && groupId != null) {
            return String.valueOf(groupId);
        }
        return String.valueOf(userId);
    }

    // ==================== Getter ====================

    public String getMessageType() { return messageType; }
    public String getSubType() { return subType; }
    public long getMessageId() { return messageId; }
    public long getMessageSeq() { return messageSeq; }
    public long getRealId() { return realId; }
    public String getRealSeq() { return realSeq; }
    public long getUserId() { return userId; }
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public Long getTargetId() { return targetId; }
    public Sender getSender() { return sender; }
    public List<MessageSegment> getMessage() { return message; }
    public String getMessageFormat() { return messageFormat; }
    public String getRawMessage() { return rawMessage; }
    public long getFont() { return font; }
    public Integer getTempSource() { return tempSource; }
    public String getMessageSentType() { return messageSentType; }
    public Map<String, Object> getRaw() { return raw; }

    /**
     * 发送者信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {
        @JsonProperty("user_id")
        private long userId;

        private String nickname;

        /** 群名片 */
        private String card;

        /** 角色：owner / admin / member */
        private String role;

        /** 性别：male / female / unknown */
        private String sex;

        private Integer age;
        private String area;
        private String level;
        private String title;

        public long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public String getCard() { return card; }
        public String getRole() { return role; }
        public String getSex() { return sex; }
        public Integer getAge() { return age; }
        public String getArea() { return area; }
        public String getLevel() { return level; }
        public String getTitle() { return title; }

        /** 是否为群主 */
        public boolean isOwner() { return "owner".equals(role); }
        /** 是否为管理员 */
        public boolean isAdmin() { return "admin".equals(role); }
        /** 是否为群主或管理员 */
        public boolean isPrivileged() { return isOwner() || isAdmin(); }

        @Override
        public String toString() {
            return "Sender{userId=" + userId + ", nickname='" + nickname + "', card='" + card + "', role='" + role + "'}";
        }
    }

    @Override
    public String toString() {
        return "MessageEvent{messageId=" + messageId + ", messageType='" + messageType + '\'' +
                ", userId=" + userId + (groupId != null ? ", groupId=" + groupId : "") +
                ", rawMessage='" + rawMessage + '\'' + "}";
    }
}
