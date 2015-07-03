package com.skywin.obd;

import java.util.Arrays;

import org.junit.Test;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.ProtocolEntity;
import com.skywin.obd.util.CommonUtils;

public class UtilTest {
	@Test
	public void hexStrTest() {
		String str = "FF AE 12";
		byte[] buf = CommonUtils.parseHexString(str);
		System.out.println(Arrays.toString(buf));
		System.out.println(CommonUtils.toHexString(buf));
	}
	
	@Test
	public void checkSumTest() {
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_CLIENT);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 10);
		msg.setDestdeviceid((short) 1000);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) 2);
		System.out.println(msg.calcCheckSum());
	}

}
