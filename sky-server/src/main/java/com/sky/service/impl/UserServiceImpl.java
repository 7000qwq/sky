package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    public static final String WX_Login = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    UserMapper userMapper;

    @Autowired
    WeChatProperties weChatProperties;

    @Override
    public User login(UserLoginDTO userLoginDTO) {

        String code = userLoginDTO.getCode();
        String openid = getOpenid(code);

        //判断是否获得了openid
        if (openid == null){
            throw new RuntimeException(MessageConstant.LOGIN_FAILED);
        }

        //判断该openid是否在用户表中
        User user = userMapper.getByOpenid(openid);

        //如果不在，自动注册
        if(user == null){

            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();

            userMapper.insert(user);
        }

        return user;
    }

    private String getOpenid(String code){
        //调用微信接口服务获得openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_Login, map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");

        return openid;
    }
}
