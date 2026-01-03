package cn.duxinglan.media.protocol.rtp;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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

/**
 * 用于表示 RTP (实时传输协议) 数据包的结构体类。
 * 该类包含 RTP 协议所需的所有字段和方法，支持 RTP 数据包的解析和操作。
 * 可用于传输音频、视频等媒体流数据。
 */
@Data
@Slf4j
public class RtpPacket {

    /**
     * 接收顺序号计数器 (Receiver Order Counter) 的值，用于 RTP 协议中接收端维护报文顺序的计数。
     */
    private int roc;

    /**
     * RTP 协议版本号，用于标识 RTP 数据包的协议版本。
     */
    private byte version;

    /**
     * 表示 RTP 数据包中的填充位 (Padding) 字段，用于标识 RTP 数据的填充信息。
     */
    private byte padding;

    /**
     * 表示 RTP 数据包中的扩展位 (Extension) 字段，用于标识 RTP 数据包是否包含扩展头信息。
     */
    private byte extension;

    /**
     * 表示 CSRC 计数 (Contributing Source Count)，对应 RTP 数据包中 CSRC 标识符的数量，范围为 0 到 15。
     * 用于标识哪些媒体源贡献了当前的 RTP 数据包。
     */
    private byte csrcCount;

    /**
     * 表示 RTP 数据包中的标记位 (Marker) 字段。
     * 该字段通常用于标识帧的边界或某些特定事件，具体定义依赖于所使用的 RTP 载荷格式。
     */
    private byte marker;

    /**
     * 表示 RTP 数据包中的载荷类型 (Payload Type) 字段。
     * 该字段用于指定 RTP 数据包传输的媒体类型或编码格式。
     * 其值根据 RTP 协议的标准定义或特定应用的需求进行分配。
     */
    private int payloadType;

    /**
     * 表示 RTP 数据包中的序列号字段 (Sequence Number)。
     * 序列号用于标识数据包的顺序，接收端可以利用该字段检测数据包的丢失或乱序情况。
     * 通常在每次发送新的 RTP 数据包时，序列号会递增。
     */
    private int sequenceNumber;

    /**
     * 表示 RTP 数据包中的时间戳字段 (Timestamp)。
     * 该字段用于标识 RTP 数据包中数据的时间点，通常用于同步多媒体流。
     * 时间戳值的单位和计算方式由 RTP 协议的载荷格式指定。
     */
    private long timestamp;

    /**
     * 表示同步源标识符 (Synchronization Source Identifier)。
     * 用于唯一标识 RTP 数据包流的源节点。
     * 在同一个 RTP 会话中，SSRC 应该是唯一的，以确保接收方能够区分不同发送方的数据流。
     */
    private long ssrc;

    /**
     * 表示当前 RTP 数据包中的 CSRC 标识符列表 (Contributing Source List)。
     * CSRC 用于标识对当前包的数据有贡献的媒体源。
     * 该字段是一个可变大小的列表，存储来自多个 CSRC 的标识符。
     */
    private final List<Long> csrcList = new ArrayList<>();

    /**
     * 表示 RTP 数据包的载荷数据。
     * 该字段为一个字节缓冲区 (ByteBuf)，用于存储 RTP 数据包中的实际媒体数据内容。
     * 数据内容可能包括音频、视频或其他类型的媒体数据，具体由 RTP 协议的载荷类型字段 (Payload Type) 定义。
     */
    private ByteBuf payload;


    public ByteBuf getPayload() {
        return payload.slice();
    }
}
