package cn.duxinglan.media.impl.webrtc.rtcp;

import lombok.Getter;

@Getter
public class ReceiverReportSnapshot {

    private final int fractionLost;      // 8bit
    private final long cumulativeLost;   // 24bit signed
    private final long extendedMaxSeq;   // 32bit
    private final long jitter;            // 32bit
    private final long lsr;               // 32bit
    private final long dlsr;              // 32bit

    public ReceiverReportSnapshot(int fractionLost,
                                  long cumulativeLost,
                                  long extendedMaxSeq,
                                  long jitter,
                                  long lsr,
                                  long dlsr) {
        this.fractionLost = fractionLost;
        this.cumulativeLost = cumulativeLost;
        this.extendedMaxSeq = extendedMaxSeq;
        this.jitter = jitter;
        this.lsr = lsr;
        this.dlsr = dlsr;
    }

}
