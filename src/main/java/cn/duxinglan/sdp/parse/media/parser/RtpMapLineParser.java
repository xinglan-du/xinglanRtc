package cn.duxinglan.sdp.parse.media.parser;

import cn.duxinglan.sdp.entity.MediaDescription;
import cn.duxinglan.sdp.entity.rtp.MediaType;
import cn.duxinglan.sdp.entity.rtp.RtpPayload;
import cn.duxinglan.sdp.entity.type.MediaInfoType;
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
public class RtpMapLineParser extends MediaLineParser {

    public static final String KEY = "rtpmap";


    @Override
    public String[] getLineStartWith() {
        return new String[]{KEY};
    }

    @Override
    protected boolean parse(MediaDescription mediaDescription, String key, String value) {
        MediaInfoType type = mediaDescription.getInfo().type();
        RtpPayload rtpPayload = new RtpPayload();
        if (mediaDescription.getInfo().type() == MediaInfoType.VIDEO) {
            rtpPayload.setMediaType(MediaType.VIDEO);
        } else if (type == MediaInfoType.AUDIO) {
            rtpPayload.setMediaType(MediaType.AUDIO);
        } else {
            log.warn("不支持的媒体类型{}", type);
            return false;
        }
        String[] s = value.split(" ");
        rtpPayload.setPayloadType(Integer.parseInt(s[0]));
        String[] split = s[1].split("/");
        rtpPayload.setEncodingName(split[0]);
        rtpPayload.setClockRate(Integer.parseInt(split[1]));
        if (split.length == 3) {
            rtpPayload.setChannels(Integer.parseInt(split[2]));
        }
        mediaDescription.addRtpPayload(rtpPayload);

        return true;
    }
}
