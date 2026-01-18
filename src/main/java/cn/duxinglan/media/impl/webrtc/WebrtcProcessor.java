package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.IceInfo;
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
public class WebrtcProcessor {

    /**
     * 用于存储 WebRTC 传输会话中 DTLS 换密所需的证书密钥材料。
     */
    private final WebRTCCertificateGenerator.DTLSKeyMaterial keyMaterial;


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


    private Map<Integer, RtpPayload> videoRtpPayloads = new LinkedHashMap<>();

    private Map<Integer, RtpPayload> audioRtpPayloads = new LinkedHashMap<>();

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
            MediaLineInfo nullAudioMediaLineInfo = WebrtcSdpDefault.createNullAudioMediaLineInfo(getMid());
            addWebrtcSenderProcessor(nullAudioMediaLineInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 初始化 RTP 负载类型配置。
     * <p>
     * 此方法用于将默认的音视频 RTP 负载类型填充到对应的 `audioRtpPayloads` 和 `videoRtpPayloads` 映射中，
     * 以支持 WebRTC 会话的 RTP 协商。
     * <p>
     * 功能描述：
     * 1. 为视频流配置默认的 VP8 和 VP8 RTX 的 RTP 负载类型。
     * 2. 为音频流配置默认的 OPUS 和 OPUS RED 的 RTP 负载类型。
     * 3. 将每个 RTP 负载类型按照其 payload type 值映射到对应的位置。
     * <p>
     * 注意事项：
     * - 该方法需要依赖默认配置类 `WebrtcSdpDefault` 提供 VP8、VP8 RTX、OPUS 和 OPUS RED
     * 等 RTP 负载类型的定义。
     * - RTP 配置必须满足 WebRTC 规范要求，并与远端设备协同工作。
     * <p>
     * 设计用途：
     * - 此方法通常在初始化阶段调用，用于完成 RTP 支持的预设。
     * - 配置完成后，`audioRtpPayloads` 和 `videoRtpPayloads` 将作为 RTP 协商的参考基础，
     * 用于构建或解析 SDP 描述。
     */
    private void initRtpPayloads() {
        RtpPayload vp8RtpPayload = WebrtcSdpDefault.defaultVp8RtpPayload();
        RtpPayload vp8RtxRtpPayload = WebrtcSdpDefault.defaultVp8RtxRtpPayload();
        this.videoRtpPayloads.put(vp8RtpPayload.getPayloadType(), vp8RtpPayload);
        this.videoRtpPayloads.put(vp8RtxRtpPayload.getPayloadType(), vp8RtxRtpPayload);


        RtpPayload opusRtpPayload = WebrtcSdpDefault.defaultOpus();
        RtpPayload opusRedRtpPayload = WebrtcSdpDefault.defaultOpusRed();
        this.audioRtpPayloads.put(opusRtpPayload.getPayloadType(), opusRtpPayload);
        this.audioRtpPayloads.put(opusRedRtpPayload.getPayloadType(), opusRedRtpPayload);

    }

    /**
     * 创建本地会话描述(Session Description)的方法。
     * <p>
     * 此方法用于生成一个包含本地媒体会话信息的 SessionDescription 对象。
     * 生成的会话描述符合 WebRTC SDP 协议的要求，包含基本的会话信息及媒体描述信息。
     * <p>
     * 功能描述：
     * - 初始化会话描述的版本、源、会话名称、时间等基本字段。
     * - 生成 BUNDLE 和扩展映射配置信息。
     * - 根据现有媒体行信息生成相应的媒体描述并进行绑定。
     * <p>
     * 注意事项：
     * - 方法依赖默认配置类 WebrtcSdpDefault 为会话描述填充默认值。
     * - 如果媒体信息映射为空或异常，会导致对应的媒体描述生成失败。
     * - 在执行过程中可能会抛出异常，使用方需要通过异常处理机制捕获。
     *
     * @return SessionDescription 生成的本地会话描述对象，包含所有相关媒体流和会话配置。
     * @throws Exception 如果在生成会话描述过程中出现错误，抛出该异常。
     */
    private SessionDescription createLocalSessionDescription() throws Exception {
        SessionDescription offer = new SessionDescription();
        offer.setVersion(WebrtcSdpDefault.defaultVersion());
        offer.setOrigin(WebrtcSdpDefault.defaultOrigin("-", CacheModel.getLocalAddress()));
        offer.setSessionName(WebrtcSdpDefault.defaultSessionName(null));
        offer.setTiming(WebrtcSdpDefault.defaultTiming());

        Bundle bundle = WebrtcSdpDefault.defaultBundle();
        offer.setBundle(bundle);
        offer.setExtMapAllowMixed(WebrtcSdpDefault.defaultExtMapAllowMixed(true));
        offer.setMSid(WebrtcSdpDefault.defaultMsid());

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

        if (mediaLineInfo.getMediaInfoType() == MediaInfoType.VIDEO) {
            for (Map.Entry<Integer, RtpPayload> rtpPayloadEntry : videoRtpPayloads.entrySet()) {
                mediaDescription.addRtpPayload(rtpPayloadEntry.getValue());
            }
        }

        if (mediaLineInfo.getMediaInfoType() == MediaInfoType.AUDIO) {
            for (Map.Entry<Integer, RtpPayload> rtpPayloadEntry : audioRtpPayloads.entrySet()) {
                mediaDescription.addRtpPayload(rtpPayloadEntry.getValue());
            }
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


    /**
     * 创建一个用于 WebRTC 发送器处理的媒体行信息。
     * <p>
     * 此方法根据传入的媒体行的读取信息，生成对应的发送信息，并初始化新的媒体行对象。
     * 生成的发送信息包含新的 SSRC 映射和 SSRC 组，用于支持 WebRTC 数据流的发送。
     *
     * @param producerMediaLineInfo 包含发送器读取配置信息的媒体行对象。其读取信息（readInfo）包括 SSRC 组和 SSRC 映射，
     *                              用于基于读取信息生成对应的发送信息。
     * @return MediaLineInfo 如果读取信息包含有效的 SSRC 组，则返回生成好的媒体行对象，
     * 其中包含发送配置信息；如果读取信息的 SSRC 组为空或无效，则返回 null。
     */
    public MediaLineInfo createWebrtcSenderProcessor(MediaLineInfo producerMediaLineInfo) {
        MediaLineInfo.Info readInfo = producerMediaLineInfo.getReadInfo();

        List<SsrcGroup> readSsrcGroups = readInfo.getSsrcGroups();
        Map<Long, SSRC> readSsrcMap = readInfo.getSsrcMap();
        if (readSsrcMap == null || readSsrcMap.isEmpty()) {
            return null;
        }

        Map<Long, SSRC> sendInfoSsrc = new LinkedHashMap<>();
        List<SsrcGroup> sendSsrcGroups = new ArrayList<>();


        Map<Long, Long> temporarySsrcMap = new HashMap<>(readSsrcMap.size());
        for (Map.Entry<Long, SSRC> longSSRCEntry : readSsrcMap.entrySet()) {
            long generateSsrc = SsrcGenerator.generateSsrc();
            SSRC readSSrc = longSSRCEntry.getValue();
            SSRC ssrc = new SSRC();
            ssrc.setSsrc(generateSsrc);
            ssrc.setCname(readSSrc.getCname());
            ssrc.setStreamId(readSSrc.getStreamId());
            temporarySsrcMap.put(longSSRCEntry.getKey(), generateSsrc);
            sendInfoSsrc.put(ssrc.getSsrc(), ssrc);
        }



        for (SsrcGroup readSsrcGroup : readSsrcGroups) {
            SsrcGroup ssrcGroup = new SsrcGroup();
            ssrcGroup.setSsrcGroupType(readSsrcGroup.getSsrcGroupType());
            for (Long readSsrc : readSsrcGroup.getSsrcList()) {
                Long sendSsrc = temporarySsrcMap.get(readSsrc);
                if (sendSsrc != null) {
                    ssrcGroup.addSsrc(sendSsrc);
                }

            }
            sendSsrcGroups.add(ssrcGroup);
        }


        MediaLineInfo.Info info = new MediaLineInfo.Info();
        info.setSsrcMap(sendInfoSsrc);
        info.setSsrcGroups(sendSsrcGroups);
        info.setRtpPayloads(readInfo.getRtpPayloads());
        MediaLineInfo mediaLineInfo = new MediaLineInfo(producerMediaLineInfo.getMediaInfoType(), getMid(), true, false);
        mediaLineInfo.setSendInfo(info);

        addWebrtcSenderProcessor(mediaLineInfo);
        return mediaLineInfo;
    }


    public boolean removeWebrtcSenderProcessor(MediaLineInfo mediaLineInfo) {
        boolean isRemove = false;

        if (mediaLineInfo.getSendInfo() != null) {
            MediaLineInfo.Info sendInfo = mediaLineInfo.getSendInfo();
            for (Long ssrc : sendInfo.getSsrcMap().keySet()) {
                for (MediaLineInfo value : mediaLineInfoMap.values()) {
                    if (value.getSendInfo() != null) {
                        if (value.getSendInfo().getSsrcMap().containsKey(ssrc)) {
                            value.closeSender();
                            isRemove = true;
                        }
                    }
                    if (value.getReadInfo() != null) {
                        if (value.getReadInfo().getSsrcMap().containsKey(ssrc)) {
                            value.closeSender();
                            isRemove = true;
                        }
                    }


                }
            }

        }
        return isRemove;
    }


    private String getMid() {
        String mid;
        do {
            mid = String.valueOf(processorMid.getAndIncrement());
        } while (mediaLineInfoMap.containsKey(mid));

        return mid;
    }

    private void addWebrtcSenderProcessor(MediaLineInfo mediaLineInfo) {
        if (!mediaLineInfoMap.containsKey(mediaLineInfo.getMid())) {
            log.debug("添加一个sdp行");
            mediaLineInfoMap.put(mediaLineInfo.getMid(), mediaLineInfo);
        }
    }

    private void addWebrtcReadProcessor(MediaDescription mediaDescription) {
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
        if (ssrcMap.isEmpty()) {
            return;
        }
        MediaLineInfo.Info readInfo = new MediaLineInfo.Info();
        readInfo.setSsrcMap(ssrcMap);
        readInfo.setRtpPayloads(mediaDescription.getRtpPayloads());
        readInfo.setSsrcGroups(mediaDescription.getSsrcGroups());
        mediaLineInfo.setReadInfo(readInfo);
        if (isAdd) {
            webrtcProcessorEvent.onAddWebrtcProducer(mediaLineInfo);
        }
    }

    public void setRemoteDescription(RTCSessionDescriptionInit rtcSessionDescriptionInit) {
        SessionDescription parse = SdpParser.parse(rtcSessionDescriptionInit.sdp());
        setRemoteDescription(parse);
        if (rtcSessionDescriptionInit.type() == RTCSdpType.OFFER) {
            try {
                SessionDescription answer = createLocalSessionDescription();
                setLocalDescription(answer);
                webrtcProcessorEvent.onAnswer(new RTCSessionDescriptionInit(RTCSdpType.ANSWER, sessionDescriptionToWebrtcStr(answer)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if (rtcSessionDescriptionInit.type() == RTCSdpType.ANSWER) {
            //暂时不需要做任何处理
            log.debug("接收到answer:{}", rtcSessionDescriptionInit.sdp());
        }

    }


    public void setRemoteDescription(SessionDescription remoteSessionDescription) {
        this.remoteSessionDescription = remoteSessionDescription;
        this.localIceInfo.setRemoteIceInfo(remoteSessionDescription.getMediaDescriptions().getFirst().getIceInfo());
        log.debug("远程的ice信息ufrag：{};pwd:{}", localIceInfo.getRemoteIceInfo().getUfrag(), localIceInfo.getRemoteIceInfo().getPwd());
        List<MediaDescription> mediaDescriptionList = remoteSessionDescription.getMediaDescriptions();
        for (MediaDescription mediaDescription : mediaDescriptionList) {
            addWebrtcReadProcessor(mediaDescription);
        }
    }

    /**
     * 设置本地会话描述。
     * 此方法会处理传入的本地会话描述并更新相应的媒体流信息。
     *
     * @param localSessionDescription 传入的本地会话描述对象。包含了媒体描述信息，
     *                                用于更新媒体行的发送信息（如SSRC映射、RTP负载类型和SSRC组）。
     */
    public void setLocalDescription(SessionDescription localSessionDescription) {
        this.localSessionDescription = localSessionDescription;
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
        }
    }

    public RTCSessionDescriptionInit createOffer() throws Exception {
        SessionDescription localSessionDescription = createLocalSessionDescription();
        setLocalDescription(localSessionDescription);
        return new RTCSessionDescriptionInit(RTCSdpType.OFFER, sessionDescriptionToWebrtcStr(this.localSessionDescription));
    }

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
                msid = String.format("stream%s track-%s", mediaVideoDescription.getMId().id(), mediaVideoDescription.getMId().id());
                sb.append("a=msid:").append(msid).append("\r\n");
            }
            if (mediaVideoDescription.getRtcpMux() != null && mediaVideoDescription.getRtcpMux().enabled()) {
                sb.append("a=rtcp-mux").append("\r\n");
            }
            sb.append(CacheModel.getLocalCandidate().toSdpStr()).append("\r\n");

            StringBuilder mediaSb = new StringBuilder();
            for (RtpPayload rtpPayload : rtpPayloads.values()) {
                mediaSb.append("a=rtpmap:")
                        .append(rtpPayload.getPayloadType())
                        .append(" ")
                        .append(rtpPayload.getEncodingName())
                        .append("/")
                        .append(rtpPayload.getClockRate());

                if (rtpPayload.getChannels() != null) {
                    mediaSb.append("/").append(rtpPayload.getChannels());
                }

                mediaSb.append("\r\n");

//                mediaSb.append(String.format("a=rtpmap:%d %s/%d", rtpPayload.getPayloadType(), rtpPayload.getEncodingName(), rtpPayload.getClockRate())).append("\r\n");
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
                    }
                }

                if (rtpPayload.getFmtp() != null) {
                    FmtpAttributes fmtp = rtpPayload.getFmtp();
                    if (fmtp.getAssociatedPayloadType() != null) {
                        if (fmtp.isCompatibleSwitch()) {
                            mediaSb.append(String.format("a=fmtp:%d %d/%d", rtpPayload.getPayloadType(), fmtp.getAssociatedPayloadType(), fmtp.getAssociatedPayloadType())).append("\r\n");
                        } else {
                            mediaSb.append(String.format("a=fmtp:%d apt=%d", rtpPayload.getPayloadType(), fmtp.getAssociatedPayloadType())).append("\r\n");
                        }
                    }
                    if (!fmtp.getParams().isEmpty()) {
                        mediaSb.append("a=fmtp:").append(rtpPayload.getPayloadType()).append(" ");
                        for (Map.Entry<String, String> stringStringEntry : fmtp.getParams().entrySet()) {
                            mediaSb.append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue()).append(";");
                        }

                        mediaSb.append("\r\n");
                    }
                }


            }

            sb.append(mediaSb);


            List<SsrcGroup> ssrcGroups = mediaVideoDescription.getSsrcGroups();
            for (SsrcGroup ssrcGroup : ssrcGroups) {
                sb.append("a=ssrc-group:").append(ssrcGroup.getSsrcGroupType().value);
                for (Long l : ssrcGroup.getSsrcList()) {
                    sb.append(" ").append(l);
                }
                sb.append("\r\n");
            }
            for (SSRC ssrc : mediaVideoDescription.getSsrcMap().values()) {
                sb.append(String.format("a=ssrc:%d cname:%s", ssrc.getSsrc(), ssrc.getCname())).append("\r\n");
                if (ssrc.getStreamId() != null) {
                    sb.append(String.format("a=ssrc:%d msid:%s", ssrc.getSsrc(), msid)).append("\r\n");
                }
            }
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


        void onAddWebrtcProducer(MediaLineInfo mediaInfo);

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
