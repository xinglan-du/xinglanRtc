package cn.duxinglan.media.impl.webrtc.rtcp;

import cn.duxinglan.media.protocol.rtp.RtpPacket;

public class RtpReceiveStats {

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
        int seq = packet.getSequenceNumber();

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
        // middle 32 bits of NTP timestamp
        lastSr = ((ntpSec & 0xFFFF) << 16) | ((ntpFrac >> 16) & 0xFFFF);
        lastSrArrivalNs = System.nanoTime();
    }

    /**
     * 生成 RR 快照（不会修改历史统计）
     */
    public ReceiverReportSnapshot snapshot(long nowNs) {
        long extendedMaxSeq = cycles + maxSeq;
        long expected = extendedMaxSeq - baseSeq + 1;
        long lost = expected - received;

        // clamp to signed 24bit
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
        if (!initialized) {
            baseSeq = seq;
            maxSeq = seq;
            initialized = true;
            received = 1;
            return;
        }

        // 检测回绕
        if (seq < maxSeq && maxSeq - seq > 30000) {
            cycles += 1L << 16;
        }

        if (seq > maxSeq) {
            maxSeq = seq;
        }

        received++;
    }
}
