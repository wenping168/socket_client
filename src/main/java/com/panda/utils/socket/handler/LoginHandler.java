package com.panda.utils.socket.handler;

/**
 * @author camel
 * @date 2019-01-23 22:28
 */
public interface LoginHandler {

	/**
	 * client登陆的处理函数
	 *
	 * @param parkId 用户id
	 *
	 * @return 是否验证通过
	 */
	boolean canLogin(String parkId);
}
