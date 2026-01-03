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
public class ReportBlock {
    /**
     * 表示 RTP 协议中的同步源标识符 (SSRC, Synchronization Source Identifier)。
     * SSRC 是一个 32 位的唯一标识符，用于标识一个 RTP 数据流的来源。
     * 在一个会话中，所有参与者的 SSRC 应该彼此不同，以便接收方能够区分来自不同发送方的数据流。
     */
    public long ssrc;
    /**
     * 表示 RTP 数据包中丢失的分数率 (Fraction Lost)。
     * 该变量表示接收端在某个时间间隔内计算出的丢包比例。
     * Fraction Lost 是一个 8 位的无符号数，表示为 256 的分数单位。
     * 它的计算公式为：
     *
     * fractionLost = ((接收到的数据包数 - 总数据包数) / 总数据包数) * 256
     *
     * 该字段用于评估 RTP 数据流的传输质量，检测是否发生了数据包的丢失情况。
     */
    public int fractionLost;

    /**
     * 表示自会话开始到当前时刻累计的丢包数量 (Cumulative Number of Packets Lost)。
     * 该字段用于反映 RTP 数据流传输过程中发生丢包的累计数量。
     *
     * 它是一个有符号的 24 位整数，如果值为负数，则表明可能存在接收端故障或者统计错误；
     * 如果值为正数，则代表丢失的 RTP 数据包数量。
     *
     * 该字段用于网络质量的评估，接收端可以通过它监控传输的可靠性。
     */
    public int cumulativeLost;

    /**
     * 表示接收端记录的最高接收到的 RTP 数据包的序列号 (Highest Sequence Number)。
     * 该字段用于描述从会话开始到当前时刻接收的 RTP 数据包中的最大序列号。
     *
     * 在 RTP 会话中，每个发送的 RTP 包都包含一个递增的序列号，
     * 接收端使用这个字段来跟踪数据包顺序和检测数据包丢失。
     *
     * 该值可以用于评估网络传输的可靠性，并结合其他字段 (如 Cumulative Lost、Fraction Lost) 分析传输中的丢包情况。
     */
    public long highestSeq;

    /**
     * 表示 RTP 数据流中测量到的抖动 (Jitter) 值。
     * 抖动是指网络传输过程中数据包到达时间的变化，通常由网络延迟的不稳定性造成。
     *
     * 抖动值是接收端根据接收数据包的时间戳和到达时间计算得出的，
     * 通常用于评估网络传输质量和实时应用的表现。
     *
     * 该字段值为一个无符号 32 位整数，表示 RTP 时间单位中的抖动大小。
     */
    public long jitter;

    /**
     * 表示最近一次接收到的发送者报告 (Sender Report) 的时间戳。
     * 该时间戳是以 NTP 时间格式记录的 32 位中间值，通常用于同步发送端和接收端的时间。
     *
     * 接收端可以通过此字段结合其他统计信息计算从发送者报告时间到当前时刻的时间差，从而评估传输延迟。
     */
    public long lastSr;

    /**
     * 表示自最近一次接收到发送者报告（Sender Report）以来的延迟时间。
     * 该字段的值是一个 32 位的整数，单位为 RTP 时钟周期。
     * 它用于衡量从发送端发送最后一个发送者报告到接收端接收的时间间隔，
     * 并结合其他字段信息评估 RTP 数据流的传输延迟。
     *
     * 通常此字段用于收集 RTP 报文统计中的网络性能指标，计算公式为：
     *
     * delay = 当前时间 - 最近一次接收到的发送者报告时间
     */
    public long delaySinceLastSr;
}
