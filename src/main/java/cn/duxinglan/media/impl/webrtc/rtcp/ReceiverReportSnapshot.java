package cn.duxinglan.media.impl.webrtc.rtcp;

import lombok.Getter;

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
