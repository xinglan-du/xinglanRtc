package cn.duxinglan.media.impl.webrtc.rtcp;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
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
public class SequenceNumberTracker {

    private boolean initialized = false;

    /**
     * 表示接收 RTP 包的基准序列号。
     * 该字段用于初始化序列号的基准值，通常在第一次接收到 RTP 包时设置。
     *
     * 此字段的值决定了接收的 RTP 序列号范围的起点，并且在统计接收包、丢包情况时
     * 起到重要作用。结合其他字段（如最大序列号）可计算总接收包数和丢失包数。
     */
    private int baseSeq = 0;          // 第一个接收序列号
    /**
     * 当前已接收的最大 RTP 包序列号。
     *
     * 此字段用于跟踪 RTP 包序列号的最大值，并在序列号更新过程中辅助检测序列号回绕情况。
     * 对于 16 位的序列号空间，此字段始终记录未扩展的最大序列号值。
     */
    private int maxSeq = 0;           // 当前最大序列号
    /**
     * 表示 RTP 序列号循环次数。
     *
     * 在 RTP 序列号为 16 位的情况下，当序列号从 0 到最大值 65535 发生回绕时，
     * 本变量用于记录发生了多少次回绕，以便能够对扩展序列号进行正确计算。
     *
     * 扩展序列号的计算公式为：cycles * 2^16 + 当前序列号。
     */
    private long cycles = 0;          // 序列号循环次数
    /**
     * 表示接收的 RTP 包的总数量。
     *
     * 该变量用于追踪接收到的 RTP 包数量，随着每个有效包的接收而递增。
     */
    private long received = 0;        // 已接收包数

    private final LinkedHashSet<Integer> missingSet = new LinkedHashSet<>(); // 丢失序列号
    private static final int REORDER_WINDOW = 50; // 乱序窗口大小，可调

    /**
     * 接收一个 RTP 包的序列号
     *
     * @param seq RTP 包序列号（16bit）
     */
    public void receive(int seq) {
        seq &= 0xFFFF; // 保证是 16bit

        if (!initialized) {
            baseSeq = seq;
            maxSeq = seq;
            received = 1;
            initialized = true;
            return;
        }

        // 检测序列号回绕
        if (seq < maxSeq && maxSeq - seq > 30000) {
            cycles += 1L << 16;
        }

        long extendedSeq = seq + cycles;

        long extendedMaxSeq = maxSeq + cycles;

        // 丢包检测
        if (extendedSeq > extendedMaxSeq) {
            for (long missing = extendedMaxSeq + 1; missing < extendedSeq; missing++) {
                missingSet.add((int) (missing & 0xFFFF)); // 记录丢失的16bit序列号
            }
            maxSeq = seq;
        } else if (missingSet.contains(seq)) {
            // 收到乱序包，移除
            missingSet.remove(seq);
        }

        // 清理乱序窗口外的过期丢包
        cleanUpMissingSet(extendedMaxSeq);

        received++;
    }

    /**
     * 获取当前丢失的序列号列表
     *
     * @return 丢失序列号列表（16bit）
     */
    public List<Integer> getMissingSeqs() {
        return new ArrayList<>(missingSet);
    }

    /**
     * 清理超过 reorder window 的丢包序列号
     */
    private void cleanUpMissingSet(long extendedMaxSeq) {
        Iterator<Integer> it = missingSet.iterator();
        while (it.hasNext()) {
            int seq = it.next();
            long extended = seq + cycles;
            if (extended + REORDER_WINDOW < extendedMaxSeq) {
                it.remove();
            } else {
                break; // 后续都是新序列号，跳过
            }
        }
    }

    /**
     * 获取当前接收的最大扩展序列号
     */
    public long getExtendedMaxSeq() {
        return maxSeq + cycles;
    }

    /**
     * 获取总接收包数
     */
    public long getReceived() {
        return received;
    }

    /**
     * 获取基序列号
     */
    public int getBaseSeq() {
        return baseSeq;
    }

    /**
     * 获取循环次数
     */
    public long getCycles() {
        return cycles;
    }

    /**
     * 重置 tracker（例如新的媒体流）
     */
    public void reset() {
        initialized = false;
        baseSeq = 0;
        maxSeq = 0;
        cycles = 0;
        received = 0;
        missingSet.clear();
    }
}
