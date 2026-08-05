package com.napcat.jni.examples;

import com.napcat.jni.model.event.Event;
import com.napcat.jni.model.event.MessageEvent;
import com.napcat.jni.model.event.NoticeEvent;
import com.napcat.jni.model.event.RequestEvent;
import com.napcat.jni.model.message.Message;
import com.napcat.jni.model.message.MessageSegment;
import com.napcat.jni.model.result.LoginInfo;
import com.napcat.jni.model.result.SendMsgResult;
import com.napcat.jni.plugin.NapCatPlugin;
import com.napcat.jni.plugin.NapCatPluginContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 示例 Java 插件：展示 Model 化 API 用法
 * <p>
 * 功能：
 * <ul>
 *   <li>启动时获取登录号信息并打印</li>
 *   <li>收到 "!ping" 回复 "pong" + 运行时间</li>
 *   <li>收到 "!java" 回复 JVM 信息</li>
 *   <li>收到 "@机器人 你好" 回复一条带 @ 的消息</li>
 *   <li>收到 "!image" 回复一张图片</li>
 * </ul>
 * <p>
 * 此示例展示 {@link MessageEvent} 强类型 API 的用法。
 */
public class DemoPlugin implements NapCatPlugin {

    private NapCatPluginContext ctx;
    private long startTime;
    private String selfId;

    @Override
    public void onInit(NapCatPluginContext ctx) {
        this.ctx = ctx;
        this.startTime = System.currentTimeMillis();
        ctx.getLogger().info("DemoPlugin 初始化成功，数据目录: {}", ctx.getDataPath());

        // 使用强类型 API 获取登录号信息
        ctx.getActions().getLoginInfoTyped()
                .thenAccept((LoginInfo info) -> {
                    selfId = String.valueOf(info.userId);
                    ctx.getLogger().info("当前登录账号: {} ({})", info.nickname, info.userId);
                })
                .exceptionally(ex -> {
                    ctx.getLogger().error("获取登录信息失败", ex);
                    return null;
                });
    }

    @Override
    public void onMessage(NapCatPluginContext ctx, MessageEvent event) throws Exception {
        ctx.getLogger().debug("onMessage: {}", event);

        String raw = event.getRawMessage();
        if (raw == null) return;

        // 使用强类型 getter 获取字段
        String peer = event.getPeerId();
        String msgType = event.getMessageType();

        if (raw.startsWith("!ping")) {
            long uptime = (System.currentTimeMillis() - startTime) / 1000;
            String text = "pong! 运行时间: " + uptime + "s";
            // 使用 Model API 发送纯文本
            ctx.getActions().sendMsgTyped(msgType, peer, Message.ofText(text))
                    .thenAccept((SendMsgResult r) -> ctx.getLogger().info("已回复: {} (msgId={})", text, r.messageId))
                    .exceptionally(ex -> {
                        ctx.getLogger().error("回复失败", ex);
                        return null;
                    });

        } else if (raw.startsWith("!java")) {
            String text = String.format(
                    "Java 信息\n  版本: %s\n  运行时: %s\n  可用内存: %.1f MB",
                    System.getProperty("java.version"),
                    System.getProperty("java.vm.name"),
                    Runtime.getRuntime().freeMemory() / 1024.0 / 1024.0
            );
            ctx.getActions().sendMsgTyped(msgType, peer, Message.ofText(text))
                    .thenAccept(r -> ctx.getLogger().info("已回复 JVM 信息"))
                    .exceptionally(ex -> {
                        ctx.getLogger().error("回复失败", ex);
                        return null;
                    });

        } else if (raw.startsWith("!image")) {
            // 使用消息构建器发送图片 + 文字
            List<MessageSegment> message = Message.builder()
                    .image("https://www.example.com/logo.png")
                    .text("这是一张图片")
                    .build();
            ctx.getActions().sendMsgTyped(msgType, peer, message)
                    .thenAccept(r -> ctx.getLogger().info("已发送图片，msgId={}", r.messageId))
                    .exceptionally(ex -> {
                        ctx.getLogger().error("发送图片失败", ex);
                        return null;
                    });

        } else if (raw.contains("@") && selfId != null && raw.contains(selfId)) {
            // 被 @时回复，使用构建器组合 @ + 文本
            List<MessageSegment> message = Message.builder()
                    .at(selfId)
                    .text(" 你好呀~")
                    .build();
            ctx.getActions().sendMsgTyped(msgType, peer, message)
                    .exceptionally(ex -> {
                        ctx.getLogger().error("回复失败", ex);
                        return null;
                    });

        } else if (raw.startsWith("!sender") && event.isGroupMessage()) {
            // 展示 sender 信息访问
            MessageEvent.Sender sender = event.getSender();
            if (sender != null) {
                String text = String.format(
                        "发送者信息\n  QQ: %d\n  昵称: %s\n  群名片: %s\n  角色: %s",
                        sender.getUserId(),
                        sender.getNickname(),
                        sender.getCard() != null ? sender.getCard() : "(无)",
                        sender.getRole() != null ? sender.getRole() : "(未知)"
                );
                ctx.getActions().sendMsgTyped(msgType, peer, Message.ofText(text))
                        .exceptionally(ex -> {
                            ctx.getLogger().error("回复失败", ex);
                            return null;
                        });
            }

        } else if (raw.startsWith("!groupinfo") && event.isGroupMessage()) {
            // 查询群信息（强类型）
            ctx.getActions().getGroupInfoTyped(peer)
                    .thenAccept(info -> {
                        List<MessageSegment> reply = Message.ofText(String.format(
                                "群信息\n  群号: %d\n  群名: %s\n  成员数: %d",
                                info.groupId, info.groupName, info.memberCount
                        ));
                        ctx.getActions().sendMsgTyped(msgType, peer, reply);
                    })
                    .exceptionally(ex -> {
                        ctx.getLogger().error("获取群信息失败", ex);
                        return null;
                    });
        }
    }

    @Override
    public void onEvent(NapCatPluginContext ctx, Event event) throws Exception {
        // 使用 Event 基类的类型判断 + as() 转换访问具体子类型
        if (event.isNotice()) {
            NoticeEvent notice = event.as(NoticeEvent.class);
            if (notice.isGroupBan()) {
                NoticeEvent.GroupBanEvent ban = event.as(NoticeEvent.GroupBanEvent.class);
                ctx.getLogger().info("群 {} 用户 {} 被禁言 {} 秒",
                        ban.getGroupId(), ban.getUserId(), ban.getDuration());
            } else if (notice.isGroupIncrease()) {
                NoticeEvent.GroupIncreaseEvent inc = event.as(NoticeEvent.GroupIncreaseEvent.class);
                ctx.getLogger().info("群 {} 新成员加入: {}", inc.getGroupId(), inc.getUserId());
            } else if (notice.isPoke()) {
                ctx.getLogger().info("戳一戳事件: {}", notice);
            }
        } else if (event.isRequest()) {
            RequestEvent req = event.as(RequestEvent.class);
            if (req.isFriendRequest()) {
                RequestEvent.FriendRequestEvent fr = event.as(RequestEvent.FriendRequestEvent.class);
                ctx.getLogger().info("好友请求: {} ({})", fr.getUserId(), fr.getComment());
            } else if (req.isGroupRequest()) {
                RequestEvent.GroupRequestEvent gr = event.as(RequestEvent.GroupRequestEvent.class);
                ctx.getLogger().info("群请求: 群={} 用户={} 类型={} 验证={}",
                        gr.getGroupId(), gr.getUserId(), gr.getSubType(), gr.getComment());
            }
        } else if (event.isMeta()) {
            ctx.getLogger().debug("元事件: {}", event);
        }
    }

    @Override
    public void onCleanup(NapCatPluginContext ctx) {
        ctx.getLogger().info("DemoPlugin 卸载完成");
    }
}
