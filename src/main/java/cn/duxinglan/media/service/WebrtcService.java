/*
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
 */
package cn.duxinglan.media.service;

import cn.duxinglan.media.config.WebrtcConfig;
import cn.duxinglan.media.core.ChannelContext;
import cn.duxinglan.media.core.endpoint.Endpoint;
import cn.duxinglan.media.core.endpoint.EndpointFactory;
import cn.duxinglan.media.core.signaling.ISignalingChannelEvent;
import cn.duxinglan.media.core.signaling.ProtocolType;
import cn.duxinglan.media.core.signaling.SignalMessage;
import cn.duxinglan.media.core.signaling.SignalServer;
import cn.duxinglan.media.core.transport.ChannelHandler;
import cn.duxinglan.media.impl.TransportContext;
import cn.duxinglan.media.impl.TransportContextLookup;
import cn.duxinglan.media.impl.WebrtcTransportSession;
import cn.duxinglan.media.impl.signaling.WsSignalingConnection;
import cn.duxinglan.media.impl.v1.WebRtcEndpoint;
import cn.duxinglan.media.signaling.data.SignalingData;
import cn.duxinglan.media.signaling.webrtc.WebsocketSignalingBootstrap;
import cn.duxinglan.media.transport.udp.InboundPacket;
import cn.duxinglan.media.transport.udp.OutboundPacket;
import cn.duxinglan.media.transport.udp.UdpChannel;
import cn.duxinglan.room.RoomService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


@Slf4j
public class WebrtcService implements ISignalingChannelEvent<ChannelHandlerContext>, WebRtcEndpoint.WebrtcProcessorEvent, TransportContextLookup {

    private final WebsocketSignalingBootstrap websocketSignalingBootstrap;

    private final UdpChannel udpChannel;

    private final SignalServer signalServer = SignalServer.getInstance();

    /**
     * wsSignalingConnectionMap 是一个线程安全的集合，用于存储 WebSocket 信令连接的映射关系。
     * 其中键为 Netty 的 ChannelHandlerContext，用于标识具体的客户端连接，
     * 值为 WsSignalingConnection 类的实例，用于处理信道的信令消息传输。
     * <p>
     * 该变量的主要用途是在 WebRTC 信令服务中，管理客户端与服务端的信令连接关系，
     * 以支持信令的收发以及连接的生命周期管理。
     */
    private final Map<ChannelHandlerContext, WsSignalingConnection> wsSignalingConnectionMap = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    private final Map<ChannelContext, WebrtcTransportSession> webrtcNodeServerMap = new ConcurrentHashMap<>();


    private final Map<String, TransportContext> iceContextMap = new ConcurrentHashMap<>();

    private final ChannelHandlerImpl channelHandler;


    public WebrtcService(WebrtcConfig config) {
        websocketSignalingBootstrap = new WebsocketSignalingBootstrap(config.getSignalingPort(), config.getSignalingPath(), this);
        this.udpChannel = new UdpChannel(config.getWebrtcRtpPort());
        this.channelHandler = new ChannelHandlerImpl(this.udpChannel, executor);

        //注册webrtc的端点处理
        EndpointFactory.getInstance().registerEndpointCreator(ProtocolType.WEBRTC, new Supplier<Endpoint>() {
            @Override
            public Endpoint get() {
                try {
                    RoomService instance = RoomService.getInstance();
                    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint(WebrtcService.this);
                    instance.addEndpoint(webRtcEndpoint);

                    return webRtcEndpoint;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            }
        });


    }


    public void start() throws IOException {
        this.udpChannel.start();
        this.websocketSignalingBootstrap.start();
    }

    @Override
    public void onAction(ChannelHandlerContext context) {
        wsSignalingConnectionMap.put(context, new WsSignalingConnection(context));
     /*   WebrtcSignalingChannelContext webrtcSignalingChannelContext = new WebrtcSignalingChannelContext(context);
        WebrtcNodeServer webrtcNodeServer = new WebrtcNodeServer(webrtcSignalingChannelContext);
        webrtcNodeServerMap.put(context, webrtcNodeServer);*/

    }

    @Override
    public void handleSignalingData(ChannelHandlerContext context, SignalMessage signalMessage) {
        WsSignalingConnection wsSignalingConnection = wsSignalingConnectionMap.get(context);
        if (wsSignalingConnection != null) {
            signalServer.onMessage(wsSignalingConnection, signalMessage);
        }
    }

    @Override
    public void handleSignalingData(ChannelHandlerContext context, SignalingData signalingData) {

       /* WebrtcNodeServer webrtcNodeServer = webrtcNodeServerMap.get(context);
        if (webrtcNodeServer != null) {
            webrtcNodeServer.handleSignalingData(signalingData);
        }*/
    }


    @Override
    public void onClosed(ChannelHandlerContext context) {
        WsSignalingConnection remove = wsSignalingConnectionMap.remove(context);
        signalServer.onClose(remove);
    /*    WebrtcNodeServer remove = webrtcNodeServerMap.remove(context);
        if (remove != null) {
            remove.close();
        }*/
    }

    public Channel[] getChannels() {
        return new Channel[]{websocketSignalingBootstrap.getChannel()};
    }

    @Override
    public void onIceInfo(TransportContext context) {
        String ufrag = context.getLocal().ufrag();
        iceContextMap.put(ufrag, context);
        String remoteUfrag = context.getRemote().ufrag();
        iceContextMap.put(remoteUfrag, context);
    }

    @Override
    public Optional<TransportContext> findByUfrag(String ufrag) {
        //在这里获取ice的上下文
        TransportContext transportContext = iceContextMap.get(ufrag);
        return Optional.ofNullable(transportContext);
    }


    private class ChannelHandlerImpl extends ChannelHandler {


        public ChannelHandlerImpl(cn.duxinglan.media.core.Channel channel, Executor executor) {
            super(channel, executor, new RtpTransportListener());
        }


    }

    private class RtpTransportListener implements ChannelHandler.TransportListener {
        @Override
        public void read(InboundPacket inboundPacket) {
            ChannelContext channelContext = inboundPacket.channelContext();
            WebrtcTransportSession webrtcTransportSession = webrtcNodeServerMap.computeIfAbsent(channelContext, k -> new WebrtcTransportSession(channelContext, WebrtcService.this));
            webrtcTransportSession.receiveProcess(inboundPacket);
        }

        @Override
        public ByteBuf write(OutboundPacket outboundPacket) {
            ChannelContext channelContext = outboundPacket.channelContext();

            WebrtcTransportSession webrtcTransportSession = webrtcNodeServerMap.get(channelContext);
            if (webrtcTransportSession == null) {
                throw new RuntimeException("webrtcTransportSession is null");
            }

            return webrtcTransportSession.sendProcess(outboundPacket);

        }


    }


}
