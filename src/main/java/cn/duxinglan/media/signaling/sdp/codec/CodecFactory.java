package cn.duxinglan.media.signaling.sdp.codec;

import cn.duxinglan.media.signaling.sdp.type.CodecType;

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
public class CodecFactory {

    public static final String RTMP = "a=rtpmap:";

    public static VideoCodec createVideoCodec(CodecType type) {
        return switch (type) {
            case VP8 -> new VP8Codec();
        };
    }


    public static VideoCodec lineToVideoCodec(String line) {
        String[] s = line.replaceAll(RTMP, "").split(" ");
        return switch (Integer.parseInt(s[0])) {
            case 96 -> new VP8Codec();
            default -> null;
        };

    }
}
