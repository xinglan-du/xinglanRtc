package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.media.impl.sdp.IceInfo;
import cn.duxinglan.media.impl.sdp.MediaDescriptionSpec;
import cn.duxinglan.media.impl.sdp.SdpProcessor;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.sdp.RTCSessionDescriptionInit;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.IceHandler;
import cn.duxinglan.media.transport.nio.webrtc.handler.ice.LocalIceInfo;
import cn.duxinglan.sdp.entity.MediaDescription;
import cn.duxinglan.sdp.entity.SessionDescription;
import cn.duxinglan.sdp.entity.media.Connection;
import cn.duxinglan.sdp.entity.media.Fingerprint;
import cn.duxinglan.sdp.entity.media.Info;
import cn.duxinglan.sdp.entity.media.RtcpConnection;
import cn.duxinglan.sdp.entity.rtp.FmtpAttributes;
import cn.duxinglan.sdp.entity.rtp.RtcpFeedback;
import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.session.*;
import cn.duxinglan.sdp.entity.ssrc.SSRC;
import cn.duxinglan.sdp.entity.ssrc.SsrcGroup;
import cn.duxinglan.sdp.entity.type.CodecType;
import cn.duxinglan.sdp.entity.type.MediaInfoType;
import cn.duxinglan.sdp.entity.type.RTCSdpType;
import cn.duxinglan.sdp.parse.SdpParser;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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
//    private Map<String, MediaDescriptionSpec> mediaDescriptionSpecMap = new LinkedHashMap<>();

    /**
     * 媒体描述规格映射表，用于保存 WebRTC 中的媒体描述信息，按 `mid` 键值映射。
     * 每个主键 `mid` 对应一个唯一的字符串标识。其值是一个内部嵌套的映射表，进一步通过
     * 长整型键值（通常是同步源标识符 SSRC 的值）与具体的 {@link MediaLineInfo} 实例相关联。
     * <p>
     * 主要用途：
     * 1. 管理与维护发送端和接收端的 WebRTC 媒体描述信息。
     * 2. 建立基于 MID（媒体标识符）和特定 SSRC（同步源标识符）的多级映射关系。
     * 3. 用于快速检索、注册和删除与 MID 和 SSRC 相关联的媒体描述信息。
     * <p>
     * 数据结构：
     * - 外层 Map 的键是媒体标识符（`mid`），表示媒体处理器的唯一标识。
     * - 内层 Map 的键是与媒体描述相关联的 SSRC（同步源标识符）。
     * - 内层 Map 的值是表示媒体描述具体配置的实例 {@link MediaLineInfo}。
     * <p>
     * 典型应用场景示例：
     * - 通过 MID 和 SSRC 标识一个 WebRTC 媒体发送端或接收端的描述。
     * - 同时记录视频流、音频流及其对应的重传流等信息，支持多媒体流配置。
     * <p>
     * 线程安全性：
     * 该字段使用 `LinkedHashMap` 实现，同时其线程安全性需由调用者在并发操作场景下额外保证。
     */
    private Map<String, MediaLineInfo> mediaLineInfoMap = new LinkedHashMap<>();


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


    /**
     * 表示WebRTC处理器中本地会话描述信息的变量。
     * <p>
     * localSessionDescription包含通过WebRTC协议生成的本地Session Description Protocol (SDP)信息。
     * 它定义了本地端的媒体配置，如音频和视频编码类型、网络候选人等。
     * 该变量通常用于WebRTC的信令阶段，与远端会话描述信息配对，协商完成媒体数据的传输参数。
     */
    private SessionDescription localSessionDescription;

    /**
     * 表示远端的会话描述信息。
     * 该变量用于存储远端传递的会话描述(Session Description Protocol, SDP)，
     * 其内容中包含了连接协商过程中必要的信息，如媒体类型、编码格式、网络地址等。
     * 这个会话描述通常在 WebRTC 的信令流程中由对等方发送，用于设置或更新 WebRTC 连接的远程会话配置。
     */
    private SessionDescription remoteSessionDescription;


    private Map<Integer, RtpPayload> rtpPayloads = new LinkedHashMap<>();

    public WebrtcProcessor(WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial, IWebrtcProcessorEvent webrtcProcessorEvent) throws Exception {
        this.keyMaterial = keyMaterial;
        this.webrtcProcessorEvent = webrtcProcessorEvent;
        this.localIceInfo = IceHandler.craterLocalIceInfo(webrtcProcessorEvent.getWebrtcNode());
        initRtpPayloads();
        log.debug("本地的ice信息ufrag：{};pwd:{}", localIceInfo.getLocalIceInfo().getUfrag(), localIceInfo.getLocalIceInfo().getPwd());
        //默认offer有一个空的数据 用于交换数据
        try {
            MediaLineInfo nullVideoMediaLineInfo = WebrtcSdpDefault.createNullVideoMediaLineInfo(getMid());
            addWebrtcSenderProcessor(nullVideoMediaLineInfo);
//            createWebrtcSenderProcessor((MediaDescriptionSpec.SSRCDescribe) null);
            this.sdpProcessor = new SdpProcessor(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initRtpPayloads() {
        RtpPayload vp8RtpPayload = WebrtcSdpDefault.defaultVp8RtpPayload();
        RtpPayload vp8RtxRtpPayload = WebrtcSdpDefault.defaultVp8RtxRtpPayload();
        this.rtpPayloads.put(vp8RtpPayload.getPayloadType(), vp8RtpPayload);
        this.rtpPayloads.put(vp8RtxRtpPayload.getPayloadType(), vp8RtxRtpPayload);
    }

    private SessionDescription createLocalSessionDescription() throws Exception {
        SessionDescription offer = new SessionDescription();
        offer.setVersion(Version.defaultVersion());
        offer.setOrigin(Origin.defaultOrigin("-", CacheModel.getLocalAddress()));
        offer.setSessionName(SessionName.defaultSessionName(null));
        offer.setTiming(Timing.defaultTiming());

        Bundle bundle = Bundle.defaultBundle();
        offer.setBundle(bundle);
        offer.setExtMapAllowMixed(ExtMapAllowMixed.defaultExtMapAllowMixed(true));
        offer.setMSid(MSid.defaultMsid());

        for (Map.Entry<String, MediaLineInfo> mediaInfoMap : mediaLineInfoMap.entrySet()) {
            bundle.addMid(mediaInfoMap.getKey());
            MediaDescription mediaDescription = createMediaDescription(mediaInfoMap.getValue());
            offer.addMediaDescription(mediaDescription);
        }

        return offer;
    }

    private MediaDescription createMediaDescription(MediaLineInfo mediaLineInfo) throws Exception {
        MediaDescription mediaDescription = new MediaDescription();
        mediaDescription.setInfo(WebrtcSdpDefault.defaultInfo(mediaLineInfo.getMediaInfoType()));
        mediaDescription.setConnection(WebrtcSdpDefault.defaultConnection());
        mediaDescription.setRtcpConnection(WebrtcSdpDefault.defaultRtcpConnection());
        mediaDescription.setIceInfo(localIceInfo.getLocalIceInfo());
        mediaDescription.setFingerprint(WebrtcSdpDefault.defaultFingerprint(keyMaterial.getFingerprint()));
        mediaDescription.setSetup(WebrtcSdpDefault.defaultSetup());
        mediaDescription.setMId(WebrtcSdpDefault.defaultMid(mediaLineInfo.getMid()));
        mediaDescription.setMediaDirection(WebrtcSdpDefault.defaultMediaDirection(mediaLineInfo));
        mediaDescription.setRtcpMux(WebrtcSdpDefault.defaultRtcpMux());


        for (Map.Entry<Integer, RtpPayload> rtpPayloadEntry : rtpPayloads.entrySet()) {
            mediaDescription.addRtpPayload(rtpPayloadEntry.getValue());
        }

        if (mediaLineInfo.getSendInfo() != null) {
            MediaLineInfo.Info sendInfo = mediaLineInfo.getSendInfo();
            if (sendInfo.getSsrcMap() != null) {
                Map<Long, SSRC> ssrcMap = sendInfo.getSsrcMap();
                List<SsrcGroup> ssrcGroups = sendInfo.getSsrcGroups();
                mediaDescription.setSsrcGroups(ssrcGroups);
                mediaDescription.setSsrcMap(ssrcMap);
                for (Map.Entry<Long, SSRC> longSSRCEntry : ssrcMap.entrySet()) {
                    SSRC value = longSSRCEntry.getValue();
                    mediaDescription.setMsid(WebrtcSdpDefault.defaultMsid(value.getStreamId()));
                }
            }
        }


        return mediaDescription;
    }


    public void createWebrtcSenderProcessor(MediaLineInfo producerMediaLineInfo) {
        MediaLineInfo.Info readInfo = producerMediaLineInfo.getReadInfo();
        MediaLineInfo.Info info = new MediaLineInfo.Info();
        info.setSsrcMap(readInfo.getSsrcMap());
        info.setSsrcGroups(readInfo.getSsrcGroups());
        info.setRtpPayloads(readInfo.getRtpPayloads());
        MediaLineInfo mediaLineInfo = new MediaLineInfo(producerMediaLineInfo.getMediaInfoType(), getMid(), true, false);
        mediaLineInfo.setSendInfo(info);


        addWebrtcSenderProcessor(mediaLineInfo);
    }


    public boolean removeWebrtcSenderProcessor(MediaLineInfo mediaLineInfo) {
        boolean isRemove = false;

        if (mediaLineInfo.getSendInfo() != null) {
            MediaLineInfo.Info sendInfo = mediaLineInfo.getSendInfo();
            for (Long ssrc : sendInfo.getSsrcMap().keySet()) {
                for (MediaLineInfo value : mediaLineInfoMap.values()) {
                    if (value.getSendInfo().getSsrcMap().containsKey(ssrc)) {
                        value.closeSender();
                        isRemove = true;
                    }
                }
            }

        }


        return isRemove;

       /* Iterator<Map.Entry<String, MediaDescriptionSpec>> it =
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

        return isRemove.get();*/
    }


    private String getMid() {
        String mid;
        do {
            mid = String.valueOf(processorMid.getAndIncrement());
        } while (mediaLineInfoMap.containsKey(mid));

        return mid;
    }

 /*   public void createWebrtcSenderProcessor(List<MediaLineInfo> mediaLineInfos) {
        for (MediaLineInfo mediaLineInfo : mediaLineInfos) {
            createWebrtcSenderProcessor(mediaLineInfo);
        }
    }
*/
  /*  public void createWebrtcSenderProcessor(MediaLineInfo mediaLineInfo) {
        if (mediaLineInfo.isSendOnly()) {
            return;
        }
        MediaLineInfo mediaSsrc = new MediaLineInfo(mediaLineInfo.getMediaInfoType(), getMid(), true, false);
     *//*   MediaDescriptionSpec mediaDescriptionSpec = new MediaDescriptionSpec(getMid(), true, false);
        sourceMediaDescriptionSpec.getReceive().ifPresent(mediaDescriptionSpec::addSender);*//*

        addWebrtcSenderProcessor(mediaSsrc);
    }*/


    private void addWebrtcSenderProcessor(MediaLineInfo mediaLineInfo) {
        if (!mediaLineInfoMap.containsKey(mediaLineInfo.getMid())) {
            log.debug("添加一个sdp行");
            mediaLineInfoMap.put(mediaLineInfo.getMid(), mediaLineInfo);
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
//        return mediaDescriptionSpecMap.values();
        return null;
    }

    @Override
    public IceInfo getOfficeInfo() {
        return localIceInfo.getLocalIceInfo();
    }


    public void setRemoteDescription(RTCSessionDescriptionInit rtcSessionDescriptionInit) {
        SessionDescription parse = SdpParser.parse(rtcSessionDescriptionInit.sdp());
//        SessionDescription sessionDescription = sdpProcessor.strToSessionDescription(rtcSessionDescriptionInit.sdp());
        setRemoteDescription(parse);
        if (rtcSessionDescriptionInit.type() == RTCSdpType.OFFER) {
            SessionDescription answer = null;
            try {
                answer = createLocalSessionDescription();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }


//            SessionDescription answer = sdpProcessor.createAnswer();
            setLocalDescription(answer);
            webrtcProcessorEvent.onAnswer(new RTCSessionDescriptionInit(RTCSdpType.ANSWER, sessionDescriptionToWebrtcStr(answer)));
        } else if (rtcSessionDescriptionInit.type() == RTCSdpType.ANSWER) {
            //暂时不需要做任何处理
        }

    }

    public void setRemoteDescription(SessionDescription remoteSessionDescription) {
        this.remoteSessionDescription = remoteSessionDescription;
//        this.sdpProcessor.setRemoteDescription(remoteSessionDescription);
        this.localIceInfo.setRemoteIceInfo(remoteSessionDescription.getMediaDescriptions().getFirst().getIceInfo());
        log.debug("远程的ice信息ufrag：{};pwd:{}", localIceInfo.getRemoteIceInfo().getUfrag(), localIceInfo.getRemoteIceInfo().getPwd());


//        Bundle bundle = remoteSessionDescription.getBundle();
//        Set<String> midKeyList = new HashSet<>(bundle.getMid());

     /*   mediaDescriptionSpecMap.entrySet().removeIf(entry -> {
            boolean toRemove = !midKeyList.contains(entry.getKey());
            if (toRemove) {
                webrtcProcessorEvent.removeWebrtcProducer(entry.getValue());
            }
            return toRemove;
        });*/


        List<MediaDescription> mediaDescriptionList = remoteSessionDescription.getMediaDescriptions();
        for (MediaDescription mediaDescription : mediaDescriptionList) {
            boolean isAdd = false;
            MediaInfoType type = mediaDescription.getInfo().type();
            String mId = mediaDescription.getMId().id();
            MediaLineInfo mediaLineInfo;
            //先处理当前媒体行是否存在
            if (mediaLineInfoMap.containsKey(mId)) {
                mediaLineInfo = mediaLineInfoMap.get(mId);
            } else {
                mediaLineInfo = new MediaLineInfo(type, mId, false, true);
                mediaLineInfoMap.put(mId, mediaLineInfo);
                isAdd = true;
            }

            //处理当前媒体行所包含的ssrc，一个媒体行就是一组ssrc的mediaSsrcInfo
            //TODO 暂时不考虑ssrc修改的问题
            Map<Long, SSRC> ssrcMap = mediaDescription.getSsrcMap();
            MediaLineInfo.Info readInfo = new MediaLineInfo.Info();

            readInfo.setSsrcMap(ssrcMap);
            readInfo.setRtpPayloads(mediaDescription.getRtpPayloads());
            readInfo.setSsrcGroups(mediaDescription.getSsrcGroups());
            mediaLineInfo.setReadInfo(readInfo);
            if (isAdd) {
                webrtcProcessorEvent.onAddWebrtcProducer(mediaLineInfo);
            }
        }



        /*for (MediaDescription mediaVideoDescription : mediaDescriptionList) {
            String mId = mediaVideoDescription.getMId().getId();
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
        }*/
    }

    //设置或更新本地使用的offer
    public void setLocalDescription(SessionDescription localSessionDescription) {
        this.localSessionDescription = localSessionDescription;
//        this.sdpProcessor.setLocalDescription(localSessionDescription);
        Bundle bundle = localSessionDescription.getBundle();
        Set<String> midKeyList = new HashSet<>(bundle.getMid());

        List<MediaDescription> mediaDescriptionList = localSessionDescription.getMediaDescriptions();


        //TODO 暂时遗留 这里应该将数据结构化 。不应该直接使用
        for (MediaDescription mediaDescription : mediaDescriptionList) {
            String mId = mediaDescription.getMId().id();

            MediaLineInfo mediaLineInfo;
            //先处理当前媒体行是否存在
            if (mediaLineInfoMap.containsKey(mId)) {
                mediaLineInfo = mediaLineInfoMap.get(mId);
                MediaLineInfo.Info sendInfo = mediaLineInfo.getSendInfo();
                if (sendInfo != null) {
                    sendInfo.setSsrcMap(mediaDescription.getSsrcMap());
                    sendInfo.setRtpPayloads(mediaDescription.getRtpPayloads());
                    sendInfo.setSsrcGroups(mediaDescription.getSsrcGroups());
                }

            }



         /*   MediaDescriptionSpec mediaDescriptionSpec;
            if (mediaDescriptionSpecMap.containsKey(mId)) {
                mediaDescriptionSpec = mediaDescriptionSpecMap.get(mId);
                mediaDescriptionSpec.addSender(mediaVideoDescription.getSsrcList());
            } else {
                mediaDescriptionSpec = new MediaDescriptionSpec(mId, true, false);
                mediaDescriptionSpec.addSender(mediaVideoDescription.getSsrcList());
                mediaDescriptionSpecMap.put(mId, mediaDescriptionSpec);
            }*/
        }
    }

    public RTCSessionDescriptionInit createOffer() throws Exception {
        SessionDescription localSessionDescription = createLocalSessionDescription();
//        SessionDescription offer = this.sdpProcessor.createOffer();
        setLocalDescription(localSessionDescription);
        return new RTCSessionDescriptionInit(RTCSdpType.OFFER, sessionDescriptionToWebrtcStr(this.localSessionDescription));
    }

/*    public List<MediaDescriptionSpec> getSenders() {
        return mediaDescriptionSpecMap.values().stream().filter(MediaDescriptionSpec::isSendOnly).filter(mediaDescriptionSpec -> mediaDescriptionSpec.getSender().isPresent()).toList();
    }*/


    public static String sessionDescriptionToWebrtcStr(SessionDescription offer) {
        StringBuilder sb = new StringBuilder();
        Version version = offer.getVersion();
        sb.append(String.format("v=%d", version.version())).append("\r\n");
        Origin origin = offer.getOrigin();
        sb.append(String.format("o=%s %s %d %s %s %s", origin.username(), origin.sessionId(), origin.sessionVersion(), origin.networkType(), origin.addressType(), origin.unicastAddress())).append("\r\n");
        SessionName sessionName = offer.getSessionName();
        sb.append(String.format("s=%s", sessionName.value())).append("\r\n");
        Timing timing = offer.getTiming();
        sb.append(String.format("t=%d %d", timing.startTime(), timing.endTime())).append("\r\n");
        sb.append("a=ice-lite").append("\r\n");
        Bundle bundle = offer.getBundle();
        if (!bundle.getMid().isEmpty()) {
            sb.append(String.format("a=group:BUNDLE %s", String.join(" ", bundle.getMid()))).append("\r\n");
        }
        ExtMapAllowMixed extMapAllowMixed = offer.getExtMapAllowMixed();
        if (extMapAllowMixed.value()) {
            sb.append("a=extmap-allow-mixed").append("\r\n");
        }
        MSid mSid = offer.getMSid();
        sb.append(String.format("a=msid-semantic: %s %s", mSid.type().value, mSid.getSDPLine())).append("\r\n");
        for (MediaDescription mediaVideoDescription : offer.getMediaDescriptions()) {
            Info info = mediaVideoDescription.getInfo();
            Map<Integer, RtpPayload> rtpPayloads = mediaVideoDescription.getRtpPayloads();
            sb.append(String.format("m=%s %d %s", info.type().value, info.port(), info.transportType().value));
            for (Integer i : rtpPayloads.keySet()) {
                sb.append(" ").append(i);
            }
            sb.append("\r\n");

            //网络连接信息
            Connection connection = mediaVideoDescription.getConnection();
            sb.append(String.format("c=%s %s %s", connection.netType().value, connection.ipVerType().value, connection.address())).append("\r\n");

            //RTCP连接信息
            RtcpConnection rtcpConnection = mediaVideoDescription.getRtcpConnection();
            sb.append(String.format("a=rtcp:%d %s %s %s", rtcpConnection.port(), rtcpConnection.netType().value, rtcpConnection.ipVerType().value, rtcpConnection.address())).append("\r\n");

            //ICE信息
            IceInfo iceInfo = mediaVideoDescription.getIceInfo();
            sb.append(String.format("a=ice-ufrag:%s", iceInfo.getUfrag())).append("\r\n");
            sb.append(String.format("a=ice-pwd:%s", iceInfo.getPwd())).append("\r\n");
            sb.append(String.format("a=ice-options:%s", iceInfo.getOptions())).append("\r\n");

            //证书信息
            Fingerprint fingerprint = mediaVideoDescription.getFingerprint();
            sb.append(String.format("a=fingerprint:%s %s", fingerprint.type().value, fingerprint.finger())).append("\r\n");
            sb.append(String.format("a=setup:%s", mediaVideoDescription.getSetup().type().value)).append("\r\n");
            sb.append(String.format("a=mid:%s", mediaVideoDescription.getMId().id())).append("\r\n");
            sb.append(String.format("a=%s", mediaVideoDescription.getMediaDirection().type().value)).append("\r\n");
            String msid = null;
            if (mediaVideoDescription.getMsid() != null) {
                msid = String.format("stream%s track-%s", mediaVideoDescription.getMsid().streamId(), mediaVideoDescription.getMId().id());
                sb.append("a=msid:").append(msid).append("\r\n");
            }
            if (mediaVideoDescription.getRtcpMux() != null && mediaVideoDescription.getRtcpMux().enabled()) {
                sb.append("a=rtcp-mux").append("\r\n");
            }
            sb.append(CacheModel.getLocalCandidate().toSdpStr()).append("\r\n");

            StringBuilder mediaSb = new StringBuilder();
            for (RtpPayload rtpPayload : rtpPayloads.values()) {
                mediaSb.append(String.format("a=rtpmap:%d %s/%d", rtpPayload.getPayloadType(), rtpPayload.getEncodingName(), rtpPayload.getClockRate())).append("\r\n");
                if (rtpPayload.getRtcpFeedbacks() != null) {
                    for (RtcpFeedback rtcpFeedback : rtpPayload.getRtcpFeedbacks()) {
                        mediaSb.append("a=rtcp-fb:").append(rtpPayload.getPayloadType());
                        if (rtcpFeedback.getRtcpFeedbackType() != null) {
                            mediaSb.append(" ").append(rtcpFeedback.getRtcpFeedbackType().value);
                        }
                        if (rtcpFeedback.getRtcpFeedbackParam() != null) {
                            mediaSb.append(" ").append(rtcpFeedback.getRtcpFeedbackParam().value);
                        }
                        mediaSb.append("\r\n");
                     /*   mediaSb.append(String.format("a=rtcp-fb:%d nack", rtpPayload.getPayloadType())).append("\r\n");
                        mediaSb.append(String.format("a=rtcp-fb:%d nack pli", codec.getPayloadType())).append("\r\n");
                        mediaSb.append(String.format("a=rtcp-fb:%d ccm fir", codec.getPayloadType())).append("\r\n");*/
                    }
                }

                if (rtpPayload.getFmtp() != null) {
                    FmtpAttributes fmtp = rtpPayload.getFmtp();
                    if (fmtp.getAssociatedPayloadType() != null) {
                        mediaSb.append(String.format("a=fmtp:%d apt=%d", rtpPayload.getPayloadType(), fmtp.getAssociatedPayloadType())).append("\r\n");
                    }
                    if (!fmtp.getParams().isEmpty()) {
                        mediaSb.append("a=fmtp:").append(rtpPayload.getPayloadType());
                        for (Map.Entry<String, String> stringStringEntry : fmtp.getParams().entrySet()) {
                            mediaSb.append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue()).append(";");
                        }

                        mediaSb.append("\r\n");
                    }
                }


//                mediaSb.append(String.format("a=rtpmap:%d rtx/%d", codec.getRetransmitPayloadType(), codec.getClockRate())).append("\r\n");
//                mediaSb.append(String.format("a=fmtp:%d apt=%d", codec.getRetransmitPayloadType(), codec.getPayloadType())).append("\r\n");
            }

            sb.append(mediaSb);


            List<SsrcGroup> ssrcGroups = mediaVideoDescription.getSsrcGroups();
            for (SsrcGroup ssrcGroup : ssrcGroups) {
                sb.append("a=ssrc-group:").append(ssrcGroup.getSsrcGroupType().value);
                for (Long l : ssrcGroup.getSsrcList()) {
                    sb.append(" ").append(l);
                }
                sb.append("\r\n");
//                sb.append(String.format("a=ssrc-group:%s %d %d", ssrcGroup.getSsrcGroupType().value, ssrcGroup.getSsrcList())).append("\r\n");
            }
            for (SSRC ssrc : mediaVideoDescription.getSsrcMap().values()) {
                sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getSsrc(), ssrc.getCname())).append("\r\n");
                if (ssrc.getStreamId() != null) {
                    sb.append(String.format("a=ssrc:%d msid:%s", ssrc.getSsrc(), msid)).append("\r\n");
                }
            }


          /*  List<SSRC> ssrcList = mediaVideoDescription.getSsrcList();
            for (SSRC ssrc : ssrcList) {
                sb.append(String.format("a=ssrc-group:FID %d %d", ssrc.getPrimaryMediaStream(), ssrc.getRtxMediaStream())).append("\r\n");
                if (msid == null) {
                    sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getPrimaryMediaStream(), ssrc.getCname())).append("\r\n");
                    sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getRtxMediaStream(), ssrc.getCname())).append("\r\n");
                } else {
                    sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getPrimaryMediaStream(), ssrc.getCname())).append("\r\n");
                    sb.append(String.format("a=ssrc:%d msid:%s", ssrc.getPrimaryMediaStream(), msid)).append("\r\n");
                    sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getRtxMediaStream(), ssrc.getCname())).append("\r\n");
                    sb.append(String.format("a=ssrc:%d msid:%s", ssrc.getRtxMediaStream(), msid)).append("\r\n");
                }


            }*/
        }
        return sb.toString();
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

        void onAddWebrtcProducer(MediaLineInfo mediaInfo);

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
