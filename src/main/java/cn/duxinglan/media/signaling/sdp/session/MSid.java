package cn.duxinglan.media.signaling.sdp.session;

import cn.duxinglan.media.signaling.sdp.type.MediaStreamType;

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
public record MSid(MediaStreamType type, String streamId, String trackId) {


    public static final String KEY = "msid-semantic";

    public static MSid defaultMsid() {
        return new MSid(MediaStreamType.WMS,null, null);
    }

    public static MSid defaultMSid(String streamId, String trackId) {
        return new MSid(MediaStreamType.WMS,streamId, trackId);
    }

    public static MSid parseLine(String line) {
        line = line.substring(line.indexOf(":") + 1).trim();
        String[] parts = line.split(" ");

        String type = parts[0];
        String streamId = parts.length > 1 ? parts[1] : null;
        String trackId = parts.length > 2 ? parts[2] : null;

        return new MSid(MediaStreamType.fromValue(type), streamId, trackId);
    }


    public String getSDPLine() {
        if (streamId != null && trackId != null) {
            return streamId + " " + trackId;
        }
        if (streamId != null) {
            return streamId;
        }
        if (trackId != null) {
            return trackId;
        }
        return "*";
    }
}
