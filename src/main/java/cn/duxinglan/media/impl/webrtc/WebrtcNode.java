package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.core.IConsumer;
import cn.duxinglan.media.core.IMediaNode;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.core.ISignaling;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.data.NodeSignalingData;
import cn.duxinglan.media.signaling.sdp.RTCSessionDescriptionInit;
import cn.duxinglan.media.util.UUIDUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class WebrtcNode implements IMediaNode,
        WebrtcProcessor.IWebrtcProcessorEvent {

    private final ObjectMapper objectMapper = CacheModel.getObjectMapper();
    /**
     * 标识当前 WebRTC 节点的唯一标识符。
     * <p>
     * 此变量表示一个全局唯一的节点标识符（UUID 格式，去除横线），
     * 通过调用 {@link UUIDUtils#createUUID()} 方法进行创建。
     * 其主要作用包括：
     * - 用于在信令交互、数据传输等场景中标识该节点。
     * - 保证同一网络下的节点彼此之间无需依赖额外信息即可明确身份。
     * <p>
     * 该变量为不可变（final）属性，初始化后不可更改。
     */
    private final String nodeId = UUIDUtils.createUUID();

    /**
     * 表示 WebRTC 节点的版本号。
     * 该变量标识当前 WebRTC 节点所使用的版本信息，用于版本管理和兼容性判断。
     * 其值在节点初始化时由构造函数传入，并在整个节点生命周期内保持不变。
     */
    private final int nodeVersion;

    /**
     * 表示与 WebRTC 信令、媒体处理和节点间通信相关的核心处理器。
     * <p>
     * webrtcProcessor 是一个不可变的终态变量，该变量主要用于处理与 WebRTC 节点相关的各种操作，
     * 包括信令处理、会话描述协议（SDP）的生成与解析、媒体流的发送与接收等。
     * 此处理器集成了信令机制以及支持节点对等通信的关键逻辑。
     */
    @Getter
    private final WebrtcProcessor webrtcProcessor;

    /**
     * 用于存储 DTLS (Datagram Transport Layer Security) 协议所需的密钥材料的变量。
     * 该变量通过 {@link WebRTCCertificateGenerator.DTLSKeyMaterial} 类管理，为
     * WebRTC 连接中的安全性认证和加密提供支持。
     * <p>
     * 功能和用途包括：
     * - 提供 DTLS 握手过程中所需的密钥信息。
     * - 支持 WebRTC 中安全通信（例如加密和解密 RTP/RTCP 流）所需的证书生成。
     * <p>
     * 本变量为只读属性，其内容主要在 WebRTC 初始化过程中生成，并在整个会话中使用。
     */
    @Getter
    private final WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial;

    /**
     * 表示用于处理信令交互的核心接口实现。
     * 该变量提供了信令在节点间交换的基础支持。
     * <p>
     * 功能包括：
     * 1. 负责信令的发送和接收。
     * 2. 在 WebRTC 节点间建立通信渠道时起关键作用。
     * <p>
     * 注意：
     * 该对象为不可变(final)引用，确保其在整个生命周期内不会被重新赋值。
     */
    private final ISignaling signaling;

    private final NodeFlowManager nodeFlowManager;

    private final GlobalIProducerMediaRouter globalMediaRouter;

    public WebrtcNode(ISignaling signaling, int nodeVersion) throws Exception {
        this.signaling = signaling;
        this.nodeVersion = nodeVersion;
        this.keyMaterial = WebRTCCertificateGenerator.generateDTLSKeyMaterial();
        webrtcProcessor = new WebrtcProcessor(this.keyMaterial, this);
        nodeFlowManager = new NodeFlowManager();
        globalMediaRouter = new GlobalIProducerMediaRouter(this);
    }

    @Override
    @NonNull
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public int getNodeVersion() {
        return nodeVersion;
    }


    /**
     * 发送 WebRTC 提议（Offer）信令信息的方法。
     * 方法主要生成一个 Offer 数据结构，记录其消息内容并发送给对端节点。
     * 它涉及以下逻辑：
     * 1. 通过 {@link WebrtcProcessor#createOffer} 方法创建或获取一个本地 Offer。
     * 2. 使用 {@link #sendMessage(WebrtcNodeDataType, Object)} 方法将生成的Offer内容作为消息发送。
     * 3. 捕获并记录过程中出现的任何异常。
     * <p>
     * 功能作用包括：
     * - 初始化 WebRTC P2P 连接前的 Offer 信令发送。
     * - 提供当前节点的描述性信息（包括 SDP）供对端节点解析。
     */
    public void sendOfferInfo() {
        try {
            RTCSessionDescriptionInit rtcSessionDescriptionInit = webrtcProcessor.createOffer();
//            log.info("发送offer:{}", rtcSessionDescriptionInit.sdp());
            sendMessage(WebrtcNodeDataType.NODE_OFFER, rtcSessionDescriptionInit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 发送 WebRTC 应答（Answer）信令信息的方法。
     * 此方法主要用于:
     * 1. 记录节点应答信令的信息（SDP 等）。
     * 2. 将应答内容通过 {@link #sendMessage(WebrtcNodeDataType, Object)} 方法发送至对端节点。
     *
     * @param answer 表示 WebRTC 应答信令内容的对象，包含类型（type）及会话描述协议（SDP）等信息。
     */
    private void sendAnswerInfo(RTCSessionDescriptionInit answer) {
//        log.info("发送answer:{}", answer.sdp());
        sendMessage(WebrtcNodeDataType.NODE_ANSWER, answer);
    }


    @Override
    public void init() {
        sendOfferInfo();
    }

    @Override
    public void handleNodeData(NodeSignalingData nodeSignalingData) {
        WebrtcNodeData webrtcNodeData = objectMapper.convertValue(nodeSignalingData.data(), WebrtcNodeData.class);
        switch (webrtcNodeData.type()) {
            case NODE_OFFER:
            case NODE_ANSWER:
                RTCSessionDescriptionInit rtcSessionDescriptionInit = objectMapper.convertValue(webrtcNodeData.data(), RTCSessionDescriptionInit.class);
                webrtcProcessor.setRemoteDescription(rtcSessionDescriptionInit);
        }

    }


    @Override
    public void updateOfferInfo() {
        log.debug("nodeId:{},更新描述信息", nodeId);
        sendOfferInfo();
    }


    public void sendMessage(WebrtcNodeDataType type, Object data) {
        WebrtcNodeData webrtcNodeData = new WebrtcNodeData(type, data);
        try {
            signaling.sendMessage(this, webrtcNodeData);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
    }


    @Override
    public void onAnswer(RTCSessionDescriptionInit answer) {
        sendAnswerInfo(answer);
    }


    @Override
    public void onAddWebrtcProducer(MediaLineInfo mediaLineInfo) {
        if (mediaLineInfo.isReadOnly()) {
            WebrtcMediaProducer webrtcMediaProducer = new WebrtcMediaProducer(mediaLineInfo);
            nodeFlowManager.addRtpMediaProducer(webrtcMediaProducer);
            globalMediaRouter.addProducer(webrtcMediaProducer);
        }

    }


    @Override
    public WebrtcNode getWebrtcNode() {
        return this;
    }

    //创建一个消费者
    @Override
    public IConsumer createConsumer(IProducer producer) {
        long primarySsrc = SsrcGenerator.generateSsrc();
//        long primarySsrc = producer.getPrimarySsrc();
        long rtxSsrc = SsrcGenerator.generateSsrc();
//        long rtxSsrc = producer.getRtxSsrc();
        MediaLineInfo producerMediaLineInfo = producer.getMediaLineInfo();
        MediaLineInfo consumerMediaLineInfo = this.webrtcProcessor.createWebrtcSenderProcessor(producerMediaLineInfo);
        WebrtcMediaConsumer webrtcMediaConsumer = new WebrtcMediaConsumer(consumerMediaLineInfo);
        nodeFlowManager.addRtpMediaConsumer(webrtcMediaConsumer);
        return webrtcMediaConsumer;
    }

    @Override
    public boolean removeConsumer(IConsumer consumer) {
        nodeFlowManager.removeRtpMediaConsumer(consumer);
        return this.webrtcProcessor.removeWebrtcSenderProcessor(consumer.getMediaLineInfo());
    }

    @Override
    public void close() {
        nodeFlowManager.close();
        globalMediaRouter.close();

    }

    @Override
    public NodeFlowManager getNodeFlowManager() {
        return nodeFlowManager;
    }

    @Override
    public GlobalIProducerMediaRouter getGlobalMediaRouter() {
        return globalMediaRouter;
    }

    @Override
    public WebRTCCertificateGenerator.DTLSKeyMaterial getDTLSKeyMaterial() {
        return getKeyMaterial();
    }


}
