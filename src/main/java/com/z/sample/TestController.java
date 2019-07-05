package com.z.sample;

import java.io.IOException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {


    @RequestMapping("/im/push/{cid}")
    public int imPushToWeb(@PathVariable String cid,String message) {  
        try {
            ImServer.sendInfo(message,cid);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }  
        return 0;
    } 
    
    //推送数据接口
    @RequestMapping("/my/push/{userId}")
    public int pushToWeb(@PathVariable String userId,String message) {  
        try {
            MyServer.sendInfo(message,userId);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }  
        return 0;
    } 

} 
