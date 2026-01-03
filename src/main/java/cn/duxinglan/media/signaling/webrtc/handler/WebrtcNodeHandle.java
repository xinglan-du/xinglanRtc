package cn.duxinglan.media.signaling.webrtc.handler;

import cn.duxinglan.media.core.MediaConnection;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.data.SignalingData;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
public class WebrtcNodeHandle extends SimpleChannelInboundHandler<WebSocketFrame>  {

    private final ObjectMapper objectMapper = CacheModel.getObjectMapper();

    private MediaConnection mediaConnection;

    private ChannelHandlerContext ctx;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        mediaConnection = new MediaConnection(ctx);
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        if (msg instanceof TextWebSocketFrame textWebSocketFrame) {
            SignalingData signalingData = objectMapper.readValue(textWebSocketFrame.text(), SignalingData.class);
            signalingProcessor(signalingData, ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        mediaConnection.close();
    }




    /**
     * 处理信令数据的方法。
     * 根据信令类型执行相应的动作，例如创建新媒体节点或向现有节点添加信令数据。
     *
     * @param signalingData 表示信令的数据对象，包含信令类型和相关数据。
     * @param ctx           Netty的ChannelHandler上下文，用于处理网络事件。
     */
    private void signalingProcessor(SignalingData signalingData, ChannelHandlerContext ctx) {
        mediaConnection.handleSignalingData(signalingData);
    }





}
