package cn.duxinglan.media.protocol.srtp;

import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

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
@Slf4j
public class SrtpFactory {

    public static final int RTP_HEADER_LENGTH = 12;




    public static SRtpPacket parseBytebufToSrtpPacket(ByteBuf byteBuf, int rtpAuthTagLength) {
        SRtpPacket srtpPacket = new SRtpPacket(rtpAuthTagLength);
        srtpPacket.setEncryptByteBuf(byteBuf);
        return srtpPacket;
    }

    public static SRtpPacket parseDecryptBytebufToSrtpPacket(ByteBuf rtpBytebuf, SRtpContext serverSrtpContext, int rtpAuthTagLength) throws InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        SRtpPacket srtpPacket = new SRtpPacket(rtpAuthTagLength);
        srtpPacket.setDecryptByteBuf(rtpBytebuf);
        srtpPacket.encrypt(serverSrtpContext);
        return srtpPacket;
    }
}
