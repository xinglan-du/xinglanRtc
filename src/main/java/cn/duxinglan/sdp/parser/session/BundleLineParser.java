package cn.duxinglan.sdp.parser.session;

import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.media.signaling.sdp.session.Timing;
import cn.duxinglan.sdp.SdpLineParser;
import org.apache.commons.lang3.math.NumberUtils;

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
public class BundleLineParser extends SdpLineParser {

    public static final String KEY = "t=";

    @Override
    public String getLineStartWith() {
        return KEY;
    }

    @Override
    protected void parse(SessionDescription sessionDescription, String key, String value) {
        String[] split = value.split(" ");
        Timing timing = new Timing(NumberUtils.toLong(split[0]), NumberUtils.toLong(split[1]));
        sessionDescription.setTiming(timing);
    }


}
