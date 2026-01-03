package cn.duxinglan.media.protocol.rtp;

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
public class RtpTimeState {

    /**
     * 表示 NTP (Network Time Protocol) 的秒数部分。
     * 该字段用于存储网络时间协议中的整秒数值，
     * 通常结合 ntpFrac 字段一起表示完整的 64 位 NTP 时间戳。
     * <p>
     * NTP 时间戳由两个部分组成：
     * - 高 32 位表示自 1900 年 1 月 1 日以来的累计整秒数，即该字段的值。
     * - 低 32 位表示未满一秒的小数部分，由 ntpFrac 字段存储。
     * <p>
     * 此字段在 RTP 协议中可用于同步媒体流的数据包时间。
     */
    private long ntpSec;

    /**
     * 表示 NTP (Network Time Protocol) 的小数部分。
     * 该字段用于存储网络时间协议中未满一秒的部分，
     * 通常与 ntpSec 字段结合使用，以构成完整的64位NTP时间戳。
     * <p>
     * NTP 时间戳由两个部分组成：
     * - 高 32 位为整秒数，使用 ntpSec 字段表示。
     * - 低 32 位为小数部分，即该字段的值，表示一秒内的精确时间。
     * <p>
     * 此字段在 RTP 协议中用于精确时间同步，尤其在高精度多媒体流传输中起到关键作用。
     */
    private long ntpFrac;

    /**
     * 表示 RTP (Real-time Transport Protocol) 基准时间戳 (Base RTP Timestamp)。
     * <p>
     * 该字段用于存储 RTP 数据包时间戳的基准值，通常由 RTCP (Real-time Transport Control Protocol)
     * 的 Sender Report (SR) 消息中提供。其作用是在 RTP 会话中同步及计算媒体数据的时间戳。
     * <p>
     * 在接收到 RTCP SR 消息时，该字段会被更新为当前的 RTP 时间戳值，并与 NTP 时间戳 (ntpSec 和 ntpFrac)
     * 关联，以用于后续的时间同步和偏移计算。
     * <p>
     * 使用场景包括：
     * - 帮助解析媒体流的时间戳以实现同步播放。
     * - 提供媒体流的基准时间，用于计算数据包的相对偏移。
     */
    private long baseRtpTs;

    /**
     * 表示以纳秒为单位的基准 NTP (Network Time Protocol) 时间戳。
     * <p>
     * 该字段用于存储 RTCP (Real-time Transport Control Protocol) Sender Report (SR)
     * 消息中基准 NTP 时间戳的纳秒值形式。基准 NTP 时间戳通过将 64 位的 NTP 秒数部分
     * (ntpSec) 和小数部分 (ntpFrac) 转换为纳秒后计算得出。
     * <p>
     * 计算方式为：
     * - 纳秒值 = ntpSec * 1,000,000,000 + (ntpFrac * 1,000,000,000 >> 32)
     * <p>
     * 主要用途：
     * - 有助于对 RTP (Real-time Transport Protocol) 媒体流进行高精度的时间同步。
     * - 提供基准时间，用于计算发送或接收的 RTP 数据包的时间偏移。
     */
    private long baseNtpNs;

    /**
     * 表示是否接收到 RTCP (Real-time Transport Control Protocol) Sender Report (SR) 消息的标志。
     * <p>
     * 该字段用于指示是否已接收到至少一条 RTCP SR 消息，并更新了相关的基准时间。
     * 当接收到有效的 RTCP SR 消息后，该字段将被设置为 true。
     * <p>
     * 主要用途：
     * - 标记 RTP 时间同步状态。
     * - 用于判断是否可以依赖基准时间 (baseRtpTs 和 baseNtpNs) 进行后续的时间同步计算。
     * <p>
     * 默认值为 false，表示尚未接收到任何 RTCP SR 消息。
     */
    private boolean hasSr = false;


    /**
     * 更新 RTP 时间状态，利用 RTCP 发件人报告 (Sender Report) 中的 NTP 时间和 RTP 时间戳。
     *
     * @param ntpSec 表示 NTP (Network Time Protocol) 时间戳的秒数部分。
     * @param ntpFrac 表示 NTP (Network Time Protocol) 时间戳的小数部分。
     * @param rtpTs 表示 RTP (Real-time Transport Protocol) 基准时间戳，用于同步媒体流的时间。
     */
    public void updateFromSr(long ntpSec, long ntpFrac, long rtpTs) {
        this.ntpSec = ntpSec;
        this.ntpFrac = ntpFrac;
        this.baseRtpTs = rtpTs;
        this.baseNtpNs =
                ntpSec * 1_000_000_000L
                + ((ntpFrac * 1_000_000_000L) >>> 32);
        this.hasSr = true;
    }
}
