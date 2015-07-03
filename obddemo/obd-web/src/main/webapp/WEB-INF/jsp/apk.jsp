<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jsp/common/common.jspf"%>
<body>
<div style="color:red;text-align:center">点击或者扫描二维码下载</div>
	<div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
				<a href="apkdown?name=obdbtsender"
					style="display: block; text-align: center;"> <img
					src="img/obdbtsender.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">蓝牙发送端</div>
		</div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
				<a href="apkdown?name=odbbtreader"
					style="display: block; text-align: center;"> <img
					src="img/odbbtreader.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">蓝牙接收端</div>
		</div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
			<a href="apkdown?name=obdclient"
					style="display: block; text-align: center;"> <img
					src="img/obdclient.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">网络发送端</div>
		</div>
		<div style="display: inline-block; width: 45%;margin-top:20px;">
			<div>
			<a href="apkdown?name=obdserver"
					style="display: block; text-align: center;"> <img
					src="img/obdserver.png" style="display: inline-block; width: 30%">
				</a>
			</div>
			<div style="text-align: center">网络接收端</div>
		</div>
	</div>
</body>
</html>