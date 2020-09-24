package com.panda.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author camel
 * @date 2019-01-25 14:20
 */
@Data
public class ServerParamVo implements Serializable {

	private static final long serialVersionUID = 5267331270045085979L;

	private String parkId;

	private String message;
}
