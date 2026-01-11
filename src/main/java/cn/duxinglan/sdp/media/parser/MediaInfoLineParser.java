package cn.duxinglan.sdp.media.parser;

import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.media.signaling.sdp.media.Info;
import cn.duxinglan.media.signaling.sdp.type.MediaInfoType;
import cn.duxinglan.media.signaling.sdp.type.MediaTransportType;
import cn.duxinglan.sdp.media.MediaLineParser;

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
public class MediaInfoLineParser extends MediaLineParser {

    public static final String KEY = "m=";

    @Override
    public String[] getLineStartWith() {
        return new String[]{KEY};
    }

    @Override
    protected boolean parse(MediaDescription mediaDescription, String key, String value) {
        String[] split = value.split(" ");
        Info info = new Info(MediaInfoType.fromValue(split[0]), Integer.parseInt(split[1]), MediaTransportType.fromValue(split[2]));
        mediaDescription.setInfo(info);
        return true;
    }


}
