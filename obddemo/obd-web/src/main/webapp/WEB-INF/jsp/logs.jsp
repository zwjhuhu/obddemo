<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@include file="/WEB-INF/jsp/common/common.jspf"%>
<body onload="test();">

	<div style="text-align:center;margin:1em 0;">
		<span>后台发送记录，显示最新20条</span><br/>
		<span id="countdown" style="color:red">将在10秒后刷新页面</span>
	</div>
	<ul style="border-top:1px solid #ccc;">
		<c:forEach items="${datas}" var="log">
			<li>
				关联设备号 ${log.deviceid }
				命令字: [${log.req}] 发送时间: ${log.updatetime}
			</li>
		</c:forEach>
	</ul>
	<script type="text/javascript">
		 function test(){
			 var count = 10;
			 var countdom = document.getElementById('countdown');
			 var timer = window.setInterval(function(){
					count--;
					countdom.innerHTML = '将在'+count+'秒后刷新页面';
					if(count==0){
						window.clearInterval(timer);
						window.location.href="logs";
					}
				}, 1000);
		 }
	</script>
</body>
</html>