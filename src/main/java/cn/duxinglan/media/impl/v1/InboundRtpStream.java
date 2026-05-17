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
 package cn.duxinglan.media.impl.v1;


 import cn.duxinglan.media.protocol.rtcp.SenderReportRtcpPacket;
 import lombok.Data;

 @Data
 public class InboundRtpStream {

     // ================= SR / LSR / DLSR =================
     private long lastSr;              // middle 32 bits of NTP
     
     private long lastSrArrivalNs;

     private long baseRtpTs;

     private long baseNtpNs;


     private boolean hasSr = false;

     public void onSenderReport(SenderReportRtcpPacket senderReportRtcpPacket) {
         this.lastSr = ((senderReportRtcpPacket.getNtpSec() & 0xFFFF) << 16) | ((senderReportRtcpPacket.getNtpFrac() >> 16) & 0xFFFF);
         this.lastSrArrivalNs = System.nanoTime();
         long ntpSec = senderReportRtcpPacket.getNtpSec();
         long ntpFrac = senderReportRtcpPacket.getNtpFrac();
         this.baseRtpTs = senderReportRtcpPacket.getRtpTimestamp();
         this.baseNtpNs = ntpSec * 1_000_000_000L + ((ntpFrac & 0xFFFFFFFFL) * 1_000_000_000L >>> 32);
         this.hasSr = true;

     }
 }
