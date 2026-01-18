package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.media.*;
import cn.duxinglan.sdp.entity.rtp.*;
import cn.duxinglan.sdp.entity.session.*;
import cn.duxinglan.sdp.entity.type.*;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
public class WebrtcSdpDefault {

    public static Info defaultInfo(MediaInfoType mediaInfoType) {
        return new Info(mediaInfoType, 9, MediaTransportType.UDP_TLS_RTP_SAVPF);
    }

    public static Connection defaultConnection() {
        return new Connection(NetworkType.IN, IpVerType.IPV4, "0.0.0.0");
    }

    public static RtcpConnection defaultRtcpConnection() {
        return new RtcpConnection(9, NetworkType.IN, IpVerType.IPV4, "0.0.0.0");
    }


    public static Fingerprint defaultFingerprint(String finger) {
        return new Fingerprint(SecurityType.SHA256, finger);
    }

    public static Setup defaultSetup() {
        return new Setup(SetupType.PASSIVE);
    }

    public static MId defaultMid(String mid) {
        return new MId(mid);
    }

    public static MediaDirection defaultMediaDirection(MediaLineInfo value) {
        if (value.isSendOnly()) {
            return new MediaDirection(MediaDirectionType.SENDONLY);
        } else if (value.isReadOnly()) {
            return new MediaDirection(MediaDirectionType.RECVONLY);
        } else {
            return new MediaDirection(MediaDirectionType.INACTIVE);
        }
    }

    public static RtcpMux defaultRtcpMux() {
        return new RtcpMux(true);
    }


    public static MSId defaultMsid(String streamId) {
        return new MSId(streamId, null);
    }

    public static MediaLineInfo createNullVideoMediaLineInfo(String mid) {
        return new MediaLineInfo(MediaInfoType.VIDEO, mid, true, false);
    }

    public static MediaLineInfo createNullAudioMediaLineInfo(String mid) {
        return new MediaLineInfo(MediaInfoType.AUDIO, mid, true, false);
    }

    public static Version defaultVersion() {
        return new Version(0);
    }

    public static Origin defaultOrigin(String username, InetAddress inetAddress) {
        if (StringUtils.isEmpty(username)) {
            username = "-";
        }
        AddressType addressType;
        if (inetAddress instanceof Inet4Address) {
            addressType = AddressType.IP4;
        } else {
            addressType = AddressType.IP6;
        }

        return new Origin(username, generateSessionId(), 0, NetworkType.IN, addressType, inetAddress.getHostAddress()); // 默认值为 1
    }


    public static long generateSessionId() {
        return ThreadLocalRandom.current().nextLong();
    }

    public static SessionName defaultSessionName(String value) {
        if (StringUtils.isEmpty(value)) {
            value = "-";
        }
        return new SessionName(value);
    }

    public static Timing defaultTiming() {
        return new Timing(0, 0);
    }

    public static Bundle defaultBundle() {
        return new Bundle();
    }

    public static MSid defaultMsid() {
        return new MSid(MediaStreamType.WMS, null, null);
    }

    public static ExtMapAllowMixed defaultExtMapAllowMixed(Boolean value) {
        if (value == null) {
            value = true;
        }
        return new ExtMapAllowMixed(value);
    }


    public static void defaultVideoRtpPayload(LinkedHashMap<Integer, RtpPayload> videoRtpPayloads) {
        //AV1
        RtpPayload av1RtpPayload = defaultAV1RtpPayload();
        RtpPayload av1RtxRtpPayload = defaultAV1RtxRtpPayload();
        videoRtpPayloads.put(av1RtpPayload.getPayloadType(), av1RtpPayload);
        videoRtpPayloads.put(av1RtxRtpPayload.getPayloadType(), av1RtxRtpPayload);

        //VP9 1
        RtpPayload vp998RtpPayload = defaultVp9Pt98RtpPayload();
        RtpPayload vp999RtxRtpPayload = defaultVp9Pt99RtxRtpPayload();
        videoRtpPayloads.put(vp998RtpPayload.getPayloadType(), vp998RtpPayload);
        videoRtpPayloads.put(vp999RtxRtpPayload.getPayloadType(), vp999RtxRtpPayload);
        //VP9 2
        RtpPayload vp9100RtpPayload = defaultVp9Pt100RtpPayload();
        RtpPayload vp9101RtxRtpPayload = defaultVp9Pt101RtxRtpPayload();
        videoRtpPayloads.put(vp9100RtpPayload.getPayloadType(), vp9100RtpPayload);
        videoRtpPayloads.put(vp9101RtxRtpPayload.getPayloadType(), vp9101RtxRtpPayload);

        //VP8
        RtpPayload vp8RtpPayload = defaultVp8RtpPayload();
        RtpPayload vp8RtxRtpPayload = defaultVp8RtxRtpPayload();
        videoRtpPayloads.put(vp8RtpPayload.getPayloadType(), vp8RtpPayload);
        videoRtpPayloads.put(vp8RtxRtpPayload.getPayloadType(), vp8RtxRtpPayload);

        //H264 1
        RtpPayload h264Pt103RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt103RtpPayload();
        RtpPayload h264Pt104RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt104RtxRtpPayload();
        RtpPayload h264Pt107RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt107RtpPayload();
        RtpPayload h264Pt108RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt108RtxRtpPayload();
        RtpPayload h264Pt109RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt109RtpPayload();
        RtpPayload h264Pt114RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt114RtxRtpPayload();
        RtpPayload h264Pt115RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt115RtpPayload();
        RtpPayload h264Pt116RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt116RtxRtpPayload();
        RtpPayload h264Pt117RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt117RtpPayload();
        RtpPayload h264Pt118RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt118RtxRtpPayload();
        RtpPayload h264Pt39RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt39RtpPayload();
        RtpPayload h264Pt40RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt40RtxRtpPayload();
        RtpPayload h264Pt119RtpPayload = H264RtpPlayLoadFactory.defaultH264Pt119RtpPayload();
        RtpPayload h264Pt120RtxRtpPayload = H264RtpPlayLoadFactory.defaultH264Pt120RtxRtpPayload();

        videoRtpPayloads.put(h264Pt103RtpPayload.getPayloadType(), h264Pt103RtpPayload);
        videoRtpPayloads.put(h264Pt104RtxRtpPayload.getPayloadType(),h264Pt104RtxRtpPayload);
        videoRtpPayloads.put(h264Pt107RtpPayload.getPayloadType(),h264Pt107RtpPayload);
        videoRtpPayloads.put(h264Pt108RtxRtpPayload.getPayloadType(),h264Pt108RtxRtpPayload);
        videoRtpPayloads.put(h264Pt109RtpPayload.getPayloadType(),h264Pt109RtpPayload);
        videoRtpPayloads.put(h264Pt114RtxRtpPayload.getPayloadType(),h264Pt114RtxRtpPayload);
        videoRtpPayloads.put(h264Pt115RtpPayload.getPayloadType(),h264Pt115RtpPayload);
        videoRtpPayloads.put(h264Pt116RtxRtpPayload.getPayloadType(),h264Pt116RtxRtpPayload);
        videoRtpPayloads.put(h264Pt117RtpPayload.getPayloadType(),h264Pt117RtpPayload);
        videoRtpPayloads.put(h264Pt118RtxRtpPayload.getPayloadType(),h264Pt118RtxRtpPayload);
        videoRtpPayloads.put(h264Pt39RtpPayload.getPayloadType(),h264Pt39RtpPayload);
        videoRtpPayloads.put(h264Pt40RtxRtpPayload.getPayloadType(),h264Pt40RtxRtpPayload);
        videoRtpPayloads.put(h264Pt119RtpPayload.getPayloadType(),h264Pt119RtpPayload);
        videoRtpPayloads.put(h264Pt120RtxRtpPayload.getPayloadType(),h264Pt120RtxRtpPayload);

    }

    public static RtpPayload defaultAV1RtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(45);
        rtpPayload.setEncodingName("AV1");
        rtpPayload.setClockRate(90000);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.CCM, RtcpFeedbackParam.PLI));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK, RtcpFeedbackParam.PLI));

        FmtpAttributes fmtpAttributes = new FmtpAttributes();
        fmtpAttributes.putParam("level-idx", "5");
        fmtpAttributes.putParam("profile", "0");
        fmtpAttributes.putParam("tier", "0");

        rtpPayload.setFmtp(fmtpAttributes);

        return rtpPayload;
    }

    public static RtpPayload defaultAV1RtxRtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(46);
        rtpPayload.setEncodingName("rtx");
        rtpPayload.setClockRate(90000);
        rtpPayload.setFmtp(new FmtpAttributes(45));
        return rtpPayload;
    }


    public static RtpPayload defaultVp9Pt98RtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(98);
        rtpPayload.setEncodingName("VP9");
        rtpPayload.setClockRate(90000);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.CCM, RtcpFeedbackParam.PLI));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK, RtcpFeedbackParam.PLI));

        FmtpAttributes fmtpAttributes = new FmtpAttributes();
        fmtpAttributes.putParam("profile-id", "0");

        rtpPayload.setFmtp(fmtpAttributes);

        return rtpPayload;
    }

    public static RtpPayload defaultVp9Pt99RtxRtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(99);
        rtpPayload.setEncodingName("rtx");
        rtpPayload.setClockRate(90000);
        rtpPayload.setFmtp(new FmtpAttributes(98));
        return rtpPayload;
    }


    public static RtpPayload defaultVp9Pt100RtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(100);
        rtpPayload.setEncodingName("VP9");
        rtpPayload.setClockRate(90000);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.CCM, RtcpFeedbackParam.PLI));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK, RtcpFeedbackParam.PLI));

        FmtpAttributes fmtpAttributes = new FmtpAttributes();
        fmtpAttributes.putParam("profile-id", "2");

        rtpPayload.setFmtp(fmtpAttributes);

        return rtpPayload;
    }

    public static RtpPayload defaultVp9Pt101RtxRtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(99);
        rtpPayload.setEncodingName("rtx");
        rtpPayload.setClockRate(90000);
        rtpPayload.setFmtp(new FmtpAttributes(100));
        return rtpPayload;
    }


    public static RtpPayload defaultVp8RtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(96);
        rtpPayload.setEncodingName("VP8");
        rtpPayload.setClockRate(90000);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.CCM, RtcpFeedbackParam.PLI));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK, RtcpFeedbackParam.PLI));
        return rtpPayload;
    }

    public static RtpPayload defaultVp8RtxRtpPayload() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(97);
        rtpPayload.setEncodingName("rtx");
        rtpPayload.setClockRate(90000);
        rtpPayload.setFmtp(new FmtpAttributes(96));
        return rtpPayload;
    }


    public static void defaultAudioRtpPayload(LinkedHashMap<Integer, RtpPayload> audioRtpPayloads) {
        RtpPayload opusRtpPayload = defaultOpus();
        RtpPayload opusRedRtpPayload = defaultOpusRed();
        audioRtpPayloads.put(opusRtpPayload.getPayloadType(), opusRtpPayload);
        audioRtpPayloads.put(opusRedRtpPayload.getPayloadType(), opusRedRtpPayload);

    }

    public static RtpPayload defaultOpus() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(111);
        rtpPayload.setEncodingName("opus");
        rtpPayload.setClockRate(48000);
        rtpPayload.setChannels(2);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.TRANSPORT_CC));
        FmtpAttributes fmtp = new FmtpAttributes();
        fmtp.putParam("minptime", "10");
        fmtp.putParam("useinbandfec", "1");
        rtpPayload.setFmtp(fmtp);
        return rtpPayload;
    }

    public static RtpPayload defaultOpusRed() {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(63);
        rtpPayload.setEncodingName("red");
        rtpPayload.setClockRate(48000);
        rtpPayload.setChannels(2);

        FmtpAttributes fmtp = new FmtpAttributes();
        fmtp.setAssociatedPayloadType(111);
        fmtp.setCompatibleSwitch(true);
        rtpPayload.setFmtp(fmtp);
        return rtpPayload;
    }
}
