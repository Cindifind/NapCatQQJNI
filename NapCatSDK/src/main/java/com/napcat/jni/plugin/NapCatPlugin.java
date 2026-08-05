package com.napcat.jni.plugin;

import com.napcat.jni.model.event.Event;
import com.napcat.jni.model.event.MessageEvent;

/**
 * NapCat Java 插件接口
 * <p>
 * 所有 Java 插件都需要实现此接口并提供无参构造函数。
 * 生命周期对应 Node 侧 PluginModule：
 * <ul>
 *   <li>{@link #onInit(NapCatPluginContext)} → plugin_init</li>
 *   <li>{@link #onMessage(NapCatPluginContext, MessageEvent)} → plugin_onmessage</li>
 *   <li>{@link #onEvent(NapCatPluginContext, Event)} → plugin_onevent</li>
 *   <li>{@link #onCleanup(NapCatPluginContext)} → plugin_cleanup</li>
 * </ul>
 */
public interface NapCatPlugin {

    /** 插件初始化：创建资源、注册配置等 */
    void onInit(NapCatPluginContext ctx) throws Exception;

    /**
     * 接收 OneBot 11 消息事件（message_type 存在时触发）。
     * <p>
     * 推荐重写此方法，使用强类型 {@link MessageEvent} 访问字段：
     * <pre>{@code
     *   public void onMessage(NapCatPluginContext ctx, MessageEvent event) {
     *       String text = event.getRawMessage();
     *       long senderId = event.getUserId();
     *       if (event.isGroupMessage()) {
     *           ctx.getActions().sendGroupText(
     *               String.valueOf(event.getGroupId()), "收到: " + text);
     *       }
     *   }
     * }</pre>
     *
     * @param ctx    插件上下文
     * @param event  OneBot 11 消息事件实体
     */
    default void onMessage(NapCatPluginContext ctx, MessageEvent event) throws Exception {
    }

    /**
     * 接收所有 OneBot 事件（包括消息、通知、请求、元事件）。
     * <p>
     * 使用 {@link Event} 基类统一承载，通过类型判断和 {@code as()} 转换访问具体子类型：
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
     *           if (req.isFriendRequest()) {
     *               RequestEvent.FriendRequestEvent fr = event.as(RequestEvent.FriendRequestEvent.class);
     *               ctx.getActions().setFriendAddRequest(fr.getFlag(), true, "");
     *           }
     *       }
     *   }
     * }</pre>
     *
     * @param ctx    插件上下文
     * @param event  OneBot 事件基类
     */
    default void onEvent(NapCatPluginContext ctx, Event event) throws Exception {
    }

    /** 插件卸载：释放资源、保存配置 */
    default void onCleanup(NapCatPluginContext ctx) throws Exception {
    }
}
