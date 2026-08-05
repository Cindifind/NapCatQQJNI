package com.napcat.jni.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * OneBot 11 通知事件
 * <p>
 * 通知事件统一承载于 {@code post_type='notice'} 的事件，
 * 通过 {@link #noticeType} 区分具体子类型。
 * <p>
 * 使用方式：
 * <pre>{@code
 *   public void onEvent(NapCatPluginContext ctx, Event event) {
 *       if (event.isNotice()) {
 *           NoticeEvent notice = event.as(NoticeEvent.class);
 *           switch (notice.getNoticeType()) {
 *               case "group_ban":
 *                   GroupBanEvent ban = event.as(GroupBanEvent.class);
 *                   // ban.getDuration(), ban.getOperatorId() ...
 *                   break;
 *               case "group_increase":
 *                   GroupIncreaseEvent inc = event.as(GroupIncreaseEvent.class);
 *                   // inc.getUserId(), inc.getSubType() ...
 *                   break;
 *           }
 *       }
 *   }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoticeEvent extends Event {

    /** 通知类型（如 group_ban / group_increase / friend_add 等） */
    @JsonProperty("notice_type")
    protected String noticeType;

    /** 子类型（部分通知事件使用，如 set/unset、ban/lift_ban 等） */
    @JsonProperty("sub_type")
    protected String subType;

    /** 操作者 QQ（多数通知事件包含此字段） */
    @JsonProperty("operator_id")
    protected Long operatorId;

    /** 用户 QQ（多数通知事件包含此字段） */
    @JsonProperty("user_id")
    protected Long userId;

    /** 群号（群通知事件包含此字段） */
    @JsonProperty("group_id")
    protected Long groupId;

    // ==================== 便捷判断 ====================

    public boolean isGroupUpload() { return "group_upload".equals(noticeType); }
    public boolean isGroupAdmin() { return "group_admin".equals(noticeType); }
    public boolean isGroupDecrease() { return "group_decrease".equals(noticeType); }
    public boolean isGroupIncrease() { return "group_increase".equals(noticeType); }
    public boolean isGroupBan() { return "group_ban".equals(noticeType); }
    public boolean isGroupRecall() { return "group_recall".equals(noticeType); }
    public boolean isFriendRecall() { return "friend_recall".equals(noticeType); }
    public boolean isFriendAdd() { return "friend_add".equals(noticeType); }
    public boolean isGroupCard() { return "group_card".equals(noticeType); }
    public boolean isEssence() { return "essence".equals(noticeType); }
    public boolean isGroupMsgEmojiLike() { return "group_msg_emoji_like".equals(noticeType); }
    public boolean isBotOffline() { return "bot_offline".equals(noticeType); }
    public boolean isNotify() { return "notify".equals(noticeType); }
    public boolean isPoke() { return isNotify() && "poke".equals(subType); }
    public boolean isGroupTitle() { return isNotify() && "title".equals(subType); }
    public boolean isGroupName() { return isNotify() && "group_name".equals(subType); }
    public boolean isGrayTip() { return isNotify() && "gray_tip".equals(subType); }
    public boolean isInputStatus() { return isNotify() && "input_status".equals(subType); }
    public boolean isProfileLike() { return isNotify() && "profile_like".equals(subType); }
    public boolean isOnlineFileReceive() { return "online_file_receive".equals(noticeType); }
    public boolean isOnlineFileSend() { return "online_file_send".equals(noticeType); }

    // ==================== Getter ====================

    public String getNoticeType() { return noticeType; }
    public String getSubType() { return subType; }
    public Long getOperatorId() { return operatorId; }
    public Long getUserId() { return userId; }
    public Long getGroupId() { return groupId; }

    @Override
    public String toString() {
        return "NoticeEvent{noticeType='" + noticeType + '\'' +
                (subType != null ? ", subType='" + subType + '\'' : "") +
                (groupId != null ? ", groupId=" + groupId : "") +
                (userId != null ? ", userId=" + userId : "") + "}";
    }

    // ==================== 具体通知事件子类 ====================

    /** 群文件上传通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupUploadNoticeEvent extends NoticeEvent {
        private GroupUploadFile file;
        public GroupUploadFile getFile() { return file; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class GroupUploadFile {
            private String id;
            private String name;
            private long size;
            @JsonProperty("busid")
            private long busid;
            public String getId() { return id; }
            public String getName() { return name; }
            public long getSize() { return size; }
            public long getBusid() { return busid; }
        }
    }

    /** 群管理员变动通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupAdminNoticeEvent extends NoticeEvent {
        /** set=设置管理员, unset=取消管理员 */
        public boolean isSet() { return "set".equals(subType); }
        public boolean isUnset() { return "unset".equals(subType); }
    }

    /** 群成员减少通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupDecreaseEvent extends NoticeEvent {
        /** leave=主动退群, kick=被踢, kick_me=自己被踢, disband=群解散 */
        public boolean isLeave() { return "leave".equals(subType); }
        public boolean isKick() { return "kick".equals(subType); }
        public boolean isKickMe() { return "kick_me".equals(subType); }
        public boolean isDisband() { return "disband".equals(subType); }
    }

    /** 群成员增加通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupIncreaseEvent extends NoticeEvent {
        /** approve=主动加群, invite=被邀请 */
        public boolean isApprove() { return "approve".equals(subType); }
        public boolean isInvite() { return "invite".equals(subType); }
    }

    /** 群禁言通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupBanEvent extends NoticeEvent {
        /** 禁言时长（秒），lift_ban 时为 0 */
        private long duration;
        public long getDuration() { return duration; }
        public boolean isBan() { return "ban".equals(subType); }
        public boolean isLiftBan() { return "lift_ban".equals(subType); }
    }

    /** 群消息撤回通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupRecallNoticeEvent extends NoticeEvent {
        @JsonProperty("message_id")
        private long messageId;
        public long getMessageId() { return messageId; }
    }

    /** 好友消息撤回通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FriendRecallNoticeEvent extends NoticeEvent {
        @JsonProperty("message_id")
        private long messageId;
        public long getMessageId() { return messageId; }
    }

    /** 好友添加通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FriendAddNoticeEvent extends NoticeEvent {
    }

    /** 群内戳一戳通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupPokeEvent extends NoticeEvent {
        @JsonProperty("target_id")
        private long targetId;
        private Object rawInfo;
        public long getTargetId() { return targetId; }
        public Object getRawInfo() { return rawInfo; }
    }

    /** 私聊戳一戳通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FriendPokeEvent extends NoticeEvent {
        @JsonProperty("target_id")
        private long targetId;
        @JsonProperty("sender_id")
        private long senderId;
        private Object rawInfo;
        public long getTargetId() { return targetId; }
        public long getSenderId() { return senderId; }
        public Object getRawInfo() { return rawInfo; }
    }

    /** 群成员头衔变更通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupTitleEvent extends NoticeEvent {
        private String title;
        public String getTitle() { return title; }
    }

    /** 群名片变更通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupCardEvent extends NoticeEvent {
        @JsonProperty("card_new")
        private String cardNew;
        @JsonProperty("card_old")
        private String cardOld;
        public String getCardNew() { return cardNew; }
        public String getCardOld() { return cardOld; }
    }

    /** 群名称变更通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupNameEvent extends NoticeEvent {
        @JsonProperty("name_new")
        private String nameNew;
        public String getNameNew() { return nameNew; }
    }

    /** 群精华消息通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupEssenceEvent extends NoticeEvent {
        @JsonProperty("message_id")
        private long messageId;
        @JsonProperty("sender_id")
        private long senderId;
        public boolean isAdd() { return "add".equals(subType); }
        public boolean isDelete() { return "delete".equals(subType); }
        public long getMessageId() { return messageId; }
        public long getSenderId() { return senderId; }
    }

    /** 群消息表情点赞通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupMsgEmojiLikeEvent extends NoticeEvent {
        @JsonProperty("message_id")
        private long messageId;
        private List<MsgEmojiLike> likes;
        @JsonProperty("is_add")
        private boolean isAdd;
        public long getMessageId() { return messageId; }
        public List<MsgEmojiLike> getLikes() { return likes; }
        public boolean isAdd() { return isAdd; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MsgEmojiLike {
            @JsonProperty("emoji_id")
            private String emojiId;
            private int count;
            public String getEmojiId() { return emojiId; }
            public int getCount() { return count; }
        }
    }

    /** 群灰条消息通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupGrayTipEvent extends NoticeEvent {
        @JsonProperty("message_id")
        private long messageId;
        @JsonProperty("busi_id")
        private String busiId;
        private String content;
        private Object rawInfo;
        public long getMessageId() { return messageId; }
        public String getBusiId() { return busiId; }
        public String getContent() { return content; }
        public Object getRawInfo() { return rawInfo; }
    }

    /** 输入状态通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InputStatusEvent extends NoticeEvent {
        @JsonProperty("status_text")
        private String statusText;
        @JsonProperty("event_type")
        private int eventType;
        public String getStatusText() { return statusText; }
        public int getEventType() { return eventType; }
    }

    /** 个人资料点赞通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileLikeEvent extends NoticeEvent {
        @JsonProperty("operator_nick")
        private String operatorNick;
        private int times;
        public String getOperatorNick() { return operatorNick; }
        public int getTimes() { return times; }
    }

    /** 在线文件接收通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OnlineFileReceiveEvent extends NoticeEvent {
        @JsonProperty("peer_id")
        private long peerId;
        public long getPeerId() { return peerId; }
    }

    /** 在线文件发送通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OnlineFileSendEvent extends NoticeEvent {
        @JsonProperty("peer_id")
        private long peerId;
        public long getPeerId() { return peerId; }
        public boolean isReceive() { return "receive".equals(subType); }
        public boolean isRefuse() { return "refuse".equals(subType); }
    }

    /** 机器人离线通知 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BotOfflineEvent extends NoticeEvent {
        private String tag;
        private String message;
        public String getTag() { return tag; }
        public String getMessage() { return message; }
    }
}
