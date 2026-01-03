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
public class ReceiverReportBlock {


    private long sourceSsrc;

    private int fractionLost;

    private int lost;

    long extHighestSeq;
    private int cycles;
    private int maxSeq;

    private long jitter;

    private long lsr;

    private long dlsr;

    private double delaySeconds;


    public void setExtHighestSeq(long extHighestSeq) {
        this.extHighestSeq = extHighestSeq;
        this.cycles = (int) (extHighestSeq >> 16);
        this.maxSeq = (int) (extHighestSeq & 0xFFFF);
    }

    public void setDlsr(long dlsr) {
        this.dlsr = dlsr;
        this.delaySeconds = dlsr / 65536.0;
    }
}
