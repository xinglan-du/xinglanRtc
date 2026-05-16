package cn.duxinglan.media.impl.webrtc.rtcp;

import cn.duxinglan.media.protocol.rtp.RtpPacket;

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
public class RtpReceiveStats {

    private static final long RTP_SEQ_MOD = 1L << 16;

    // ================= 序列号 / 丢包 =================
    private boolean initialized = false;

    private int baseSeq;
    private int maxSeq;
    private long cycles;
    private long received;

    private long expectedPrior;
    private long receivedPrior;

    // ================= 抖动 =================
    private long jitter;
    private long lastTransit;

    // ================= SR / LSR / DLSR =================
    private long lastSr;              // middle 32 bits of NTP
    private long lastSrArrivalNs;

    /**
     * 处理一个 RTP 包
     *
     * @param packet        RTP 包
     * @param arrivalTimeNs 到达时间（System.nanoTime）
     * @param clockRate     RTP 时钟频率
     */
    public void onRtpPacket(RtpPacket packet, long arrivalTimeNs, int clockRate) {
        int seq = packet.getSequenceNumber() & 0xFFFF;

        updateSeq(seq);

        long arrivalRtpTime = arrivalTimeNs * clockRate / 1_000_000_000L;
        long transit = arrivalRtpTime - packet.getTimestamp();

        if (received > 1) {
            long d = Math.abs(transit - lastTransit);
            jitter += (d - jitter) / 16;
        }

        lastTransit = transit;
    }

    /**
     * 处理 Sender Report
     */
    public void onSenderReport(long ntpSec, long ntpFrac) {
        lastSr = ((ntpSec & 0xFFFF) << 16) | ((ntpFrac >> 16) & 0xFFFF);
        lastSrArrivalNs = System.nanoTime();
    }

    /**
     * 生成 RR 快照（不会修改历史统计）
     */
    public ReceiverReportSnapshot snapshot(long nowNs) {
        if (!initialized) {
            long dlsr = 0;
            if (lastSr != 0) {
                long delayNs = nowNs - lastSrArrivalNs;
                dlsr = (delayNs * 65536) / 1_000_000_000L;
            }
            return new ReceiverReportSnapshot(0, 0, 0, jitter, lastSr, dlsr);
        }

        long extendedMaxSeq = cycles + maxSeq;
        long expected = extendedMaxSeq - baseSeq + 1;
        long lost = expected - received;

        if (lost < 0) {
            lost = 0;
        } else if (lost > 0x7FFFFF) {
            lost = 0x7FFFFF;
        }

        long expectedInterval = expected - expectedPrior;
        long receivedInterval = received - receivedPrior;
        long lostInterval = expectedInterval - receivedInterval;

        int fractionLost = 0;
        if (expectedInterval > 0 && lostInterval > 0) {
            fractionLost = (int) ((lostInterval << 8) / expectedInterval);
        }

        expectedPrior = expected;
        receivedPrior = received;

        long dlsr = 0;
        if (lastSr != 0) {
            long delayNs = nowNs - lastSrArrivalNs;
            dlsr = (delayNs * 65536) / 1_000_000_000L;
        }

        return new ReceiverReportSnapshot(
                fractionLost,
                lost,
                extendedMaxSeq,
                jitter,
                lastSr,
                dlsr
        );
    }

    // ================= 内部方法 =================

    private void updateSeq(int seq) {
        seq &= 0xFFFF;
        if (!initialized) {
            baseSeq = seq;
            maxSeq = seq;
            initialized = true;
            received = 1;
            return;
        }

        long currentExtendedMax = cycles + maxSeq;
        long extendedSeq = extendToNearestCycle(seq, currentExtendedMax);
        if (extendedSeq > currentExtendedMax) {
            maxSeq = (int) (extendedSeq & 0xFFFF);
            cycles = extendedSeq & 0xFFFF0000L;
        }

        received++;
    }

    private long extendToNearestCycle(int seq16, long referenceExtendedSeq) {
        int referenceSeq16 = (int) (referenceExtendedSeq & 0xFFFF);
        long diff = seq16 - referenceSeq16;
        if (diff > (RTP_SEQ_MOD / 2 - 1)) {
            diff -= RTP_SEQ_MOD;
        } else if (diff < -(RTP_SEQ_MOD / 2)) {
            diff += RTP_SEQ_MOD;
        }
        return referenceExtendedSeq + diff;
    }
}
