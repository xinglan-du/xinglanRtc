package cn.duxinglan.sdp.media.parser;

import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.media.signaling.sdp.rtp.RtcpFeedback;
import cn.duxinglan.media.signaling.sdp.rtp.RtcpFeedbackParam;
import cn.duxinglan.media.signaling.sdp.rtp.RtcpFeedbackType;
import cn.duxinglan.media.signaling.sdp.rtp.RtpPayload;
import cn.duxinglan.sdp.media.MediaLineParser;
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
public class RtcpFbLineParser extends MediaLineParser {

    public static final String KEY = "rtcp-fb";


    @Override
    public String[] getLineStartWith() {
        return new String[]{KEY};
    }

    @Override
    protected boolean parse(MediaDescription mediaDescription, String key, String value) {
        String[] s = value.split(" ");
        RtpPayload rtpPayload = mediaDescription.getRtpPayloads().get(Integer.parseInt(s[0]));
        if (rtpPayload == null) {
            log.warn("未获取到媒体编解码器描述");
            return false;
        }
        RtcpFeedback rtcpFeedback = new RtcpFeedback();
        rtcpFeedback.setRtcpFeedbackType(RtcpFeedbackType.fromValue(s[1]));
        if (s.length == 3) {
            rtcpFeedback.setRtcpFeedbackParam(RtcpFeedbackParam.fromValue(s[2]));
        }
        rtpPayload.addRtcpFb(rtcpFeedback);
        return true;
    }
}
