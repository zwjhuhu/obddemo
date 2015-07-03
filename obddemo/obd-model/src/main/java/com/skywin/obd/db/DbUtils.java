package com.skywin.obd.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.skywin.obd.exception.DataOperationError;
import com.skywin.obd.model.DBModel;
import com.skywin.obd.model.ModelMapper;

/*
 <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close"> 
 <!-- 基本属性 url、user、password -->
 <property name="url" value="${jdbc_url}" />
 <property name="username" value="${jdbc_user}" />
 <property name="password" value="${jdbc_password}" />

 <!-- 配置初始化大小、最小、最大 -->
 <property name="initialSize" value="1" />
 <property name="minIdle" value="1" /> 
 <property name="maxActive" value="20" />

 <!-- 配置获取连接等待超时的时间 -->
 <property name="maxWait" value="60000" />

 <!-- 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 -->
 <property name="timeBetweenEvictionRunsMillis" value="60000" />

 <!-- 配置一个连接在池中最小生存的时间，单位是毫秒 -->
 <property name="minEvictableIdleTimeMillis" value="300000" />

 <property name="validationQuery" value="SELECT 'x'" />
 <property name="testWhileIdle" value="true" />
 <property name="testOnBorrow" value="false" />
 <property name="testOnReturn" value="false" />

 <!-- 打开PSCache，并且指定每个连接上PSCache的大小 -->
 <property name="poolPreparedStatements" value="true" />
 <property name="maxPoolPreparedStatementPerConnectionSize" value="20" />

 <!-- 配置监控统计拦截的filters -->
 <property name="filters" value="stat" /> 
 </bean>
 */
public class DbUtils {

	private static Logger logger = LoggerFactory.getLogger(DbUtils.class);

	private DbUtils() {

	}

	private static DruidDataSource dataSource;

	private static ModelMapper modelMapper = ModelMapper.getInstance();

	private static void createDataSource() {
		InputStream in = DbUtils.class.getClassLoader().getResourceAsStream(
				"db.properties");
		if (in != null) {
			Properties props = new Properties();
			boolean inited = false;
			try {
				props.load(in);
				dataSource = new DruidDataSource();
				dataSource.setDriverClassName(props
						.getProperty("jdbc.driverclass"));
				dataSource.setUrl(props.getProperty("jdbc.url"));
				dataSource.setUsername(props.getProperty("jdbc.username"));
				dataSource.setPassword(props.getProperty("jdbc.password"));
				dataSource.setInitialSize(Integer.parseInt(props
						.getProperty("pool.initialSize")));
				dataSource.setMinIdle(Integer.parseInt(props
						.getProperty("pool.minIdle")));
				dataSource.setMaxActive(Integer.parseInt(props
						.getProperty("pool.maxActive")));

				dataSource.init();
				inited = true;
			} catch (IOException e) {
				logger.error("load db file error!");
			} catch (SQLException ex) {
				logger.error("init datasource error!", ex);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						logger.error("error", e);
					}
				}
				if (!inited) {

					if (dataSource != null) {
						dataSource.close();
					}
					dataSource = null;
				}
			}

		} else {
			logger.error("can't find db file!");
		}
	}

	static {
		createDataSource();
	}

	public static Connection getConn() {
		Connection conn = null;
		if (dataSource != null) {
			try {
				conn = dataSource.getConnection();
			} catch (SQLException e) {
				logger.error("can't get conn!", e);
				conn = null;
			}
		} else {
			throw new IllegalStateException("get conn error!");
		}
		return conn;
	}

	public static void closeConn(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("conn close error!", e);
			}
		}
	}

	public static void execUpdate(String sql) {
		Connection conn = getConn();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DataOperationError("save error", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					logger.error("error!", e);
				}
			}
			closeConn(conn);
		}
	}

	public static void execUpdate(String sql, List<Object> params) {
		Connection conn = getConn();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			int len = params.size();
			for (int i = 1; i <= len; i++) {
				ps.setObject(i, params.get(i - 1));
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DataOperationError("save error", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					logger.error("error!", e);
				}
			}
			closeConn(conn);
		}
	}

	public static void execUpdateBatch(List<String> sqls,
			List<List<Object>> paramsList) {
		Connection conn = getConn();
		PreparedStatement ps = null;
		boolean saved = true;
		try {
			int count = sqls.size();
			conn.setAutoCommit(false);
			String sql;
			List<Object> params;
			for (int i = 0; i < count; i++) {
				sql = sqls.get(i);
				params = paramsList.get(i);
				ps = conn.prepareStatement(sql);
				int len = params.size();
				for (int j = 1; j <= len; j++) {
					ps.setObject(j, params.get(j - 1));
				}
				ps.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			saved = false;
			throw new DataOperationError("save error", e);
		} finally {
			if (!saved) {
				try {
					conn.rollback();
				} catch (SQLException e) {
					logger.error("error!", e);
				}
			}
			closeConn(conn);
		}
	}

	private static Map<String,List<Object>> createInsertCmd(DBModel<?> model){
		Map<String,List<Object>> retMap = new HashMap<String, List<Object>>();
		Map<String, Method> getMap = modelMapper.findGetMap(model.getClass());
		String tabname = modelMapper.findTabName(model.getClass());
		Method getm;
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(tabname).append(" ");
		List<String> fs = new ArrayList<String>();
		List<Object> vs = new ArrayList<Object>();
		Object val = null;
		for (String key : getMap.keySet()) {
			getm = getMap.get(key);
			try {
				val = getm.invoke(model, new Object[] {});
				if (val != null) {
					fs.add(key);
					vs.add(val);
				}
			} catch (Exception e) {
				throw new DataOperationError("create insertcmd error", e);
			}
		}
		int len = fs.size();
		if (len > 0) {
			sb.append("(");
			for (int i = 0; i < len; i++) {
				sb.append(fs.get(i)).append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(") values (");
			for (int i = 0; i < len; i++) {
				sb.append("?,");
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append(")");
		}
		retMap.put(sb.toString(), vs);
		return retMap;
	}
	
	public static void insert(DBModel<?> model) {
		if (model == null) {
			throw new IllegalArgumentException("model is null!");
		}
		Map<String,List<Object>> retMap = createInsertCmd(model);
		String sql = retMap.keySet().iterator().next();
		List<Object> vs = retMap.get(sql);
		execUpdate(sql, vs);
	}
	
	public static void insertBatch(List<? extends DBModel<?>> models) {
		if (models == null||models.isEmpty()) {
			return ;
		}
		List<String> sqls = new ArrayList<String>();
		List<List<Object>> paramsList = new ArrayList<List<Object>>();
		Map<String,List<Object>> retMap;
		String sql;
		List<Object> vs;
		for(DBModel<?> model:models){
			retMap = createInsertCmd(model);
			sql = retMap.keySet().iterator().next();
			vs = retMap.get(sql);
			sqls.add(sql);
			paramsList.add(vs);
		}
		execUpdateBatch(sqls, paramsList);
	}

	public static void updateById(DBModel<?> model) {
		if (model == null) {
			throw new IllegalArgumentException("model is null!");
		}
		Map<String, Method> getMap = modelMapper.findGetMap(model.getClass());
		String tabname = modelMapper.findTabName(model.getClass());
		Method getm;
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(tabname).append(" ");
		List<String> fs = new ArrayList<String>();
		List<Object> vs = new ArrayList<Object>();
		List<String> nulls = new ArrayList<String>();
		Object val = null;
		for (String key : getMap.keySet()) {
			if ("id".equals(key)) {
				continue;
			}
			getm = getMap.get(key);
			try {
				val = getm.invoke(model, new Object[] {});
				if (val != null) {

					fs.add(key);
					vs.add(val);
				} else {
					nulls.add(key);
				}

			} catch (Exception e) {
				throw new DataOperationError("update error", e);
			}
		}
		int len = fs.size();
		sb.append("set ");
		for (int i = 0; i < len; i++) {
			sb.append(fs.get(i)).append(" = ?,");
		}

		len = nulls.size();
		if (len > 0) {
			for (int i = 0; i < len; i++) {
				sb.append(nulls.get(i)).append(" = null,");
			}

		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" where id = ").append(model.getId());
		execUpdate(sb.toString(), vs);

	}

	public static <T> List<T> execQuery(String sql, Class<T> clazz) {
		Connection conn = getConn();
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<T> retList = new ArrayList<T>();
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			T elem = null;
			Map<String, Method> setMap = modelMapper.findSetMap(clazz);
			while (rs.next()) {
				elem = findMapperEntity(rs, setMap, clazz);
				if (elem != null) {
					retList.add(elem);
				}
			}
		} catch (SQLException e) {
			throw new DataOperationError("save error", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					logger.error("error!", e);
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					logger.error("error!", e);
				}
			}
			closeConn(conn);
		}
		return retList;
	}

	private static <T> T findMapperEntity(ResultSet rs,
			Map<String, Method> setMap, Class<T> clazz) {
		T entity = null;
		try {
			entity = clazz.newInstance();
			Object val;
			for (String fname : setMap.keySet()) {
				val = rs.getObject(fname);
				if (val != null) {
					setMap.get(fname).invoke(entity, val);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return entity;
	}

	public static <T> T findById(Serializable id, Class<T> clazz) {
		if (id == null) {
			throw new IllegalArgumentException("id is null!");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(modelMapper.findTabName(clazz))
				.append(" where id = ").append(id);
		List<T> list = execQuery(sb.toString(), clazz);
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}

	public static <T> List<T> findBYSql(String sql, Class<T> clazz) {
		return execQuery(sql, clazz);
	}

	public static void deleteById(Serializable id, Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(modelMapper.findTabName(clazz))
				.append(" where id = ").append(id);
		execUpdate(sb.toString());
	}

}
