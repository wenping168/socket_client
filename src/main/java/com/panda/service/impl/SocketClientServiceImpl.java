package com.panda.service.impl;

import com.panda.core.ServiceException;
import com.panda.model.ClientSocket;
import com.panda.service.SocketClientService;
import com.panda.utils.socket.client.SocketClient;
import com.panda.utils.socket.constants.SocketConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import org.springframework.util.DigestUtils;


import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * @author 丁许
 * @date 2019-01-25 9:50
 */
@Service
@Slf4j
public class SocketClientServiceImpl implements SocketClientService {

	/**
	 * 全局缓存，用于存储已存在的socket客户端连接
	 */
	public static ConcurrentMap<String, ClientSocket> existSocketClientMap = new ConcurrentHashMap<>();

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") ;


	@Resource(name = "clientTaskPool")
	private ThreadPoolTaskExecutor clientExecutor;

	@Resource(name = "clientMessageTaskPool")
	private ThreadPoolTaskExecutor messageExecutor;

	@Override
	public void startOneClient(String parkId) {
		if (existSocketClientMap.containsKey(parkId)) {
			throw new ServiceException("该用户已登陆");
		}
		//异步创建socket
		clientExecutor.execute(() -> {
			//新建一个socket连接
			SocketClient client;
			try {
				client = new SocketClient(InetAddress.getByName("127.0.0.1"), 60000);
//				client = new SocketClient(InetAddress.getByName("47.101.134.243"), 60000);
			} catch (UnknownHostException e) {
				throw new ServiceException("socket新建失败");
			}
			client.setLastOnTime(new Date());

			ScheduledExecutorService clientHeartExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "socket_client_heart_" + r.hashCode()));
			ClientSocket clientSocket = new ClientSocket(client,clientHeartExecutor);
			//登陆
			/*ClientSendDto dto = new ClientSendDto();
			dto.setFunctionCode(FunctionCodeEnum.LOGIN.getValue());
			dto.setParkId(parkId);*/
			client.println(parkId +"\r\n");
			messageExecutor.submit(() -> {
				try {
					String message;
					while ((message = client.readLine()) != null) {
						log.info("客户端:{}，获得消息：{}", parkId, message);
						/*ServerSendDto serverSendDto;
						try {
							serverSendDto = JSONObject.parseObject(message, ServerSendDto.class);
						} catch (Exception e) {
							ClientSendDto sendDto = new ClientSendDto();
							sendDto.setFunctionCode(FunctionCodeEnum.MESSAGE.getValue());
							sendDto.setMessage("data error");
							client.println(JSONObject.toJSONString(sendDto));
							break;
						}
						Integer functionCode = serverSendDto.getFunctionCode();*/
						if (message.contains("socket_server heart response")) {
							//心跳类型
							client.setLastOnTime(new Date());
						}else if (message.contains("GETPARKID 15")){
							/*ClientGetParkIdVo sendDto = new ClientGetParkIdVo();
							sendDto.setFunctionCode(FunctionCodeEnum.MESSAGE.getValue());
							sendDto.setParkId("Parking-Dongyang1");
							sendDto.setTime(simpleDateFormat.format(new Date()));
							String str = sendDto.getParkId()+sendDto.getTime() ;
							sendDto.setSignature(DigestUtils.md5DigestAsHex(str.getBytes()));*/
							String parkId2 = parkId ;
							String time = simpleDateFormat.format(new Date());
							String signature = DigestUtils.md5DigestAsHex((parkId2 + time).getBytes());
							//GETPARKID{"parkId":"1","time":"2015-11-24 10:33:58","signature":"112233afadf"}\r\n
							String res = "GETPARKID{\"parkId\":" + "\"" + parkId2 + "\",\"time\":\""  + time + "\"," + "\"signature\":" + "\"" + signature +"\"}"+"\r\n" ;
							log.info("-------"+res+"-------");
							client.println(res);
						}else if (message.startsWith("GET_CAR_POSITION")){
							String res = "GET_CAR_POSITION{\"status\":\"success\",\"plate\":\"123\",\"position\":[{\"parkName\":\"停车场名称\",\"parkSpace\":\"B039\",\"plateId\":\"浙A799VL\"},{\"parkName\":\"停车场名称\",\"parkSpace\":\"车位名称\",\"plateId\":\"A1235\"},{\"parkName\":\"停车场名称\",\"parkSpace\":\"车位名称\",\"plateId\":\"A1236\"}]}" ;
							//String res = "GET_CAR_POSITION{\"status\":\"fail\",\"plate\":\"123\",\"position\":[]}" ;
							//String res = "GET_CAR_POSITION{\"status\":\"success\",\"plate\":\"123\",\"position\":[]}" ;
							client.println(res+"\r\n");
						}
					}
				} catch (Exception e) {
					log.error("客户端异常,parkId:{},exception：{}", parkId, e.getMessage());
					client.close();
					existSocketClientMap.remove(parkId);
				}
			});
			clientHeartExecutor.scheduleWithFixedDelay(() -> {
				try {
					Date lastOnTime = client.getLastOnTime();
					long heartDuration = (new Date()).getTime() - lastOnTime.getTime();
					if (heartDuration > SocketConstant.HEART_RATE) {
						//心跳超时,关闭当前线程
						log.error("心跳超时");
						throw new Exception("服务端已断开socket");
					}
					/*ClientSendDto heartDto = new ClientSendDto();
					heartDto.setFunctionCode(FunctionCodeEnum.HEART.getValue());
					heartDto.setMessage("客户端💗💗💗");
					client.println(JSONObject.toJSONString(heartDto));*/
					client.println("USERSTATE"+"\r\n");
				} catch (Exception e) {
					log.error("客户端异常,parkId:{},exception：{}", parkId, e.getMessage());
					client.close();
					existSocketClientMap.remove(parkId);
					clientHeartExecutor.shutdown();
				}
			}, 0, 5, TimeUnit.SECONDS);
			existSocketClientMap.put(parkId, clientSocket);
		});
	}

	private String getMd5Password(
			String password, String salt) {
		// 加密规则：使用“盐+密码+盐”作为原始数据，执行5次加密
		String result = salt + password + salt;
		for (int i = 0; i < 5; i++) {
			result = DigestUtils.md5DigestAsHex(result.getBytes()).toUpperCase();
		}
		return result;
	}

	@Override
	public void closeOneClient(String parkId) {
		if (!existSocketClientMap.containsKey(parkId)) {
			throw new ServiceException("该用户未登陆，不能关闭");
		}
		ClientSocket clientSocket = existSocketClientMap.get(parkId);
		clientSocket.getClientHeartExecutor().shutdown();
		clientSocket.getSocketClient().close();
		existSocketClientMap.remove(parkId);
	}
}
