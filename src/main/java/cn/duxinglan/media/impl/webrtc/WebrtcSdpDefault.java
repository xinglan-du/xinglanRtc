package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.media.*;
import cn.duxinglan.sdp.entity.rtp.*;
import cn.duxinglan.sdp.entity.session.*;
import cn.duxinglan.sdp.entity.type.*;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
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
