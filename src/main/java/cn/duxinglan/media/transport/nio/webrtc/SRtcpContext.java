package cn.duxinglan.media.transport.nio.webrtc;

import lombok.Data;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;

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
public class SRtcpContext {


    private final long ssrc;

    private int lastSeq = -1;


    private final Cipher cipher;


    private int opmode;

    private SecretKeySpec spec;

    private SrtpKeyDerivationFunction kdf;

    private int sentIndex = 1;


    public SRtcpContext(long ssrc, SrtpKeyDerivationFunction kdf, int opmode) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.ssrc = ssrc;
        this.opmode = opmode;
        this.kdf = kdf;
        kdf.calculationSaltKey(SrtpKeyDerivationFunction.LABEL_RTCP_SALT);
        kdf.calculationAuthKey(SrtpKeyDerivationFunction.LABEL_RTCP_MSG_AUTH);
        kdf.calculationEncKey(SrtpKeyDerivationFunction.LABEL_RTCP_ENCRYPTION);
        this.spec = new SecretKeySpec(kdf.getEncKey(), "AES");
        Provider provider = Security.getProvider("SunJCE");
        cipher = Cipher.getInstance("AES/CTR/NoPadding", provider);
    }


    /**
     * 对输入缓冲区的数据进行加密或解密操作，并将结果存储到输出缓冲区中。
     *
     * @param inputBuffer  包含待处理数据的输入缓冲区。
     * @param outputBuffer 用于存储处理结果的输出缓冲区。
     * @return 处理后的字节数。
     * @throws InvalidAlgorithmParameterException 如果提供的算法参数无效。
     * @throws InvalidKeyException                如果提供的密钥无效。
     * @throws ShortBufferException               如果输出缓冲区的长度不足以存储结果。
     * @throws IllegalBlockSizeException          如果数据块的大小不符合要求。
     * @throws BadPaddingException                如果数据的填充无效或不正确。
     */
    public int calculationPayload(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        int index = sentIndex;
        byte[] iv = calculationIV(index, kdf.getSaltKey());
        cipher.init(this.opmode, this.spec, new IvParameterSpec(iv));
        return cipher.doFinal(inputBuffer, outputBuffer);
    }


    /**
     * 使用指定的缓冲区加密或解密数据，并返回处理的字节数。
     *
     * @param buffer 输入和输出的共享缓冲区，包含待处理的数据。
     * @param off    数据处理起始位置的偏移量。
     * @param length 要处理的数据长度（字节数）。
     * @return 处理的字节数。
     * @throws InvalidAlgorithmParameterException 如果提供的算法参数无效。
     * @throws InvalidKeyException                如果提供的密钥无效。
     * @throws ShortBufferException               如果输出缓冲区长度不足以存储结果。
     * @throws IllegalBlockSizeException          如果处理的数据块大小无效。
     * @throws BadPaddingException                如果数据未正确填充。
     */
    public int calculationPayload(ByteBuffer buffer, int off, int length) throws InvalidAlgorithmParameterException, InvalidKeyException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        int index = sentIndex;
        byte[] iv = calculationIV(index, kdf.getSaltKey());
        cipher.init(this.opmode, this.spec, new IvParameterSpec(iv));

        ByteBuffer input = buffer.duplicate();
        ByteBuffer output = buffer.duplicate();

        input.position(off);
        input.limit(off + length);

        output.position(off);
        output.limit(off + length);
        return cipher.doFinal(input, output);
    }

    public void addSentIndex() {
        sentIndex++;
        sentIndex &= ~0x80000000;
    }

    private byte[] calculationIV(int index, byte[] saltKey) {

        byte[] iv = new byte[16];
        iv[0] = saltKey[0];
        iv[1] = saltKey[1];
        iv[2] = saltKey[2];
        iv[3] = saltKey[3];

        // The shifts transform the ssrc and index into network order
        iv[4] = (byte) (((ssrc >> 24) & 0xff) ^ saltKey[4]);
        iv[5] = (byte) (((ssrc >> 16) & 0xff) ^ saltKey[5]);
        iv[6] = (byte) (((ssrc >> 8) & 0xff) ^ saltKey[6]);
        iv[7] = (byte) ((ssrc & 0xff) ^ saltKey[7]);

        iv[8] = saltKey[8];
        iv[9] = saltKey[9];

        iv[10] = (byte) (((index >> 24) & 0xff) ^ saltKey[10]);
        iv[11] = (byte) (((index >> 16) & 0xff) ^ saltKey[11]);
        iv[12] = (byte) (((index >> 8) & 0xff) ^ saltKey[12]);
        iv[13] = (byte) ((index & 0xff) ^ saltKey[13]);

        iv[14] = iv[15] = 0;
        return iv;
    }
}
