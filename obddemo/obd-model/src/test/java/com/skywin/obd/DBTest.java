package com.skywin.obd;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.skywin.obd.db.DbUtils;
import com.skywin.obd.model.DeviceInfo;

public class DBTest {
	@Test
	public void insertTest() {
		DeviceInfo info = new DeviceInfo();
		info.setChecktime(new Date());
		info.setName("测试");
		info.setState(1);
		DbUtils.insert(info);
	}

	@Test
	public void findByIdTest() {

		DeviceInfo model = DbUtils.findById(1, DeviceInfo.class);
		System.out.println(model);
	}

	@Test
	public void findByQueryTest() {
		List<DeviceInfo> list = DbUtils.findBYSql(
				"select * from deviceinfo where id > 1", DeviceInfo.class);
		System.out.println(list.size());
	}

	@Test
	public void deleteByIdTest() {
		DbUtils.deleteById(1, DeviceInfo.class);
	}

	@Test
	public void updateByIdTest() {
		DeviceInfo info = new DeviceInfo();
		info.setId(1);
		info.setName("修改sss");
		info.setState(2);
		info.setChecktime(new Date());
		DbUtils.updateById(info);
	}
}
