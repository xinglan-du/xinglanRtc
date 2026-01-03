package cn.duxinglan.media.impl.sdp;

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
public record IceInfo(String ufrag, String pwd, String options) {

    public static final String UFRAG_KEY = "ice-ufrag";
    public static final String PWS = "ice-pwd";
    public static final String OPTIONS = "ice-options";

    // ICE-CHAR 字符集（RFC 5245/8445）：ALPHA / DIGIT / "+" / "/"
    private static final char[] ICE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private static String randomIce(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = ICE_CHARS[SECURE_RANDOM.nextInt(ICE_CHARS.length)];
        }
        return new String(buf);
    }

    public static IceInfo defaultIceInfo() {
        // 规范：ufrag 长度 4–256；pwd 长度 22–256。这里选择更安全的长度。
        String ufrag = randomIce(16);
        String pwd = randomIce(32);
        return new IceInfo(ufrag, pwd, "trickle");
    }

}
