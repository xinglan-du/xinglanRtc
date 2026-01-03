package cn.duxinglan.srtp;

import cn.duxinglan.media.transport.nio.webrtc.SRtpContext;
import cn.duxinglan.media.transport.nio.webrtc.SrtpContextFactory;
import cn.duxinglan.media.transport.nio.webrtc.SrtpProfilesType;
import cn.duxinglan.media.util.ByteUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 用于测试 SRTP (安全实时传输协议) 的密钥派生功能。
 * 此类依赖于 SRTP RFC 3711 的测试向量来验证实现的正确性。
 * <p>
 * 本测试类主要通过以下步骤完成测试：
 * 1. 初始化测试向量，包括主密钥、主盐值、加密密钥、加密盐值和认证密钥。
 * 2. 创建 SRTP 上下文，并设置适用的 SRTP 配置文件。
 * 3. 使用主密钥和主盐值生成加密密钥、盐值和认证密钥。
 * 4. 验证生成的密钥是否与测试向量一致。
 * <p>
 * 此类验证通过的前提是 SRTP 的密钥派生和加密/认证算法实现符合 RFC 3711 的规范。
 * <p>
 * <a href="https://datatracker.ietf.org/doc/html/rfc3711">RFC3711</a>
 * @author duzongyue
 * @version 1.0
 * @since 2025/10/28 09:09
 */
public class SrtpKeyTest {
    /* Key derivation test vectors from RFC 3711. */
    private static final byte[] masterKey128 =
            ByteUtils.hexStringToByteArray("E1F97A0D3E018BE0D64FA32C06DE4139");
    private static final byte[] masterSalt128 =
            ByteUtils.hexStringToByteArray("0EC675AD498AFEEBB6960B3AABE6");

    private static final byte[] cipherKey128 =
            ByteUtils.hexStringToByteArray("C61E7A93744F39EE10734AFE3FF7A087");
    private static final byte[] cipherSalt128 =
            ByteUtils.hexStringToByteArray("30CBBC08863D8C85D49DB34A9AE1");
    private static final byte[] authKey128 =
            ByteUtils.hexStringToByteArray("CEBE321F6FF7716B6FD4AB49AF256A156D38BAA4");


    private SrtpProfilesType srtpProfilesType = SrtpProfilesType.SRTP_AES128_CM_HMAC_SHA1_80;


    @Test
    public void srtpKdf128Test() throws Exception {
        SrtpContextFactory srtpContextFactory = new SrtpContextFactory();
        srtpContextFactory.setSrtpProfilesType(srtpProfilesType);
        srtpContextFactory.setClientCipher(masterKey128, masterSalt128);
        srtpContextFactory.setServerCipher(masterKey128, masterSalt128);
        SRtpContext clientSRtpContext = srtpContextFactory.getClientSrtpContext(1);
        SRtpContext serverSRtpContext = srtpContextFactory.getServerSrtpContext(2);

        assertArrayEquals(cipherKey128, clientSRtpContext.getKdf().getEncKey());
        assertArrayEquals(cipherSalt128, clientSRtpContext.getKdf().getSaltKey());
        assertArrayEquals(authKey128, clientSRtpContext.getKdf().getAuthKey());


        assertArrayEquals(cipherKey128, serverSRtpContext.getKdf().getEncKey());
        assertArrayEquals(cipherSalt128, serverSRtpContext.getKdf().getSaltKey());
        assertArrayEquals(authKey128, serverSRtpContext.getKdf().getAuthKey());

    }


}
