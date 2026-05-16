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

    /**
     * 用于定义 RTP 的序列号循环模数，其值为 2 的 16 次方（65536）。
     * RTP 序列号为 16 位，因此当序列号达到 65535 后会回绕至 0，此值用于处理
     * 序列号循环的相关计算和扩展。
     */
    private static final long RTP_SEQ_MOD = 1L << 16;

    /**
     * 表示序列号跟踪器是否已完成初始化。
     * <p>
     * 该变量用于标记当前序列号跟踪器的初始化状态。
     * 当该值为 false 时，表示跟踪器尚未初始化；
     * 当该值为 true 时，表示跟踪器已经完成初始化，可以开始处理 RTP 包。
     */
    private boolean initialized = false;

    /**
     * 表示接收 RTP 包的基准序列号。
     * 该字段用于初始化序列号的基准值，通常在第一次接收到 RTP 包时设置。
     * <p>
     * 此字段的值决定了接收的 RTP 序列号范围的起点，并且在统计接收包、丢包情况时
     * 起到重要作用。结合其他字段（如最大序列号）可计算总接收包数和丢失包数。
     */
    private int baseSeq = 0;          // 第一个接收序列号
    /**
     * 当前已接收的最大 RTP 包序列号。
     * <p>
     * 此字段用于跟踪 RTP 包序列号的最大值，并在序列号更新过程中辅助检测序列号回绕情况。
     * 对于 16 位的序列号空间，此字段始终记录未扩展的最大序列号值。
     */
    private int maxSeq = 0;           // 当前最大序列号
    /**
     * 表示 RTP 序列号循环次数。
     * <p>
     * 在 RTP 序列号为 16 位的情况下，当序列号从 0 到最大值 65535 发生回绕时，
     * 本变量用于记录发生了多少次回绕，以便能够对扩展序列号进行正确计算。
     * <p>
     * 扩展序列号的计算公式为：cycles * 2^16 + 当前序列号。
     */
    private long cycles = 0;          // 序列号循环次数
    /**
     * 表示接收的 RTP 包的总数量。
     * <p>
     * 该变量用于追踪接收到的 RTP 包数量，随着每个有效包的接收而递增。
     */
    private long received = 0;        // 已接收包数

    /**
     * 一个用于存储丢失扩展序列号（32 位）的 `LinkedHashSet` 集合。
     * <ul>
     * - 该集合用于追踪当前丢失的序列号，以便在需要构建 NACK（Negative Acknowledgement）或其他恢复逻辑时使用。
     * - 按插入顺序保存扩展序列号，确保后续处理过程中顺序一致。
     * - 结合接收的 RTP 包和序列号清理函数一起协作，维护一个动态更新的丢包列表。
     * </ul>
     */
    private final LinkedHashSet<Long> missingSet = new LinkedHashSet<>(); // 丢失扩展序列号

    /**
     * 用于定义乱序窗口的大小，即允许的最大乱序范围。
     * 乱序范围决定了在处理接收到的 RTP 序列号时，系统允许的乱序范围，
     * 超出此范围的序列号可能会被视为丢失。
     * <p>
     * 此常量在 RTP 序列号追踪算法中起到关键作用，确保在网络可能出现的包乱序情况下，
     * 正确地检测和恢复包序列，同时避免过多的误判。
     * <p>
     * 可根据实际网络情况和对乱序包容性需求进行调整。
     */
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

        long extendedMaxSeq = maxSeq + cycles;
        long extendedSeq = extendToNearestCycle(seq, extendedMaxSeq);

        // 丢包检测
        if (extendedSeq > extendedMaxSeq) {
            for (long missing = extendedMaxSeq + 1; missing < extendedSeq; missing++) {
                missingSet.add(missing);
            }
            maxSeq = (int) (extendedSeq & 0xFFFF);
            cycles = extendedSeq & 0xFFFF0000L;
        } else {
            // 收到乱序包，若此前标记过丢失则移除
            missingSet.remove(extendedSeq);
        }

        // 清理乱序窗口外的过期丢包
        cleanUpMissingSet(maxSeq + cycles);

        received++;
    }

    /**
     * 获取当前丢失的序列号列表
     *
     * @return 丢失序列号列表（16bit）
     */
    public List<Integer> getMissingSeqs() {
        List<Integer> result = new ArrayList<>(missingSet.size());
        for (Long extendedSeq : missingSet) {
            result.add((int) (extendedSeq & 0xFFFF));
        }
        return result;
    }

    /**
     * 获取当前丢失扩展序列号列表（用于构建 NACK）
     */
    public List<Long> getMissingExtendedSeqs() {
        return new ArrayList<>(missingSet);
    }


    /**
     * 清理过期的丢失扩展序列号，使其不再包含超过允许重排序窗口范围的旧序列号。
     *
     * @param extendedMaxSeq 当前接收的最大扩展序列号（32 位），用于判断哪些序列号已经过期。
     */
    private void cleanUpMissingSet(long extendedMaxSeq) {
        Iterator<Long> it = missingSet.iterator();
        while (it.hasNext()) {
            long extended = it.next();
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
        return initialized ? maxSeq + cycles : 0;
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

    /**
     * 将给定的 16 位序列号扩展为一个最接近的 32 位扩展序列号，用于处理序列号循环的情况下仍能正确计算连续性。
     *
     * @param seq16                当前 RTP 包的 16 位短序列号。
     * @param referenceExtendedSeq 当前已知的参考扩展序列号（32 位），用于确定扩展的基准点。
     * @return 扩展为 32 位的扩展序列号，其值为与参考扩展序列号最接近的可能结果。
     */
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
