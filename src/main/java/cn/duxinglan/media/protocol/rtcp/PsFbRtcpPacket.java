package cn.duxinglan.media.protocol.rtcp;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@EqualsAndHashCode(callSuper = true)
@Data
public class PsFbRtcpPacket extends RtcpPacket {

    /**
     * 表示 RTCP 协议中 "FMT" (Format) 字段。
     * 该字段指定特定 RTCP 数据包的格式类型，通常与载荷类型和具体的 RTCP 功能相关。
     * 不同的 "FMT" 值可能表示不同的 RTCP 操作，具体定义应参考相应的 RTCP 协议标准。
     * <p>
     * 该字段值为整数，具体含义取决于上下文。
     */
    private int fmt;

    /**
     * 表示发送方同步源标识符 (SSRC, Synchronization Source Identifier)。
     * <p>
     * senderSsrc 是 RTCP 协议中用于标识数据流发送方的一个 32 位唯一标识符。
     * 它允许接收方知道数据包的来源，并在多个会话之间区分来自不同发送方的流。
     * <p>
     * 通常，SSRC 由发送方在会话初始化时随机生成，以确保其唯一性。
     * <p>
     * 本字段主要出现在 RTCP 报文中，例如 Sender Report (SR) 和 Source Description (SDES)
     * 数据包，用于提供发送方的标识信息。
     */
    private long senderSsrc;

    /**
     * 表示媒体同步源标识符 (SSRC, Synchronization Source Identifier)。
     * <p>
     * mediaSsrc 是 RTCP 协议中用于标识与某个媒体流关联的同步源的 32 位唯一标识符。
     * 它允许接收方将报告与特定的媒体流相关联，从而支持多流通信场景。
     * <p>
     * 在 RTP/RTCP 会话中，mediaSsrc 通常用于区分不同的媒体流，例如音频流、视频流等。
     * 发送者和接收者通过该字段了解每个媒体流的统计信息，如丢包率、抖动等。
     * <p>
     * 本字段的值由系统或会话初始化时随机生成，确保在会话范围内的唯一性。
     */
    private long mediaSsrc;

    /**
     * 表示 RTCP 协议中的 FCI (Feedback Control Information) 字段。
     * FCI 是 RTCP 协议中某些反馈消息的一个可选字段，用于携带特定的控制信息。
     * <p>
     * 不同类型的 RTCP 报文可能具有不同格式的 FCI 数据，具体格式应参考对应的
     * RTCP 协议标准或实现文档。例如：
     * - NACK (Negative Acknowledgment) RTCP 报文中的 FCI 字段用于指示丢失的 RTP 包序列号。
     * - PLI (Picture Loss Indication) 和 FIR (Full Intra Request) 报文中的 FCI 字段可为空。
     * <p>
     * 该字段的类型为 {@link ByteBuf}，用于高效地处理二进制数据。
     */
    private ByteBuf fci;

    private FirEntry firEntry;
}
