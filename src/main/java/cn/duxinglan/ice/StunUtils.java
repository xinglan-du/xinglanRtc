package cn.duxinglan.ice;

import io.netty.buffer.ByteBuf;

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
public class StunUtils {

    public static final int STUN_DATA_HEADER_LENGTH = 20;



    public static final int STUN_COOKIE = 0x2112A442;

    public static final int FINGERPRINT_XOR = 0x5354554e; // "STUN"


    public static boolean isStunMessage(ByteBuf content) {
        // STUN 消息至少 20 字节
        if (content.readableBytes() < STUN_DATA_HEADER_LENGTH) {
            return false;
        }

        // 标记当前读索引，避免影响后续读
        int readerIndex = content.readerIndex();

        // 取前两个字节，前两位必须是 0
        byte b0 = content.getByte(readerIndex);
        if ((b0 & 0xC0) != 0) {
            return false;
        }

        // 取 magic cookie（固定 0x2112A442）
        int cookie = content.getInt(readerIndex + 4);
        return cookie == STUN_COOKIE;
    }


}
