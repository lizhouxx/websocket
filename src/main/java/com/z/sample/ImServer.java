package com.z.sample;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@ServerEndpoint("/imSer/{userId}")
@Component
public class ImServer {

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //旧：concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    //private static CopyOnWriteArraySet<ImController> webSocketSet = new CopyOnWriteArraySet<ImController>();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    //新：使用map对象，便于根据userId来获取对应的WebSocket
    private static ConcurrentHashMap<String,ImServer> websocketList = new ConcurrentHashMap<>();
    //接收sid
    private String userId="";
    
    private JSONObject success(Object o)
    {
        JSONObject j = new JSONObject();
        j.put("success", o);
        return j;
    }
    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        this.session = session;
        websocketList.put(userId,this);
        System.out.println("websocketList->"+JSON.toJSONString(websocketList));
        //webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        System.out.println("有新窗口开始监听:"+userId+",当前在线人数为" + getOnlineCount());
        this.userId=userId;
        try {
            sendMessage(JSON.toJSONString(success("连接成功")));
        } catch (IOException e) {
            System.out.println("websocket IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if(websocketList.get(this.userId)!=null){
            websocketList.remove(this.userId);
            //webSocketSet.remove(this);  //从set中删除
            subOnlineCount();           //在线数减1
            System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到来自窗口"+userId+"的信息:"+message);
        if(StringUtils.isNotBlank(message)){
            JSONArray list=JSONArray.parseArray(message);
            for (int i = 0; i < list.size(); i++) {
                try {
                    //解析发送的报文
                    JSONObject object = list.getJSONObject(i);
                    String toUserId=object.getString("toUserId");
                    String contentText=object.getString("contentText");
                    object.put("fromUserId",this.userId);
                    //传送给对应用户的websocket
                    if(StringUtils.isNotBlank(toUserId)&&StringUtils.isNotBlank(contentText)){
                        ImServer socketx=websocketList.get(toUserId);
                        //需要进行转换，userId
                        if(socketx!=null){
                            socketx.sendMessage(JSON.toJSONString(success(object)));
                            //此处可以放置相关业务代码，例如存储到数据库
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 群发自定义消息
     * */
    public static void sendInfo(String message, String userId) throws IOException {
        System.out.println("推送消息到窗口"+userId+"，推送内容:"+message);
        if(StringUtils.isNotBlank(userId))
        {
            websocketList.get(userId).sendMessage(message);
        }
        else
        {

            for (Entry<String, ImServer> item : websocketList.entrySet()) {
                try {
                    item.getValue().sendMessage(message);
                } catch (IOException e) {
                }
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        ImServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        ImServer.onlineCount--;
    }
}

