package cn.duxinglan.media.util;

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
public class ByteUtils {



    public static byte[] hexStringToByteArray(String hexStr) {
        if (hexStr == null || hexStr.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must not be null and must have even length");
        }

        int len = hexStr.length();
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hexStr.charAt(i), 16);   // 高四位
            int low = Character.digit(hexStr.charAt(i + 1), 16); // 低四位
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character at position " + i);
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }
}
