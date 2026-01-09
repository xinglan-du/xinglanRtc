package cn.duxinglan.sdp.session.parser;

import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.media.signaling.sdp.session.MSid;
import cn.duxinglan.media.signaling.sdp.type.MediaStreamType;
import cn.duxinglan.sdp.session.SessionLineParser;
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
public class MSidExpandParser extends SessionLineParser {

    public static final String KEY = "msid-semantic";

    private static final String BUNDLE_KEY = "BUNDLE";

    @Override
    public String getLineStartWith() {
        return KEY;
    }

    @Override
    protected void parse(SessionDescription sessionDescription, String key, String value) {
        String[] parts = value.split(" ");

        String type = parts[0];
        String streamId = parts.length > 1 ? parts[1] : null;
        String trackId = parts.length > 2 ? parts[2] : null;

        MSid mSid = new MSid(MediaStreamType.fromValue(type), streamId, trackId);
        sessionDescription.setMSid(mSid);
    }
}
