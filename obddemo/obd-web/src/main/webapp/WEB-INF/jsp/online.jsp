<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jsp/common/common.jspf"%>
<body>
	<div style="text-align:center;margin:1em 0;">
		<span>在线设备列表</span>
	</div>
	<ul style="border-top:1px solid #ccc;">
		<c:forEach items="${datas}" var="device">
			<li>
				名称: ${device.name} 最后更新时间: ${device.checktime}
			</li>
		</c:forEach>
	</ul>
</body>
</html>