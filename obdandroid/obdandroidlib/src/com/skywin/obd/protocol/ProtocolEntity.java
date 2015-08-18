package com.skywin.obd.protocol;

import com.skywin.obd.util.CommonUtils;

public class ProtocolEntity {

	private byte side;

	private short srcdeviceid;

	private short destdeviceid;

	private byte code;

	private short sequenceid;

	private byte contentlen;

	private byte[] content;

	private short checksum;

	public byte getSide() {
		return side;
	}

	public void setSide(byte side) {
		this.side = side;
	}

	public short getSrcdeviceid() {
		return srcdeviceid;
	}

	public void setSrcdeviceid(short srcdeviceid) {
		this.srcdeviceid = srcdeviceid;
	}

	public short getDestdeviceid() {
		return destdeviceid;
	}

	public void setDestdeviceid(short destdeviceid) {
		this.destdeviceid = destdeviceid;
	}

	public byte getCode() {
		return code;
	}

	public void setCode(byte code) {
		this.code = code;
	}

	public short getSequenceid() {
		return sequenceid;
	}

	public void setSequenceid(short sequenceid) {
		this.sequenceid = sequenceid;
	}

	public byte getContentlen() {
		return contentlen;
	}

	public void setContentlen(byte contentlen) {
		this.contentlen = contentlen;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public void setChecksum(short checksum) {
		this.checksum = checksum;
	}

	public short getChecksum() {
		return checksum;
	}

	public short calcCheckSum() {
		// 计算校验和
		int sum = 0;
		sum = 0 + (0xFF & side) + (0xFFFF & srcdeviceid) / 256
				+ (0xFFFF & srcdeviceid) % 256 + (0xFFFF & destdeviceid) / 256
				+ (0xFFFF & destdeviceid) % 256 + (0xFF & code)
				+ (0xFFFF & sequenceid) / 256 + (0xFFFF & sequenceid) % 256
				+ (0xFF & contentlen);
		if (content != null && content.length > 0) {
			for (int c : content) {
				sum = sum + (0xFF & c);
			}
		}
		return (short) (0xFFFF & sum);
	}

	@Override
	public String toString() {
		return "ProtocolEntity [side=" + (side & 0xFF) + ", srcdeviceid="
				+ (srcdeviceid & 0xFFFF) + ", destdeviceid="
				+ (destdeviceid & 0xFFFF) + ", code=" + (code & 0xFF)
				+ ", sequenceid=" + (sequenceid & 0xFFFF) + ", contentlen="
				+ (contentlen & 0xFF) + ", content=" + "["
				+ CommonUtils.toByteString(content) + "]" + ", checksum="
				+ (checksum & 0xFF) + "]";
	}

}
