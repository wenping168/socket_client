package com.panda.utils.socket.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author camel
 * @version 1.0
 * @date 2020/9/23 3:41 下午
 */
@Data
public class ClientGetParkIdVo implements Serializable {

    private static final long serialVersionUID = -2789901878539099942L;

    private Integer functionCode ;

    private String parkId ;

    private String message;

    private String time ;

    private String signature ;
}
