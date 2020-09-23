package com.panda.service;

/**
 * @author 丁许
 * @date 2019-01-25 9:49
 */
public interface SocketClientService {

	/**
	 * 开始一个socket客户端
	 *
	 * @param parkId 用户id
	 */
	void startOneClient(String parkId);

	void closeOneClient(String parkId);
}
