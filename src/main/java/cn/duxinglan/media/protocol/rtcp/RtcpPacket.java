package cn.duxinglan.media.protocol.rtcp;

import lombok.Data;

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
@Data
public abstract class RtcpPacket {

    /**
     * 表示 RTCP 协议中版本号字段 (Version)。
     * 该字段用于标识 RTCP 数据包的协议版本，通常为固定值 2。
     */
    private byte version;

    /**
     * 表示 RTCP 数据包中的填充位 (Padding) 字段。
     * 该字段用于标识 RTCP 数据包中是否包含附加的填充字节，这些字节用于对齐数据包大小。
     * 填充字段在某些特定场景（如加密）下可能会被使用。
     */
    private byte padding;

    /**
     * 表示 RTCP 数据包中的载荷类型 (Payload Type) 字段。
     * 该字段用于标识当前 RTCP 数据包的载荷类型，其值根据 RTCP 协议标准定义或具体实现需求来分配。
     * 常见取值可以参考 RtcpPayloadType 枚举类。
     */
    private int payloadType;

    /**
     * 表示 RTCP 数据包中的长度字段 (Length)。
     * 该字段用于指定 RTCP 数据包的总长度，单位为 32 位字（4 字节）。
     * 长度字段的值应包括整个 RTCP 数据包的内容，从版本号字段 (Version) 到最后的填充字节。
     */
    private int length;


    public int getTotalLength() {
        return (length + 1) * 4;
    }


}
