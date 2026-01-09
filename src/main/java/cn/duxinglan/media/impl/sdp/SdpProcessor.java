package cn.duxinglan.media.impl.sdp;

import cn.duxinglan.media.impl.webrtc.WebRTCCertificateGenerator;
import cn.duxinglan.media.module.CacheModel;
import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.media.signaling.sdp.SdpParse;
import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.media.signaling.sdp.codec.CodecFactory;
import cn.duxinglan.media.signaling.sdp.codec.VideoCodec;
import cn.duxinglan.media.signaling.sdp.media.*;
import cn.duxinglan.media.signaling.sdp.session.*;
import cn.duxinglan.media.signaling.sdp.ssrc.SSRC;
import cn.duxinglan.media.signaling.sdp.type.CodecType;
import cn.duxinglan.media.signaling.sdp.type.MediaDirectionType;
import cn.duxinglan.media.signaling.sdp.type.MediaInfoType;
import cn.duxinglan.media.signaling.sdp.type.SetupType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

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
public class SdpProcessor {

    /**
     * 表示一个用于处理 SDP 会话相关操作的回调接口实例。
     * 该回调接口允许提供 DTLS 密钥材料、媒体描述信息以及与 ICE 相关的操作。
     * 在构造 {@link SdpProcessor} 对象时必须提供该回调实例，用于完成会话描述的生成、解析和更新等工作。
     */
    private final SdpProcessorCallback sdpProcessorCallback;

    public SdpProcessor(SdpProcessorCallback sdpProcessorCallback) {
        this.sdpProcessorCallback = sdpProcessorCallback;
    }


    //创建offer
    public SessionDescription createOffer() throws Exception {
        return getSessionDescription();
    }

    //创建应答
    public SessionDescription createAnswer() throws SdpException {
        return getSessionDescription();
    }

    private SessionDescription getSessionDescription() throws SdpException {
        SessionDescription offer = new SessionDescription();
        offer.setVersion(Version.defaultVersion());
        offer.setOrigin(Origin.defaultOrigin("-", CacheModel.getLocalAddress()));
        offer.setSessionName(SessionName.defaultSessionName(null));
        offer.setTiming(Timing.defaultTiming());

        Bundle bundle = Bundle.defaultBundle();
        offer.setBundle(bundle);
        offer.setExtMapAllowMixed(ExtMapAllowMixed.defaultExtMapAllowMixed(true));
        offer.setMSid(MSid.defaultMsid());

        List<MediaDescription> offerMediaDescription = getOfferMediaDescription();
        for (MediaDescription mediaDescription : offerMediaDescription) {
            bundle.addMid(mediaDescription.getMId().getId());
            offer.addMediaDescription(mediaDescription);
        }
        return offer;
    }

    private List<MediaDescription> getOfferMediaDescription() throws SdpException {
        List<MediaDescription> mediaDescriptions = new ArrayList<>();
        Collection<MediaDescriptionSpec> mediaDescriptionSpecs = sdpProcessorCallback.getMediaDescriptions();

        for (MediaDescriptionSpec mediaDescriptionSpec : mediaDescriptionSpecs) {
            MediaDescription mediaDescription = generatedMediaDescription(mediaDescriptionSpec);
            mediaDescriptions.add(mediaDescription);
        }
        return mediaDescriptions;
    }

    private MediaDescription generatedMediaDescription(MediaDescriptionSpec mediaDescriptionSpec) throws SdpException {
        MediaDescription mediaDescription = new MediaDescription();
        mediaDescription.setInfo(Info.defaultInfo(mediaDescriptionSpec.getMediaInfoType()));
        mediaDescription.setConnection(Connection.defaultConnection());
        mediaDescription.setRtcpConnection(RtcpConnection.defaultRtcpConnection());
        mediaDescription.setIceInfo(sdpProcessorCallback.getOfficeInfo());
        try {
            mediaDescription.setFingerprint(Fingerprint.defaultFingerprint(this.sdpProcessorCallback.getKeyMaterial().getFingerprint()));
        } catch (Exception e) {
            throw new SdpException("dtls证书获取失败", e);
        }
        //dtls服务器
        mediaDescription.setSetup(new Setup(SetupType.PASSIVE));
        mediaDescription.setMId(new MId(mediaDescriptionSpec.getMid()));
        if (mediaDescriptionSpec.isSendOnly()) {
            mediaDescription.setMediaDirection(new MediaDirection(MediaDirectionType.SENDONLY));
        } else if (mediaDescriptionSpec.isReadOnly()) {
            mediaDescription.setMediaDirection(new MediaDirection(MediaDirectionType.RECVONLY));
        } else {
            mediaDescription.setMediaDirection(new MediaDirection(MediaDirectionType.INACTIVE));
        }
        mediaDescription.setRtcpMux(new RtcpMux(true));
        List<CodecType> codecTypes = sdpProcessorCallback.getCodecTypes();
        for (CodecType codecType : codecTypes) {
            VideoCodec videoCodec = CodecFactory.createVideoCodec(codecType);
            mediaDescription.addCodec(videoCodec);
        }

        if (mediaDescriptionSpec.isSendOnly()) {
            Optional<MediaDescriptionSpec.SSRCDescribe> senderOpt = mediaDescriptionSpec.getSender();
            senderOpt.ifPresent(ssrcDescribe -> {
                mediaDescription.setMsid(new MSId(ssrcDescribe.getStreamId()));
                mediaDescription.addSSRC(new SSRC(ssrcDescribe.getPrimaryMediaStream(), ssrcDescribe.getRtxMediaStream(), ssrcDescribe.getCname()));
            });
        }


        return mediaDescription;
    }


    public String sessionDescriptionToStr(SessionDescription offer) {
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
            sb.append(String.format("m=%s %d %s %s", info.type().value, info.port(), info.transportType().value, mediaVideoDescription.getPayloadsToString())).append("\r\n");

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
            sb.append(String.format("a=mid:%s", mediaVideoDescription.getMId().getId())).append("\r\n");
            sb.append(String.format("a=%s", mediaVideoDescription.getMediaDirection().type().value)).append("\r\n");
            String msid = null;
            if (mediaVideoDescription.getMsid() != null) {
                msid = String.format("stream-%s track-%s", mediaVideoDescription.getMsid().streamId(), mediaVideoDescription.getMId());
                sb.append("a=msid:").append(msid).append("\r\n");
            }
            if (mediaVideoDescription.getRtcpMux().enabled()) {
                sb.append("a=rtcp-mux").append("\r\n");
            }
            sb.append(CacheModel.getLocalCandidate().toSdpStr()).append("\r\n");

            List<VideoCodec> codecs = mediaVideoDescription.getCodecs();
            StringBuilder mediaSb = new StringBuilder();
            for (VideoCodec codec : codecs) {
                mediaSb.append(String.format("a=rtpmap:%d %s/%d", codec.getPayloadType(), codec.getEncodingFormat(), codec.getClockRate())).append("\r\n");
                mediaSb.append(String.format("a=rtcp-fb:%d nack", codec.getPayloadType())).append("\r\n");
                mediaSb.append(String.format("a=rtcp-fb:%d nack pli", codec.getPayloadType())).append("\r\n");
                mediaSb.append(String.format("a=rtcp-fb:%d ccm fir", codec.getPayloadType())).append("\r\n");
                mediaSb.append(String.format("a=rtpmap:%d rtx/%d", codec.getRetransmitPayloadType(), codec.getClockRate())).append("\r\n");
                mediaSb.append(String.format("a=fmtp:%d apt=%d", codec.getRetransmitPayloadType(), codec.getPayloadType())).append("\r\n");


            }
            sb.append(mediaSb);
            List<SSRC> ssrcList = mediaVideoDescription.getSsrcList();
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


            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "";
    }

    public SessionDescription strToSessionDescription(String str) {

        String[] sdpLines = str.replaceAll("\r\n", "\n").split("m=");
        Map<String, List<String>> stringListMap = SdpParse.sdpStrToSdpMap(sdpLines[0]);

        SessionDescription sessionDescription = new SessionDescription();
        sessionDescription.setVersion(Version.parseLine(stringListMap.get(Version.KEY).getFirst()));
        sessionDescription.setOrigin(Origin.parseLine(stringListMap.get(Origin.KEY).getFirst()));
        sessionDescription.setSessionName(SessionName.parseLine(stringListMap.get(SessionName.KEY).getFirst()));
        sessionDescription.setTiming(Timing.parseLine(stringListMap.get(Timing.KEY).getFirst()));
        Map<String, List<String>> aExtMap = SdpParse.aExtMapToMap(stringListMap.get(SdpParse.EXPAND_KEY), SdpParse.EXPAND_DELIMITER);

        if (aExtMap.containsKey(Bundle.KEY)) {
            sessionDescription.setBundle(Bundle.parseLine(aExtMap.get(Bundle.KEY).getFirst()));
        }
        sessionDescription.setExtMapAllowMixed(ExtMapAllowMixed.parseLine(aExtMap.containsKey(ExtMapAllowMixed.KEY)));
        sessionDescription.setMSid(MSid.parseLine(aExtMap.get(MSid.KEY).getFirst()));

        //处理相关媒体数据
        for (int i = 1; i < sdpLines.length; i++) {
            String sdpLine = sdpLines[i];
            stringListMap = SdpParse.sdpStrToSdpMap("m=" + sdpLine);
            MediaDescription mediaDescription = new MediaDescription();
            mediaDescription.setInfo(Info.parseLine(stringListMap.get(Info.KEY).getFirst()));
            mediaDescription.setConnection(Connection.parseLine(stringListMap.get(Connection.KEY).getFirst()));
            aExtMap = SdpParse.aExtMapToMap(stringListMap.get(SdpParse.EXPAND_KEY), SdpParse.EXPAND_DELIMITER);
            mediaDescription.setRtcpConnection(RtcpConnection.parseLine(aExtMap.get(RtcpConnection.KEY).getFirst()));
            IceInfo iceInfo = new IceInfo();
            iceInfo.setUfrag(aExtMap.get(IceInfo.UFRAG_KEY).getFirst());
            iceInfo.setPwd(aExtMap.get(IceInfo.PWS).getFirst());
            iceInfo.setOptions(aExtMap.get(IceInfo.OPTIONS).getFirst());

            mediaDescription.setIceInfo(iceInfo);
            mediaDescription.setMId(new MId(aExtMap.get(SdpParse.MID_KEY).getFirst()));
            resolveMediaDirection(aExtMap).ifPresent(mediaDescription::setMediaDirection);
            if (aExtMap.containsKey(SdpParse.RTCP_MUX)) {
                mediaDescription.setRtcpMux(new RtcpMux(true));
            }
            if (mediaDescription.getInfo().type() == MediaInfoType.VIDEO) {
                List<VideoCodec> videoCodecs = resolveVideoCodec(sdpLine);
                mediaDescription.addCodecs(videoCodecs);
            } else if (mediaDescription.getInfo().type() == MediaInfoType.AUDIO) {
//                VideoCodec codec = resolveVideoCodec(sdpLine);
//                mediaDescription.addCodec(codec);
            }
            SSRC ssrc = SSRC.parseFromSdp(sdpLine);
            if (ssrc != null) {
                mediaDescription.addSSRC(ssrc);
            }

            sessionDescription.addMediaDescription(mediaDescription);
        }


        return sessionDescription;
    }

    public static Optional<MediaDirection> resolveMediaDirection(Map<String, List<String>> extMap) {
        for (MediaDirectionType type : MediaDirectionType.values()) {
            if (extMap.containsKey(type.value)) {
                return Optional.of(new MediaDirection(type));
            }
        }
        return Optional.empty();
    }


    private List<VideoCodec> resolveVideoCodec(String sdpLine) {
        List<VideoCodec> videoCodecs = new ArrayList<>();
        //先将a=rtpmap:前的内容过滤掉
        String[] split = sdpLine.substring(sdpLine.indexOf("a=rtpmap:")).split("\n");
        VideoCodec videoCodec = null;
        for (String string : split) {
            if (string.contains("a=rtpmap:")) {
                if (videoCodec != null) {
                    if (string.startsWith("a=rtpmap:" + videoCodec.getRetransmitPayloadType())) {
                        continue;
                    }
                    videoCodecs.add(videoCodec);
                }
                videoCodec = CodecFactory.lineToVideoCodec(string);
            }
        }
        if (videoCodec != null) {
            videoCodecs.add(videoCodec);
        }
        return videoCodecs;
    }

/*    public void updateOffer() {
        this.offer = null;
    }*/


    /**
     * 提供 SDP 处理相关的回调接口，用于获取关键的媒体和加密信息。
     */
    public interface SdpProcessorCallback {

        /**
         * 获取 DTLS 密钥材料容器。
         *
         * @return 包含密钥对、公钥证书和加密上下文的 DTLS 密钥材料对象。
         */
        WebRTCCertificateGenerator.DTLSKeyMaterial getKeyMaterial();

        /**
         * 获取支持的编解码类型列表。
         *
         * @return 包含支持的编解码类型的列表。
         */
        List<CodecType> getCodecTypes();


        Collection<MediaDescriptionSpec> getMediaDescriptions();

        /**
         * 获取与办公室相关的 ICE 信息。
         *
         * @return 包含与办公室相关的 ICE 信息的 IceInfo 对象。
         */
        IceInfo getOfficeInfo();
    }
}
