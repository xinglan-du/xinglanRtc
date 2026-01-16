package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.media.*;
import cn.duxinglan.sdp.entity.rtp.*;
import cn.duxinglan.sdp.entity.type.*;

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

    public static MSId defaultMsid(String streamId) {
        return new MSId(streamId, null);
    }

    public static MediaLineInfo createNullVideoMediaLineInfo(String mid) {
        return new MediaLineInfo(MediaInfoType.VIDEO, mid, true, false);

    }
}
