package cn.duxinglan.media.signaling.sdp.session;

import cn.duxinglan.media.signaling.sdp.type.AddressType;
import cn.duxinglan.media.signaling.sdp.type.NetworkType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

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
public record Origin(String username, long sessionId, int sessionVersion, NetworkType networkType,
                     AddressType addressType, String unicastAddress) {

    public static final String KEY = "o";

    public static Origin defaultOrigin(String username, InetAddress inetAddress) {
        if (StringUtils.isEmpty(username)) {
            username = "-";
        }
        AddressType addressType;
        if (inetAddress instanceof Inet4Address) {
            addressType = AddressType.IP4;
        } else {
            addressType = AddressType.IP6;
        }

        return new Origin(username, generateSessionId(), 0, NetworkType.IN, addressType, inetAddress.getHostAddress()); // 默认值为 1
    }


    public static long generateSessionId() {
        return ThreadLocalRandom.current().nextLong();
    }


    public static Origin parseLine(String line) {
        String[] split = line.split(" ");
        return new Origin(split[0], NumberUtils.toLong(split[1]), NumberUtils.toInt(split[2]), NetworkType.fromValue(split[3]), AddressType.fromValue(split[4]), split[5]); // 默认值为 1
    }
}
