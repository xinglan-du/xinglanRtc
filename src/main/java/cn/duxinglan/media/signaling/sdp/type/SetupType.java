package cn.duxinglan.media.signaling.sdp.type;

import java.util.Arrays;

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
public enum SetupType {

    /**
     * 表示本端既可以充当 DTLS 客户端，也可以充当服务器，具体角色由对端决定。
     */
    ACTPASS("actpass"),

    /**
     * 表示本端仅可以充当 DTLS 客户端。
     */
    ACTIVE("active"),

    /**
     * 表示本端仅可以充当 DTLS 服务器。
     */
    PASSIVE("passive");


    public final String value;

    SetupType(String value) {
        this.value = value;
    }

    public static SetupType fromValue(String value) {
        return Arrays.stream(SetupType.values())
                .filter(setupType -> setupType.value.equals(value))
                .findAny()
                .orElse(null);
    }
}
