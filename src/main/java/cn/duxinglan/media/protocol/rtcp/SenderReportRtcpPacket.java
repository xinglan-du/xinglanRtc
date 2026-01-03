package cn.duxinglan.media.protocol.rtcp;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
@EqualsAndHashCode(callSuper = true)
@Data
public class SenderReportRtcpPacket extends RtcpPacket {

    private static final long NTP_UNIX_EPOCH_DIFF_MS = 2208988800L * 1000;


    /**
     * 表示同步源标识符 (SSRC, Synchronization Source Identifier)。
     * 用于唯一标识 RTCP 数据包的源节点。
     * 在同一个 RTP/RTCP 会话中，SSRC 应该是唯一的，以确保能够正确地区分来自不同源的数据流。
     */
    private long ssrc;

    /**
     * 表示 RTCP 数据包中的接收者报告块计数字段 (Reception Report Count)。
     * 该字段用于标识当前 RTCP 数据包中包含的接收者报告块的数量。
     * 范围为 0 到 31，由发送端根据需要设置。
     */
    private int rc;


    /**
     * 表示 NTP 时间戳中的秒部分，用于指定发送者报告 (Sender Report) 中的时间戳。
     * NTP 时间戳由秒 (ntpSec) 和分数 (ntpFrac) 两部分组成，
     * 用于同步发送端和接收端的时间，确保多媒体数据的时序一致性。
     */
    private long ntpSec;

    /**
     * 表示 NTP 时间戳中的分数部分，用于指定发送者报告 (Sender Report) 中的时间戳。
     * NTP 时间戳由秒 (ntpSec) 和分数 (ntpFrac) 两部分组成，
     * 分数部分表示秒的小数部分，通常用于更高精度的时间信息表示。
     * 用于同步发送端和接收端的时间，确保多媒体数据的时序一致性。
     */
    private long ntpFrac;

    /**
     * 表示 RTP 时间戳字段，用于标识发送者报告 (Sender Report) 中 RTP 数据包的时间戳。
     * RTP 时间戳通常与 NTP 时间戳 (ntpSec 和 ntpFrac) 配合使用，用于多媒体数据的时间同步和时序控制。
     * 它表示发送者在生成 RTP 数据包时的采样时间点，单位依赖于所使用的 RTP 载荷格式规定的时钟频率。
     * 接收端可通过 RTP 时间戳实现媒体流的解码同步和回放。
     */
    private long rtpTimestamp;

    /**
     * 表示发送者报告 (Sender Report) 中发送方已传输的 RTP 数据包总数。
     * 此计数用于记录从会话开始到当前时刻，发送端成功传输的 RTP 包数量。
     * <p>
     * 接收端可以通过此字段与自己接收到的 RTP 包数量进行对比，
     * 用于检测数据包丢失以及评估传输性能。
     */
    private long senderPacketCount;

    /**
     * 表示发送者报告 (Sender Report) 中发送方在会话期间累计传输的 RTP 数据包的总字节数。
     * 该值包括从会话开始到当前时刻，RTP 发送方成功传输的所有数据的总字节数（单位为字节）。
     * <p>
     * 接收方可以利用此字段配合其他统计信息评估网络传输的性能，例如总数据量和效率。
     */
    private long senderOctetCount;

    /**
     * 表示发送者报告 (Sender Report) 包含的接收报告块列表。
     * 每个接收报告块 (Report Block) 描述了一个接收端关于发送方传输质量的统计信息。
     * <p>
     * 每个 ReportBlock 包括发送源的 SSRC、丢包率 (Fraction Lost)、累计丢包数 (Cumulative Lost)、最高序列号 (Highest Sequence Number) 等字段。
     * 该列表可用于分析网络传输的质量，包括丢包、抖动等。
     */
    public List<ReportBlock> reportBlocks = new ArrayList<>();

}
