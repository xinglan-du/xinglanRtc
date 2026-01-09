package cn.duxinglan.sdp;

import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.sdp.media.MediaParser;
import cn.duxinglan.sdp.session.SessionParser;
import org.apache.commons.lang3.StringUtils;

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
public class SdpParser {

    public static SessionDescription parse(String sdp) {
        if (StringUtils.isBlank(sdp)) {
            throw new SdpParseException("SDP is empty");
        }
        SdpParseState state = SdpParseState.SESSION;

        SessionDescription sessionDescription = new SessionDescription();
        MediaDescription currentMediaDescription = null;
        String[] lines = sdp.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("m=")) {
                currentMediaDescription = new MediaDescription();
                sessionDescription.addMediaDescription(currentMediaDescription);
                state = SdpParseState.MEDIA;
            }
            switch (state) {
                case SESSION:
                    SessionParser.parse(sessionDescription, line);
                    break;
                case MEDIA:
                    MediaParser.parse(currentMediaDescription, line);
                    break;
            }

        }


        return sessionDescription;
    }


    public enum SdpParseState {
        SESSION,
        MEDIA
    }
}
