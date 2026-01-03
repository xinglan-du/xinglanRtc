package cn.duxinglan.media.impl.webrtc;

import java.security.SecureRandom;

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
public final class SsrcGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a non-zero 32-bit SSRC.
     */
    public static long generateSsrc() {
        long ssrc;
        do {
            ssrc = RANDOM.nextInt() & 0xFFFFFFFFL;
        } while (ssrc == 0);
        return ssrc;
    }
}
