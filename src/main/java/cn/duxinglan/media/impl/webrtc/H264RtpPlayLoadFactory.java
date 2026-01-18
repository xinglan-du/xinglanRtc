package cn.duxinglan.media.impl.webrtc;

import cn.duxinglan.sdp.entity.rtp.*;

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
public class H264RtpPlayLoadFactory {


    public static RtpPayload defaultH264Pt103RtpPayload() {
        return defaultH264RtpPayload(103, "1", "1", "42001f");
    }

    public static RtpPayload defaultH264Pt104RtxRtpPayload() {
        return defaultH264RtxRtpPayload(104, 103);
    }

    public static RtpPayload defaultH264Pt107RtpPayload() {
        return defaultH264RtpPayload(107, "1", "0", "42001f");
    }

    public static RtpPayload defaultH264Pt108RtxRtpPayload() {
        return defaultH264RtxRtpPayload(108, 107);
    }

    public static RtpPayload defaultH264Pt109RtpPayload() {
        return defaultH264RtpPayload(109, "1", "1", "42e01f");
    }

    public static RtpPayload defaultH264Pt114RtxRtpPayload() {
        return defaultH264RtxRtpPayload(114, 109);
    }

    public static RtpPayload defaultH264Pt115RtpPayload() {
        return defaultH264RtpPayload(115, "1", "0", "42e01f");
    }

    public static RtpPayload defaultH264Pt116RtxRtpPayload() {
        return defaultH264RtxRtpPayload(116, 115);
    }


    public static RtpPayload defaultH264Pt117RtpPayload() {
        return defaultH264RtpPayload(117, "1", "1", "4d001f");
    }

    public static RtpPayload defaultH264Pt118RtxRtpPayload() {
        return defaultH264RtxRtpPayload(118, 117);
    }

    public static RtpPayload defaultH264Pt39RtpPayload() {
        return defaultH264RtpPayload(39, "1", "0", "4d001f");
    }

    public static RtpPayload defaultH264Pt40RtxRtpPayload() {
        return defaultH264RtxRtpPayload(40, 39);
    }

    public static RtpPayload defaultH264Pt119RtpPayload() {
        return defaultH264RtpPayload(119, "1", "1", "64001f");
    }

    public static RtpPayload defaultH264Pt120RtxRtpPayload() {
        return defaultH264RtxRtpPayload(120, 119);
    }


    public static RtpPayload defaultH264RtpPayload(
            int payloadType, String levelAsymmetryAllowed, String packetizationMode, String profileLevelId) {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(payloadType);
        rtpPayload.setEncodingName("H264");
        rtpPayload.setClockRate(90000);
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.CCM, RtcpFeedbackParam.PLI));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK));
        rtpPayload.addRtcpFb(new RtcpFeedback(RtcpFeedbackType.NACK, RtcpFeedbackParam.PLI));

        FmtpAttributes fmtpAttributes = new FmtpAttributes();
        if (levelAsymmetryAllowed != null) {
            fmtpAttributes.putParam("level-asymmetry-allowed", levelAsymmetryAllowed);
        }
        if (packetizationMode != null) {
            fmtpAttributes.putParam("packetization-mode", packetizationMode);

        }
        if (profileLevelId != null) {
            fmtpAttributes.putParam("profile-level-id", profileLevelId);
        }

        rtpPayload.setFmtp(fmtpAttributes);
        return rtpPayload;
    }

    public static RtpPayload defaultH264RtxRtpPayload(int payloadType, int associatedPayloadType) {
        RtpPayload rtpPayload = new RtpPayload();
        rtpPayload.setPayloadType(payloadType);
        rtpPayload.setEncodingName("rtx");
        rtpPayload.setClockRate(90000);
        rtpPayload.setFmtp(new FmtpAttributes(associatedPayloadType));
        return rtpPayload;
    }

}
