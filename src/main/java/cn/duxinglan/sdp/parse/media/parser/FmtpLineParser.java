package cn.duxinglan.sdp.parse.media.parser;

import cn.duxinglan.sdp.entity.MediaDescription;
import cn.duxinglan.sdp.entity.rtp.FmtpAttributes;
import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.parse.media.MediaLineParser;
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
public class FmtpLineParser extends MediaLineParser {

    public static final String KEY = "fmtp";


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
        FmtpAttributes fmtp = new FmtpAttributes();

        if (s[1].contains("apt")) {
            String[] apt = s[1].split("=");
            fmtp.setAssociatedPayloadType(Integer.parseInt(apt[1]));
        } else if (s[1].contains("/")) {
            String[] apt = s[1].split("/");
            fmtp.setAssociatedPayloadType(Integer.parseInt(apt[1]));
        } else {
            String[] keyValue = s[1].split(";");
            for (String string : keyValue) {
                String[] keyValuePair = string.split("=");
                fmtp.putParam(keyValuePair[0], keyValuePair[1]);
            }
        }


        rtpPayload.setFmtp(fmtp);

        return true;
    }
}
