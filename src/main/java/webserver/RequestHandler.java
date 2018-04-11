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
            String [] headAndBody = req.split("\n\n");
            String head = headAndBody[0];
            log.debug("{}",head);
            head.split("\\s+"); //띄어쓰기를 기준으로 분류
            String uri = head.split("\\s+")[1];
            uri= defaultPath+uri;
            ///////////////////////HTTP REQUEST 해석//////////////////////////////////////

            if(uri.split("\\?")[0].equals(defaultPath+"/user/create")){
               String param = uri.split("\\?")[1];

               Map<String,String> map = new HashMap<>();

               for(String p:param.split("&")){
                   String [] temp = p.split("=");
                    map.put(temp[0],temp[1]);
               }
               User user = new User(map.get("userId"),map.get("password"),map.get("name"),map.get("email"));
               log.info("추가된 유저 {}",user.toString());
                uri = defaultPath+"/user/list.html";
            }
            byte[] body = Files.readAllBytes(new File(uri).toPath()); //NIO를 활용한 File to byte[]body
            response200Header(dos, body.length);
            responseBody(dos, body);

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
