package com.skywin.obd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.ProtocolEntity;
import com.skywin.obd.util.CommonUtils;

public class UtilTest {
	
	@Test
	public void otherTest() {
		Map<String,String> map = new HashMap<String, String>();
		String test = "01 0C";
		map.put(test+"\r", "wtf");
		System.out.println(map.get("01 0C\r"));
	}
	
	@Test
	public void hexStrTest() {
		String str = "AT AE 12";
		byte[] buf = CommonUtils.parseByteString(str);
		System.out.println(Arrays.toString(buf));
		System.out.println(CommonUtils.toByteString(buf));
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
