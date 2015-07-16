<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jsp/common/common.jspf"%>
<body>
<div style="color:red;text-align:center">点击或者扫描二维码下载</div>
	<div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
				<a href="apkdown?name=obdreader"
					style="display: block; text-align: center;"> <img
					src="img/obdreader.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">蓝牙读取</div>
		</div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
				<a href="apkdown?name=obdremote"
					style="display: block; text-align: center;"> <img
					src="img/obdremote.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">远程接收</div>
		</div>
	</div>
</body>
</html>