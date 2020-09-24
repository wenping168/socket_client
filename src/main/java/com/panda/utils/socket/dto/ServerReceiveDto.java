package com.panda.utils.socket.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author camel
 * @date 2019-01-24 15:10
 */
@Data
public class ServerReceiveDto implements Serializable {

	private static final long serialVersionUID = 6600253865619639317L;

	/**
	 * 功能码 0 心跳 1 登陆 2 登出 3 发送消息
	 */
	private Integer functionCode;

	/**
	 * 用户id
	 */
	private String parkId;

	/**
	 * 这边假设是string的消息体
	 */
	private String message;

}
