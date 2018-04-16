package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static String defaultPath = "./webapp";
    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            DataOutputStream dos = new DataOutputStream(out);

            ///////////////////////HTTP REQUEST 해석//////////////////////////////////////
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String  req = IOUtils.readData(reader,connection.getReceiveBufferSize());
            String [] headAndBody = req.split("\\r\\n\\r\\n"); //줄바꿈을 기준으로 분리(한줄공백)
            String httpHead = headAndBody[0];
            String httpBody ="";
            log.debug("HttpHead:::::::::::::::::::::::::{}",req);
            httpHead.split("\\s+"); //띄어쓰기를 기준으로 분류

            //todo Http Model 만들어서 해결 할 것 18.04.16
            String method = httpHead.split("\\s+")[0];
            String uri = httpHead.split("\\s+")[1];
            String httpVersion = httpHead.split("\\s+")[2];
            uri= defaultPath+uri;

            if(method.equals("POST")){
                httpBody = headAndBody[1];
            }

            ///////////////////////HTTP REQUEST 해석//////////////////////////////////////
            /*회원가입요청 /user/create */
            if(uri.split("\\?")[0].equals(defaultPath+"/user/create")){
                Map<String,String> map = null;
                //get일 경우
                if(method.equals("GET")){
                    map = getParam(uri);
                }else if(method.equals("POST")){
                    map = getParam(httpBody);
                }
                User user = new User(map.get("userId"),map.get("password"),map.get("name"),map.get("email"));
                log.info("추가된 유저 {}",user.toString());
                uri = "/user/list.html"; //완료후 LIST 페이지로
                response302Header(dos,uri);
                responseBody(dos,new byte[0]);
            }
            /*회원가입요청끝*/

            byte[] body = Files.readAllBytes(new File(uri).toPath()); //NIO를 활용한 File to byte[]body
            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private Map getParam(String paramString){
        String param = paramString;
        if(paramString.contains("?")){
            param = paramString.split("\\?")[1];
        }

        Map<String,String> map = new HashMap<>();

        for(String p:param.split("&")){
            String [] temp = p.split("=");
            map.put(temp[0],temp[1]);
        }
        return map;
    }

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("LOCATION: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
