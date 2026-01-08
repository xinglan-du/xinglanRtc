package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.impl.sdp.IceInfo;
import cn.duxinglan.media.impl.sdp.MediaDescriptionSpec;
import cn.duxinglan.media.impl.sdp.SdpProcessor;
import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.media.signaling.sdp.RTCSessionDescriptionInit;
import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.media.signaling.sdp.session.Bundle;
import cn.duxinglan.media.signaling.sdp.type.CodecType;
import cn.duxinglan.media.signaling.sdp.type.RTCSdpType;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.IceHandler;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.LocalIceInfo;
import cn.duxinglan.sdp.SdpParser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
@Data
@Slf4j
public class WebrtcProcessor implements SdpProcessor.SdpProcessorCallback {

    /**
     * 用于存储 WebRTC 传输会话中 DTLS 换密所需的证书密钥材料。
     */
    private final WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial;

    /**
     * 用于处理会话描述协议(SDP)的具体实现，提供解析和生成SDP的功能。
     */
    private SdpProcessor sdpProcessor;

    /**
     * 表示一个用于存储编码器类型的列表。
     * <p>
     * 该变量用于记录和管理 WebRTC 处理过程中支持的编码器类型，为媒体流的编解码操作提供支持。
     * 在初始化时默认包含 VP8 编码器类型，但可以根据需求进一步扩展和修改列表内容。
     */
    private List<CodecType> codecTypeList = List.of(CodecType.VP8);

    /**
     * 表示一个用于管理 WebRTC 媒体描述信息的映射表。
     * <p>
     * mediaDescriptionSpecMap 是一个以 MID（媒体标识符）为键，
     * MediaDescriptionSpec 对象为值的有序映射，用于存储和管理与
     * WebRTC 会话相关的媒体描述信息。
     * <p>
     * 该映射表的用途包括：
     * 1. 快速查询和获取指定 MID 对应的媒体描述配置。
     * 2. 便于在会话过程中动态添加、更新或移除媒体描述信息。
     * 3. 支持 WebRTC 发送和接收处理器的映射注册操作。
     * <p>
     * 该字段采用 LinkedHashMap 实现，确保插入顺序的有效性，
     * 同时提供较快的查找性能。
     */
    private Map<String, MediaDescriptionSpec> mediaDescriptionSpecMap = new LinkedHashMap<>();


    private AtomicInteger processorMid = new AtomicInteger(0);

    /**
     * 表示本地 ICE（Interactive Connectivity Establishment）信息的实例。
     * 此字段用于存储 WebRTC 处理过程中本地的 ICE 配置信息。
     * <p>
     * 在 WebRTC 会话中，ICE 信息包含了网络候选项列表以及与连接建立相关的其他信息，
     * 用于在端到端通信中选择最佳的网络路径。
     * <p>
     * 在 WebrtcProcessor 类中，此字段可能被用作处理本地网络连接的依据，
     * 并与远端的 ICE 信息进行配合，完成网络通信的协商及建立。
     */
    private final LocalIceInfo localIceInfo;

    private final IWebrtcProcessorEvent webrtcProcessorEvent;


    //远端描述
    private SessionDescription localSessionDescription;

    //本地描述
    private SessionDescription remoteSessionDescription;


    public WebrtcProcessor(WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial, IWebrtcProcessorEvent webrtcProcessorEvent) {
        this.keyMaterial = keyMaterial;
        this.webrtcProcessorEvent = webrtcProcessorEvent;
        this.localIceInfo = IceHandler.craterLocalIceInfo(webrtcProcessorEvent.getWebrtcNode());
        log.debug("本地的ice信息ufrag：{};pwd:{}", localIceInfo.getLocalIceInfo().ufrag(), localIceInfo.getLocalIceInfo().pwd());
        //默认offer有一个空的数据 用于交换数据
        try {
            createWebrtcSenderProcessor((MediaDescriptionSpec.SSRCDescribe) null);
            this.sdpProcessor = new SdpProcessor(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 创建一个 WebRTC 发送端处理器。
     * 根据提供的同步源标识描述信息 SSRCDescribe，生成对应的媒体描述信息对象。
     * 如果提供了同步源标识描述信息，则将其设置到该媒体描述对象中，并将该对象添加到发送处理器映射表中。
     *
     * @param ssrcDescribe 表示发送端的同步源标识描述，包含主要媒体流和重传媒体流的标识信息。
     * @return 表示 WebRTC 媒体描述信息的实例，用于指定发送端配置的相关信息。
     */
    public MediaDescriptionSpec createWebrtcSenderProcessor(MediaDescriptionSpec.SSRCDescribe ssrcDescribe) {
        MediaDescriptionSpec mediaDescriptionSpec = new MediaDescriptionSpec(getMid(), true, false);
        if (ssrcDescribe != null) {
            mediaDescriptionSpec.setSender(ssrcDescribe);
        }
        addWebrtcSenderProcessor(mediaDescriptionSpec);
        return mediaDescriptionSpec;
    }

    /**
     * 创建一个 WebRTC 发送端处理器。
     * 根据提供的同步源标识描述信息，生成对应的媒体描述对象并添加到发送处理器映射表中。
     *
     * @param primarySsrc 表示主要媒体流的同步源标识符 (SSRC)。
     * @param rtxSsrc     表示重传媒体流的同步源标识符 (SSRC)。
     * @param cname       表示用于标识发送端的 CNAME 值。
     * @param streamId    表示流的唯一标识符。
     */
    public void createWebrtcSenderProcessor(long primarySsrc, long rtxSsrc, String cname, String streamId) {
        MediaDescriptionSpec mediaDescriptionSpec = new MediaDescriptionSpec(getMid(), true, false);
        MediaDescriptionSpec.SSRCDescribe ssrcDescribe = new MediaDescriptionSpec.SSRCDescribe();
        ssrcDescribe.setPrimaryMediaStream(primarySsrc);
        ssrcDescribe.setRtxMediaStream(rtxSsrc);
        ssrcDescribe.setCname(cname);
        ssrcDescribe.setStreamId(streamId);
        mediaDescriptionSpec.setSender(ssrcDescribe);
        addWebrtcSenderProcessor(mediaDescriptionSpec);
    }


    public boolean removeWebrtcSenderProcessor(long primarySsrc, long rtxSsrc) {

        AtomicBoolean isRemove = new AtomicBoolean(false);
        Iterator<Map.Entry<String, MediaDescriptionSpec>> it =
                mediaDescriptionSpecMap.entrySet().iterator();

        for (MediaDescriptionSpec value : mediaDescriptionSpecMap.values()) {
            value.getSender().ifPresent(ssrcDescribe -> {
                if (ssrcDescribe.getPrimaryMediaStream() == primarySsrc
                    || ssrcDescribe.getRtxMediaStream() == rtxSsrc) {
                    value.closeSender();
                    isRemove.set(true);
                }
            });
        }

        return isRemove.get();
    }


    private String getMid() {
        String mid;
        do {
            mid = String.valueOf(processorMid.getAndIncrement());
        } while (mediaDescriptionSpecMap.containsKey(mid));

        return mid;
    }

    public void createWebrtcSenderProcessor(List<MediaDescriptionSpec> mediaDescriptionSpecs) {
        for (MediaDescriptionSpec mediaDescriptionSpec : mediaDescriptionSpecs) {
            createWebrtcSenderProcessor(mediaDescriptionSpec);
        }
    }

    public void createWebrtcSenderProcessor(MediaDescriptionSpec sourceMediaDescriptionSpec) {
        if (sourceMediaDescriptionSpec.isSendOnly()) {
            return;
        }
        MediaDescriptionSpec mediaDescriptionSpec = new MediaDescriptionSpec(getMid(), true, false);
        sourceMediaDescriptionSpec.getReceive().ifPresent(mediaDescriptionSpec::addSender);

        addWebrtcSenderProcessor(mediaDescriptionSpec);
    }


    /**
     * 向 `MediaDescriptionSpec` 映射表中添加一个新的 WebRTC 发送处理器。如果 `mediaDescriptionSpecMap` 中尚未包含指定的 MID，
     * 则向映射表中插入该 `MediaDescriptionSpec`，并通过 `webrtcProcessorEvent` 注册新的发送处理器。
     * <p>
     * 方向以服务器为准
     *
     * @param mediaDescriptionSpec 表示 WebRTC 媒体描述的实例，包含 MID、同步源标识等信息，用于指定需要添加的发送处理器配置。
     */
    private void addWebrtcSenderProcessor(MediaDescriptionSpec mediaDescriptionSpec) {
        if (!mediaDescriptionSpecMap.containsKey(mediaDescriptionSpec.getMid())) {
            log.debug("添加一个sdp行");
            mediaDescriptionSpecMap.put(mediaDescriptionSpec.getMid(), mediaDescriptionSpec);
        }
    }


    @Override
    public WebRTCCertificateGenerator.DTLSKeyMaterial getKeyMaterial() {
        return keyMaterial;
    }

    @Override
    public List<CodecType> getCodecTypes() {
        return codecTypeList;
    }


    @Override
    public Collection<MediaDescriptionSpec> getMediaDescriptions() {
        return mediaDescriptionSpecMap.values();
    }

    @Override
    public IceInfo getOfficeInfo() {
        return localIceInfo.getLocalIceInfo();
    }


    public void setRemoteDescription(RTCSessionDescriptionInit rtcSessionDescriptionInit) {
        SessionDescription parse = SdpParser.parse(rtcSessionDescriptionInit.sdp());
        log.info("当前远程的数据");

        if (rtcSessionDescriptionInit.type() == RTCSdpType.OFFER) {
            setRemoteDescription(sdpProcessor.strToSessionDescription(rtcSessionDescriptionInit.sdp()));
            SessionDescription answer = sdpProcessor.createAnswer();
            setLocalDescription(answer);
            webrtcProcessorEvent.onAnswer(new RTCSessionDescriptionInit(RTCSdpType.ANSWER, sdpProcessor.sessionDescriptionToStr(answer)));
        } else if (rtcSessionDescriptionInit.type() == RTCSdpType.ANSWER) {
            setRemoteDescription(sdpProcessor.strToSessionDescription(rtcSessionDescriptionInit.sdp()));
        }

    }

    public void setRemoteDescription(SessionDescription remoteSessionDescription) {
        this.remoteSessionDescription = remoteSessionDescription;
//        this.sdpProcessor.setRemoteDescription(remoteSessionDescription);
        this.localIceInfo.setRemoteIceInfo(remoteSessionDescription.getMediaDescriptions().getFirst().getIceInfo());
        log.debug("远程的ice信息ufrag：{};pwd:{}", localIceInfo.getRemoteIceInfo().ufrag(), localIceInfo.getRemoteIceInfo().pwd());


        Bundle bundle = remoteSessionDescription.getBundle();
        Set<String> midKeyList = new HashSet<>(bundle.getMid());

        mediaDescriptionSpecMap.entrySet().removeIf(entry -> {
            boolean toRemove = !midKeyList.contains(entry.getKey());
            if (toRemove) {
                webrtcProcessorEvent.removeWebrtcProducer(entry.getValue());
            }
            return toRemove;
        });


        List<MediaDescription> mediaDescriptionList = remoteSessionDescription.getMediaDescriptions();

        for (MediaDescription mediaVideoDescription : mediaDescriptionList) {
            String mId = mediaVideoDescription.getMId();
            //这里需要明确一下。虽然这个是接受的属性，但是对应整个业务来说 这里属于生产者
            MediaDescriptionSpec mediaDescriptionSpec;
            if (mediaDescriptionSpecMap.containsKey(mId)) {
                mediaDescriptionSpec = mediaDescriptionSpecMap.get(mId);
                mediaDescriptionSpec.addReceive(mediaVideoDescription.getSsrcList(), webrtcProcessorEvent.getId());
            } else {
                mediaDescriptionSpec = new MediaDescriptionSpec(mId, false, true);
                mediaDescriptionSpec.addReceive(mediaVideoDescription.getSsrcList(), webrtcProcessorEvent.getId());

                mediaDescriptionSpecMap.put(mId, mediaDescriptionSpec);
                webrtcProcessorEvent.addWebrtcProducer(mediaDescriptionSpec);
            }
        }
    }

    //设置或更新本地使用的offer
    public void setLocalDescription(SessionDescription localSessionDescription) {
        this.localSessionDescription = localSessionDescription;
//        this.sdpProcessor.setLocalDescription(localSessionDescription);
        Bundle bundle = localSessionDescription.getBundle();
        Set<String> midKeyList = new HashSet<>(bundle.getMid());

        List<MediaDescription> mediaDescriptionList = localSessionDescription.getMediaDescriptions();

        //TODO 暂时遗留 这里应该将数据结构化 。不应该直接使用
        for (MediaDescription mediaVideoDescription : mediaDescriptionList) {
            String mId = mediaVideoDescription.getMId();
            MediaDescriptionSpec mediaDescriptionSpec;
            if (mediaDescriptionSpecMap.containsKey(mId)) {
                mediaDescriptionSpec = mediaDescriptionSpecMap.get(mId);
                mediaDescriptionSpec.addSender(mediaVideoDescription.getSsrcList());
            }/* else {
                mediaDescriptionSpec = new MediaDescriptionSpec(mId, true, false);
                mediaDescriptionSpec.addSender(mediaVideoDescription.getSsrcList());
                mediaDescriptionSpecMap.put(mId, mediaDescriptionSpec);
            }*/
        }
    }

    public RTCSessionDescriptionInit createOffer() throws Exception {
        SessionDescription offer = this.sdpProcessor.createOffer();
        setLocalDescription(offer);
        return new RTCSessionDescriptionInit(RTCSdpType.OFFER, this.sdpProcessor.sessionDescriptionToStr(offer));
    }

    public List<MediaDescriptionSpec> getSenders() {
        return mediaDescriptionSpecMap.values().stream().filter(MediaDescriptionSpec::isSendOnly).filter(mediaDescriptionSpec -> mediaDescriptionSpec.getSender().isPresent()).toList();
    }


    /**
     * 定义用于处理 WebRTC 接收和发送处理器操作的事件接口。
     * 提供添加和移除接收处理器及发送处理器的方法。
     * 此接口主要用于实现对 WebRTC 处理器的动态管理。
     *
     * @author duzongyue
     * @version 1.0
     * @since 2025/11/10 21:14
     */
    public interface IWebrtcProcessorEvent {

        /**
         * 处理 WebRTC 回答 (Answer) 的方法。
         *
         * @param answer 表示 WebRTC 会话描述信息的对象，包含会话描述类型和 SDP 信息。
         */
        void onAnswer(RTCSessionDescriptionInit answer);

        /**
         * 添加一个 WebRTC 发送端描述器。
         *
         * @param mediaDescriptionSpec 表示 WebRTC 会话中媒体描述的对象，包含唯一标识符、媒体类型、
         *                             发送/接收权限以及同步源标识描述等信息。
         */
        void addWebrtcProducer(MediaDescriptionSpec mediaDescriptionSpec);


        /**
         * 移除指定的 WebRTC 发送端描述器。
         *
         * @param mediaDescriptionSpec 表示 WebRTC 会话中媒体描述的对象，包含唯一标识符、媒体类型、
         *                             发送/接收权限以及同步源标识描述等信息。
         */
        void removeWebrtcProducer(MediaDescriptionSpec mediaDescriptionSpec);

        /**
         * 获取唯一标识符。
         *
         * @return 返回当前实例的唯一标识符。
         */
        String getId();

        /**
         * 获取当前 WebRTC 节点实例的方法。
         * <p>
         * 此方法用于返回与接口实现相关联的 {@code WebrtcNode} 对象。
         * 调用者可以通过获取的实例访问具体的 WebRTC 节点功能。
         *
         * @return 返回与当前接口实现对应的 {@code WebrtcNode} 实例。
         */
        WebrtcNode getWebrtcNode();
    }

}
