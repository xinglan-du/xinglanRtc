package cn.duxinglan.media.impl.webrtc;

import lombok.Getter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

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
public class WebRTCCertificateGenerator {

    private static final String EC_CURVE_NAME = "secp256r1";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final int CERT_VALIDITY_DAYS = 365;

    private static final BcTlsCrypto crypto = new BcTlsCrypto(new SecureRandom());

    /**
     * 生成密钥对（SECP256R1）
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_CURVE_NAME);
        keyGen.initialize(ecSpec);
        return keyGen.generateKeyPair();
    }

    /**
     * 生成自签名证书
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        return generateSelfSignedCertificate(keyPair, "CN=xinglan, O=xinglan, C=CN");
    }

    /**
     * 生成自签名证书（指定主题）
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDN) throws Exception {
        // 获取当前时间
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + CERT_VALIDITY_DAYS * 24L * 60L * 60L * 1000L);

        // 创建证书构建器
        X500Name subject = new X500Name(subjectDN);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                subject, // 颁发者（自签名所以和主题相同）
                serialNumber,
                notBefore,
                notAfter,
                subject, // 主题
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        // 添加扩展
        certificateBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false) // 非CA证书
        );

        certificateBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyAgreement)
        );

        certificateBuilder.addExtension(
                Extension.extendedKeyUsage,
                true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        );

        // 创建签名器
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(keyPair.getPrivate());

        // 生成证书
        X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    /**
     * 生成PKCS10证书请求（用于向CA申请证书）
     */
    public static PKCS10CertificationRequest generateCertificateRequest(KeyPair keyPair) throws Exception {
        return generateCertificateRequest(keyPair, "CN=WebRTC-SFU, O=WebRTC, C=US");
    }

    public static PKCS10CertificationRequest generateCertificateRequest(KeyPair keyPair, String subjectDN) throws Exception {
        X500Name subject = new X500Name(subjectDN);
        PKCS10CertificationRequestBuilder requestBuilder = new JcaPKCS10CertificationRequestBuilder(
                subject,
                keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(keyPair.getPrivate());

        return requestBuilder.build(signer);
    }

    /**
     * 将证书和私钥转换为Bouncy Castle格式（用于DTLS）
     */
    public static ECPrivateKeyParameters getPrivateKeyParameters(PrivateKey privateKey) throws Exception {
        return (ECPrivateKeyParameters) PrivateKeyFactory.createKey(privateKey.getEncoded());
    }

    public static ECPublicKeyParameters getPublicKeyParameters(PublicKey publicKey) throws Exception {
        return (ECPublicKeyParameters) PublicKeyFactory.createKey(publicKey.getEncoded());
    }

    /**
     * 生成完整的DTLS证书信息（包含密钥对和证书）
     */
    public static DTLSKeyMaterial generateDTLSKeyMaterial() throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateSelfSignedCertificate(keyPair);
        return new DTLSKeyMaterial(keyPair, certificate, crypto);
    }



    /**
     * DTLS密钥材料容器类
     */
    public static class DTLSKeyMaterial {
        @Getter
        private final KeyPair keyPair;
        @Getter
        private final X509Certificate certificate;
        @Getter
        private final BcTlsCrypto crypto;

        public DTLSKeyMaterial(KeyPair keyPair, X509Certificate certificate, BcTlsCrypto crypto) throws Exception {
            this.keyPair = keyPair;
            this.certificate = certificate;
            this.crypto = crypto;
        }

        public TlsCredentialedSigner getSignerCredentials(TlsContext context) throws Exception {
            // 1. 转换 Java X509Certificate 为 BC 的 Certificate
            org.bouncycastle.asn1.x509.Certificate asn1Cert =
                    org.bouncycastle.asn1.x509.Certificate.getInstance(certificate.getEncoded());
            BcTlsCertificate bcTlsCertificate = new BcTlsCertificate(crypto, asn1Cert);

            // 2. 封装成 TLS 层的证书链
            org.bouncycastle.tls.Certificate tlsCertChain =
                    new org.bouncycastle.tls.Certificate(new org.bouncycastle.tls.crypto.TlsCertificate[]{bcTlsCertificate});

            // 3. 定义签名算法（ECDSA + SHA256）
            SignatureAndHashAlgorithm sigAlg =
                    new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.ecdsa);


            // 5. 使用该 fakeContext 构造参数
            TlsCryptoParameters cryptoParams = new TlsCryptoParameters(context);

            // 6. 获取私钥的 BC 参数
            ECPrivateKeyParameters privateKeyParams = getPrivateKeyParameters(keyPair.getPrivate());

            // 7. 创建签名凭证
            return new BcDefaultTlsCredentialedSigner(
                    cryptoParams,
                    crypto,
                    privateKeyParams,
                    tlsCertChain,
                    sigAlg
            );
        }




        public PrivateKey getPrivateKey() {
            return keyPair.getPrivate();
        }

        public PublicKey getPublicKey() {
            return keyPair.getPublic();
        }

        /**
         * 获取证书指纹（用于SDP中的a=fingerprint）
         */
        public String getFingerprint() throws Exception {
            return getCertificateFingerprint(certificate);
        }

        /**
         * 获取证书指纹（SHA-256）
         */
        public String getCertificateFingerprint(X509Certificate certificate) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = certificate.getEncoded();
            byte[] fingerprint = md.digest(der);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fingerprint.length; i++) {
                sb.append(String.format("%02X", fingerprint[i]));
                if (i < fingerprint.length - 1) {
                    sb.append(":");
                }
            }
            return sb.toString();
        }
    }
}
