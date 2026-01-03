package cn.duxinglan.media.signaling.sdp.media;

import cn.duxinglan.media.signaling.sdp.type.IpVerType;
import cn.duxinglan.media.signaling.sdp.type.NetworkType;

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
public record RtcpConnection(int port, NetworkType netType, IpVerType ipVerType, String address) {


    public static final String KEY = "rtcp";

    public static RtcpConnection defaultRtcpConnection() {
        return new RtcpConnection(9, NetworkType.IN, IpVerType.IPV4, "0.0.0.0");
    }

    public static RtcpConnection parseLine(String line) {
        String[] split = line.split(" ");
        return new RtcpConnection(Integer.parseInt(split[0]), NetworkType.fromValue(split[1]), IpVerType.fromValue(split[2]), split[3]);

    }
}
