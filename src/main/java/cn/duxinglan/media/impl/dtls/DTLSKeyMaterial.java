 /*
  * 版权所有 (c) 2025 www.duxinglan.cn
  *
  * 项目名称：xinglanRtc
  *
  * 本文件属于 xinglanRtc 项目的一部分。
  *
  * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
  * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
  * 收费运维、收费技术支持等行为。
  *
  * 详情请参阅项目根目录下的 LICENSE 文件。
  */
 package cn.duxinglan.media.impl.dtls;


 import lombok.Getter;
 import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
 import org.bouncycastle.tls.*;
 import org.bouncycastle.tls.crypto.TlsCryptoParameters;
 import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
 import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
 import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

 import java.security.KeyPair;
 import java.security.MessageDigest;
 import java.security.PrivateKey;
 import java.security.PublicKey;
 import java.security.cert.X509Certificate;

 public class DTLSKeyMaterial {

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
         ECPrivateKeyParameters privateKeyParams = DtlsCertificateGenerator.getPrivateKeyParameters(keyPair.getPrivate());

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
