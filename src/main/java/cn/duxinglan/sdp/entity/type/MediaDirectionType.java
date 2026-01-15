package cn.duxinglan.sdp.entity.type;

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
public enum MediaDirectionType {
    SENDRECV("sendrecv"),   // 双向发送接收
    SENDONLY("sendonly"),   // 只发送
    RECVONLY("recvonly"),   // 只接收
    INACTIVE("inactive");   // 不收不发，仅保持会话

    public final String value;

    MediaDirectionType(String value) {
        this.value = value;
    }

    public static MediaDirectionType fromValue(String value) {
        for (MediaDirectionType mediaDirectionType : MediaDirectionType.values()) {
            if (mediaDirectionType.value.equals(value)) {
                return mediaDirectionType;
            }
        }
        return null;
    }
}
