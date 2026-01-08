package cn.duxinglan.sdp;

import cn.duxinglan.media.signaling.sdp.SessionDescription;

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
public class SdpTest {

    private static final String SDP_STRING= """
            v=0
            o=- 7861807851227584976 3 IN IP4 127.0.0.1
            s=-
            t=0 0
            a=group:BUNDLE 0 1
            a=extmap-allow-mixed
            a=msid-semantic: WMS
            m=video 50655 UDP/TLS/RTP/SAVPF 96 97 98 99 100 101 35 36 37 38 103 104 107 108 109 114 115 116 117 118 39 40 41 42 43 44 45 46 47 48 119 120 121 122 49 50 51 52 123 124 125 53
            c=IN IP4 192.168.64.1
            a=rtcp:9 IN IP4 0.0.0.0
            a=candidate:3810978244 1 udp 2122194687 192.168.64.1 50655 typ host generation 0 network-id 1
            a=candidate:1549464807 1 udp 2122063615 10.240.1.51 65148 typ host generation 0 network-id 3 network-cost 10
            a=candidate:908032040 1 udp 2121998079 172.16.0.223 51784 typ host generation 0 network-id 5 network-cost 50
            a=candidate:1026944229 1 udp 2122265343 fd1e:c273:37cf:685f:461:27a2:489e:cdd0 63671 typ host generation 0 network-id 2
            a=candidate:1146655827 1 udp 2122131711 2408:821b:439:7f21:90a3:34b2:eb36:da2c 64258 typ host generation 0 network-id 4 network-cost 10
            a=ice-ufrag:P9wn
            a=ice-pwd:o2nwJWWXhMH3Mj1LoRdqQK/r
            a=ice-options:trickle
            a=fingerprint:sha-256 85:B2:9F:85:35:48:3F:76:31:30:E5:91:60:C9:4D:E5:0C:EF:D8:F7:44:76:AF:58:59:EC:1B:37:3E:09:3F:CA
            a=setup:actpass
            a=mid:0
            a=extmap:1 urn:ietf:params:rtp-hdrext:toffset
            a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
            a=extmap:3 urn:3gpp:video-orientation
            a=extmap:4 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
            a=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay
            a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type
            a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing
            a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/color-space
            a=extmap:9 urn:ietf:params:rtp-hdrext:sdes:mid
            a=extmap:10 urn:ietf:params:rtp-hdrext:sdes:rtp-stream-id
            a=extmap:11 urn:ietf:params:rtp-hdrext:sdes:repaired-rtp-stream-id
            a=recvonly
            a=rtcp-mux
            a=rtcp-rsize
            a=rtpmap:96 VP8/90000
            a=rtcp-fb:96 ccm fir
            a=rtcp-fb:96 nack
            a=rtcp-fb:96 nack pli
            a=rtpmap:97 rtx/90000
            a=fmtp:97 apt=96
            a=rtpmap:98 VP9/90000
            a=rtcp-fb:98 goog-remb
            a=rtcp-fb:98 transport-cc
            a=rtcp-fb:98 ccm fir
            a=rtcp-fb:98 nack
            a=rtcp-fb:98 nack pli
            a=fmtp:98 profile-id=0
            a=rtpmap:99 rtx/90000
            a=fmtp:99 apt=98
            a=rtpmap:100 VP9/90000
            a=rtcp-fb:100 goog-remb
            a=rtcp-fb:100 transport-cc
            a=rtcp-fb:100 ccm fir
            a=rtcp-fb:100 nack
            a=rtcp-fb:100 nack pli
            a=fmtp:100 profile-id=2
            a=rtpmap:101 rtx/90000
            a=fmtp:101 apt=100
            a=rtpmap:35 VP9/90000
            a=rtcp-fb:35 goog-remb
            a=rtcp-fb:35 transport-cc
            a=rtcp-fb:35 ccm fir
            a=rtcp-fb:35 nack
            a=rtcp-fb:35 nack pli
            a=fmtp:35 profile-id=1
            a=rtpmap:36 rtx/90000
            a=fmtp:36 apt=35
            a=rtpmap:37 VP9/90000
            a=rtcp-fb:37 goog-remb
            a=rtcp-fb:37 transport-cc
            a=rtcp-fb:37 ccm fir
            a=rtcp-fb:37 nack
            a=rtcp-fb:37 nack pli
            a=fmtp:37 profile-id=3
            a=rtpmap:38 rtx/90000
            a=fmtp:38 apt=37
            a=rtpmap:103 H264/90000
            a=rtcp-fb:103 goog-remb
            a=rtcp-fb:103 transport-cc
            a=rtcp-fb:103 ccm fir
            a=rtcp-fb:103 nack
            a=rtcp-fb:103 nack pli
            a=fmtp:103 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42001f
            a=rtpmap:104 rtx/90000
            a=fmtp:104 apt=103
            a=rtpmap:107 H264/90000
            a=rtcp-fb:107 goog-remb
            a=rtcp-fb:107 transport-cc
            a=rtcp-fb:107 ccm fir
            a=rtcp-fb:107 nack
            a=rtcp-fb:107 nack pli
            a=fmtp:107 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42001f
            a=rtpmap:108 rtx/90000
            a=fmtp:108 apt=107
            a=rtpmap:109 H264/90000
            a=rtcp-fb:109 goog-remb
            a=rtcp-fb:109 transport-cc
            a=rtcp-fb:109 ccm fir
            a=rtcp-fb:109 nack
            a=rtcp-fb:109 nack pli
            a=fmtp:109 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f
            a=rtpmap:114 rtx/90000
            a=fmtp:114 apt=109
            a=rtpmap:115 H264/90000
            a=rtcp-fb:115 goog-remb
            a=rtcp-fb:115 transport-cc
            a=rtcp-fb:115 ccm fir
            a=rtcp-fb:115 nack
            a=rtcp-fb:115 nack pli
            a=fmtp:115 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f
            a=rtpmap:116 rtx/90000
            a=fmtp:116 apt=115
            a=rtpmap:117 H264/90000
            a=rtcp-fb:117 goog-remb
            a=rtcp-fb:117 transport-cc
            a=rtcp-fb:117 ccm fir
            a=rtcp-fb:117 nack
            a=rtcp-fb:117 nack pli
            a=fmtp:117 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=4d001f
            a=rtpmap:118 rtx/90000
            a=fmtp:118 apt=117
            a=rtpmap:39 H264/90000
            a=rtcp-fb:39 goog-remb
            a=rtcp-fb:39 transport-cc
            a=rtcp-fb:39 ccm fir
            a=rtcp-fb:39 nack
            a=rtcp-fb:39 nack pli
            a=fmtp:39 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=4d001f
            a=rtpmap:40 rtx/90000
            a=fmtp:40 apt=39
            a=rtpmap:41 H264/90000
            a=rtcp-fb:41 goog-remb
            a=rtcp-fb:41 transport-cc
            a=rtcp-fb:41 ccm fir
            a=rtcp-fb:41 nack
            a=rtcp-fb:41 nack pli
            a=fmtp:41 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=f4001f
            a=rtpmap:42 rtx/90000
            a=fmtp:42 apt=41
            a=rtpmap:43 H264/90000
            a=rtcp-fb:43 goog-remb
            a=rtcp-fb:43 transport-cc
            a=rtcp-fb:43 ccm fir
            a=rtcp-fb:43 nack
            a=rtcp-fb:43 nack pli
            a=fmtp:43 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=f4001f
            a=rtpmap:44 rtx/90000
            a=fmtp:44 apt=43
            a=rtpmap:45 AV1/90000
            a=rtcp-fb:45 goog-remb
            a=rtcp-fb:45 transport-cc
            a=rtcp-fb:45 ccm fir
            a=rtcp-fb:45 nack
            a=rtcp-fb:45 nack pli
            a=fmtp:45 level-idx=5;profile=0;tier=0
            a=rtpmap:46 rtx/90000
            a=fmtp:46 apt=45
            a=rtpmap:47 AV1/90000
            a=rtcp-fb:47 goog-remb
            a=rtcp-fb:47 transport-cc
            a=rtcp-fb:47 ccm fir
            a=rtcp-fb:47 nack
            a=rtcp-fb:47 nack pli
            a=fmtp:47 level-idx=5;profile=1;tier=0
            a=rtpmap:48 rtx/90000
            a=fmtp:48 apt=47
            a=rtpmap:119 H264/90000
            a=rtcp-fb:119 goog-remb
            a=rtcp-fb:119 transport-cc
            a=rtcp-fb:119 ccm fir
            a=rtcp-fb:119 nack
            a=rtcp-fb:119 nack pli
            a=fmtp:119 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=64001f
            a=rtpmap:120 rtx/90000
            a=fmtp:120 apt=119
            a=rtpmap:121 H264/90000
            a=rtcp-fb:121 goog-remb
            a=rtcp-fb:121 transport-cc
            a=rtcp-fb:121 ccm fir
            a=rtcp-fb:121 nack
            a=rtcp-fb:121 nack pli
            a=fmtp:121 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=64001f
            a=rtpmap:122 rtx/90000
            a=fmtp:122 apt=121
            a=rtpmap:49 H265/90000
            a=rtcp-fb:49 goog-remb
            a=rtcp-fb:49 transport-cc
            a=rtcp-fb:49 ccm fir
            a=rtcp-fb:49 nack
            a=rtcp-fb:49 nack pli
            a=fmtp:49 level-id=180;profile-id=1;tier-flag=0;tx-mode=SRST
            a=rtpmap:50 rtx/90000
            a=fmtp:50 apt=49
            a=rtpmap:51 H265/90000
            a=rtcp-fb:51 goog-remb
            a=rtcp-fb:51 transport-cc
            a=rtcp-fb:51 ccm fir
            a=rtcp-fb:51 nack
            a=rtcp-fb:51 nack pli
            a=fmtp:51 level-id=180;profile-id=2;tier-flag=0;tx-mode=SRST
            a=rtpmap:52 rtx/90000
            a=fmtp:52 apt=51
            a=rtpmap:123 red/90000
            a=rtpmap:124 rtx/90000
            a=fmtp:124 apt=123
            a=rtpmap:125 ulpfec/90000
            a=rtpmap:53 flexfec-03/90000
            a=rtcp-fb:53 goog-remb
            a=rtcp-fb:53 transport-cc
            a=fmtp:53 repair-window=10000000
            m=video 9 UDP/TLS/RTP/SAVPF 96 97 103 104 107 108 109 114 115 116 117 118 39 40 45 46 98 99 100 101 119 120 49 50 123 124 125
            c=IN IP4 0.0.0.0
            a=rtcp:9 IN IP4 0.0.0.0
            a=ice-ufrag:P9wn
            a=ice-pwd:o2nwJWWXhMH3Mj1LoRdqQK/r
            a=ice-options:trickle
            a=fingerprint:sha-256 85:B2:9F:85:35:48:3F:76:31:30:E5:91:60:C9:4D:E5:0C:EF:D8:F7:44:76:AF:58:59:EC:1B:37:3E:09:3F:CA
            a=setup:actpass
            a=mid:1
            a=extmap:1 urn:ietf:params:rtp-hdrext:toffset
            a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
            a=extmap:3 urn:3gpp:video-orientation
            a=extmap:4 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
            a=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay
            a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type
            a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing
            a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/color-space
            a=extmap:9 urn:ietf:params:rtp-hdrext:sdes:mid
            a=extmap:10 urn:ietf:params:rtp-hdrext:sdes:rtp-stream-id
            a=extmap:11 urn:ietf:params:rtp-hdrext:sdes:repaired-rtp-stream-id
            a=sendonly
            a=msid:- de8b27bd-3b04-4feb-a8df-e4a5c802007c
            a=rtcp-mux
            a=rtcp-rsize
            a=rtpmap:96 VP8/90000
            a=rtcp-fb:96 goog-remb
            a=rtcp-fb:96 transport-cc
            a=rtcp-fb:96 ccm fir
            a=rtcp-fb:96 nack
            a=rtcp-fb:96 nack pli
            a=rtpmap:97 rtx/90000
            a=fmtp:97 apt=96
            a=rtpmap:103 H264/90000
            a=rtcp-fb:103 goog-remb
            a=rtcp-fb:103 transport-cc
            a=rtcp-fb:103 ccm fir
            a=rtcp-fb:103 nack
            a=rtcp-fb:103 nack pli
            a=fmtp:103 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42001f
            a=rtpmap:104 rtx/90000
            a=fmtp:104 apt=103
            a=rtpmap:107 H264/90000
            a=rtcp-fb:107 goog-remb
            a=rtcp-fb:107 transport-cc
            a=rtcp-fb:107 ccm fir
            a=rtcp-fb:107 nack
            a=rtcp-fb:107 nack pli
            a=fmtp:107 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42001f
            a=rtpmap:108 rtx/90000
            a=fmtp:108 apt=107
            a=rtpmap:109 H264/90000
            a=rtcp-fb:109 goog-remb
            a=rtcp-fb:109 transport-cc
            a=rtcp-fb:109 ccm fir
            a=rtcp-fb:109 nack
            a=rtcp-fb:109 nack pli
            a=fmtp:109 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f
            a=rtpmap:114 rtx/90000
            a=fmtp:114 apt=109
            a=rtpmap:115 H264/90000
            a=rtcp-fb:115 goog-remb
            a=rtcp-fb:115 transport-cc
            a=rtcp-fb:115 ccm fir
            a=rtcp-fb:115 nack
            a=rtcp-fb:115 nack pli
            a=fmtp:115 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f
            a=rtpmap:116 rtx/90000
            a=fmtp:116 apt=115
            a=rtpmap:117 H264/90000
            a=rtcp-fb:117 goog-remb
            a=rtcp-fb:117 transport-cc
            a=rtcp-fb:117 ccm fir
            a=rtcp-fb:117 nack
            a=rtcp-fb:117 nack pli
            a=fmtp:117 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=4d001f
            a=rtpmap:118 rtx/90000
            a=fmtp:118 apt=117
            a=rtpmap:39 H264/90000
            a=rtcp-fb:39 goog-remb
            a=rtcp-fb:39 transport-cc
            a=rtcp-fb:39 ccm fir
            a=rtcp-fb:39 nack
            a=rtcp-fb:39 nack pli
            a=fmtp:39 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=4d001f
            a=rtpmap:40 rtx/90000
            a=fmtp:40 apt=39
            a=rtpmap:45 AV1/90000
            a=rtcp-fb:45 goog-remb
            a=rtcp-fb:45 transport-cc
            a=rtcp-fb:45 ccm fir
            a=rtcp-fb:45 nack
            a=rtcp-fb:45 nack pli
            a=fmtp:45 level-idx=5;profile=0;tier=0
            a=rtpmap:46 rtx/90000
            a=fmtp:46 apt=45
            a=rtpmap:98 VP9/90000
            a=rtcp-fb:98 goog-remb
            a=rtcp-fb:98 transport-cc
            a=rtcp-fb:98 ccm fir
            a=rtcp-fb:98 nack
            a=rtcp-fb:98 nack pli
            a=fmtp:98 profile-id=0
            a=rtpmap:99 rtx/90000
            a=fmtp:99 apt=98
            a=rtpmap:100 VP9/90000
            a=rtcp-fb:100 goog-remb
            a=rtcp-fb:100 transport-cc
            a=rtcp-fb:100 ccm fir
            a=rtcp-fb:100 nack
            a=rtcp-fb:100 nack pli
            a=fmtp:100 profile-id=2
            a=rtpmap:101 rtx/90000
            a=fmtp:101 apt=100
            a=rtpmap:119 H264/90000
            a=rtcp-fb:119 goog-remb
            a=rtcp-fb:119 transport-cc
            a=rtcp-fb:119 ccm fir
            a=rtcp-fb:119 nack
            a=rtcp-fb:119 nack pli
            a=fmtp:119 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=64001f
            a=rtpmap:120 rtx/90000
            a=fmtp:120 apt=119
            a=rtpmap:49 H265/90000
            a=rtcp-fb:49 goog-remb
            a=rtcp-fb:49 transport-cc
            a=rtcp-fb:49 ccm fir
            a=rtcp-fb:49 nack
            a=rtcp-fb:49 nack pli
            a=fmtp:49 level-id=156;profile-id=1;tier-flag=0;tx-mode=SRST
            a=rtpmap:50 rtx/90000
            a=fmtp:50 apt=49
            a=rtpmap:123 red/90000
            a=rtpmap:124 rtx/90000
            a=fmtp:124 apt=123
            a=rtpmap:125 ulpfec/90000
            a=ssrc-group:FID 2440592327 4194085000
            a=ssrc:2440592327 cname:HA5TDv71xx5R5C0W
            a=ssrc:2440592327 msid:- de8b27bd-3b04-4feb-a8df-e4a5c802007c
            a=ssrc:4194085000 cname:HA5TDv71xx5R5C0W
            a=ssrc:4194085000 msid:- de8b27bd-3b04-4feb-a8df-e4a5c802007c
            """;

    static void main() {
        SessionDescription parse = SdpParser.parse(SDP_STRING);
        System.out.println(parse);
    }
}
