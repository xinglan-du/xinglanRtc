package cn.duxinglan.media.core;

import cn.duxinglan.media.impl.webrtc.WebrtcNode;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.data.CreateNodeData;
import cn.duxinglan.media.signaling.data.NodeSignalingData;
import cn.duxinglan.media.signaling.data.SignalingData;
import cn.duxinglan.media.signaling.type.RtpTransportType;
import cn.duxinglan.media.signaling.type.SignalingType;
import cn.duxinglan.room.RoomService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MediaConnection类表示一个媒体信令连接的抽象。
 * 它用于处理接收到的信令数据，并负责初始化和管理媒体节点实例。
 * 通过信令接口（ISignaling）的实现，支持发送信令到远程对端。
 * <p>
 * 此类依赖若干外部组件，包括ObjectMapper用于JSON处理和ChannelHandlerContext用于网络通信。
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
@Slf4j
@Data
public class MediaConnection implements ISignaling {

    private final ChannelHandlerContext ctx;

    private final ObjectMapper objectMapper = CacheModel.getObjectMapper();

    private final Map<String, IMediaNode> mediaNodeMap = new ConcurrentHashMap<>();

    public MediaConnection(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }


    public void handleSignalingData(SignalingData signalingData) {
        SignalingType type = signalingData.type();
        if (type == SignalingType.INIT) {
            CreateNodeData createNodeData = objectMapper.convertValue(signalingData.data(), CreateNodeData.class);
            IMediaNode node = createNode(createNodeData.transportType(), createNodeData.version(), this);
            mediaNodeMap.put(node.getNodeId(), node);
            RoomService.getInstance().addMediaNode(node);

            try {
                sendMessage(new SignalingData(SignalingType.INIT, new CreateNodeData(RtpTransportType.WEBRTC, node.getNodeVersion(), node.getNodeId())));
                node.init();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        } else if (type == SignalingType.NODE) {
            NodeSignalingData nodeSignalingData = objectMapper.convertValue(signalingData.data(), NodeSignalingData.class);
            IMediaNode mediaNode = mediaNodeMap.get(nodeSignalingData.nodeId());
            if (mediaNode != null) {
                mediaNode.handleNodeData(nodeSignalingData);
            }
        }
    }


    /**
     * 创建一个媒体节点实例。
     * 根据传入的RTP传输类型构造相应的媒体节点实例。如果传输类型为WEBRTC，则返回一个WebrtcNode对象。
     * 否则返回null。
     *
     * @param rtpTransportType 表示RTP传输类型，用于决定创建何种类型的节点。
     * @param version          版本号，用于扩展的备用参数。
     * @param signaling        指定的信令处理接口，用于初始化媒体节点。
     * @return 一个实现了IMediaNode接口的媒体节点实例，如果传输类型为未支持的类型则返回null。
     */
    private IMediaNode createNode(RtpTransportType rtpTransportType, int version, ISignaling signaling) {
        if (rtpTransportType == RtpTransportType.WEBRTC) {
            try {
                return new WebrtcNode(signaling, version);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
        return null;
    }


    @Override
    public void sendMessage(IMediaNode node, Object data) throws JsonProcessingException {
        NodeSignalingData nodeSignalingData = new NodeSignalingData(node.getNodeId(), data);
        sendMessage(new SignalingData(SignalingType.NODE, nodeSignalingData));
    }

    private void sendMessage(SignalingData signalingData) throws JsonProcessingException {
        String text = CacheModel.getObjectMapper().writeValueAsString(signalingData);
        ctx.writeAndFlush(new TextWebSocketFrame(text));
    }

    public void close() {
        mediaNodeMap.values().forEach(new Consumer<IMediaNode>() {
            @Override
            public void accept(IMediaNode mediaNode) {
                RoomService.getInstance().removeMediaNode(mediaNode);
                mediaNode.close();
            }
        });

    }
}
