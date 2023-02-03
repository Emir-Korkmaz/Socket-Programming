import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

public class ClientHandler  implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers=new ArrayList<>();
    public static ArrayList<String> names=new ArrayList<String>();
    private Socket socket;  
    private Map<String,PublicKey> keysMap;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;  
    public String clientUsername;
    
    /**
     * @param socket
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public ClientHandler(Socket socket) throws NoSuchAlgorithmException, InvalidKeySpecException{
        try {
            this.socket=socket;
            this.bufferedWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername=bufferedReader.readLine();
            String publicKeyPemStr = bufferedReader.readLine();
            String path="Uygulamanin src'sinin copyPath i yazilacak"+clientUsername;
            try {

                byte[] byte_pubkey  = Base64.getDecoder().decode(publicKeyPemStr);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");    
                PublicKey publicKey =(PublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(byte_pubkey));

                FileOutputStream keyfos = new FileOutputStream(path);
                keyfos.write(byte_pubkey);
                keyfos.close();

                try {
                    Path newPath = Paths.get(path);
                    byte[] data = Files.readAllBytes(newPath);
                    PublicKey newPublicKey =(PublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(data));
                    
                } catch (Exception e) {
                    System.out.println("Hata 10");
                }



            } catch (Exception e) {
                System.out.println("Hata 12");
            }
            
            names.add(clientUsername);
            System.out.println("clientUsername:"+clientUsername+"bitti");
            clientHandlers.add(this);
            broadcastMessageEveryBody("SERVER: "+clientUsername+" has entered the chat!");

        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }




    @Override
    public void run() {
        String messageFromClient;
        getNames();
        while (socket.isConnected()) {
            try{
                messageFromClient=bufferedReader.readLine();

                if(messageFromClient.charAt(0)=='|'){
                    String[] fullMessage=messageFromClient.split(" "); //0-> | 1->receiverName 2->userName: 3->Rsa
                    for (String string : fullMessage) {
                        System.out.println(string);
                    }
                    System.out.println("message: "+fullMessage[3]+"\n");
                    broadcastMessagePrivate(fullMessage[3], fullMessage[1]);   
                }else
                    broadcastMessageEveryBody(messageFromClient);



            }catch(IOException e){
                closeEverything(socket,bufferedReader,bufferedWriter);
                System.out.println("hata 19");
                break;
            } 
            
        }
    }



    public void broadcastMessageEveryBody(String messageToSend){
        for(ClientHandler clientHandler:clientHandlers){
            try {
                if(!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(clientUsername+":"+messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
        
    }
    public void broadcastMessagePrivate(String messageToSend,String receiverName){
        for(ClientHandler clientHandler:clientHandlers){
            try {
                if(clientHandler.clientUsername.equals(receiverName)){
                    clientHandler.bufferedWriter.write("| "+receiverName+" "+messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket,bufferedReader,bufferedWriter);
            }
        }
        
    }

    public void getNames(){
        for(ClientHandler clientHandler:clientHandlers){
        try {
            String userNames="Kullanicilar: ";
                for (String name:names) {
                    if(name.equals(clientHandler.clientUsername)){
                        continue;
                    }
                    userNames+=name+",";
                }
                if(userNames.equals("Aktif Kullanicilar: ")){
                    clientHandler.bufferedWriter.write("Aktif Kullanici yok");
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }else{
                    clientHandler.bufferedWriter.write(userNames);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
        } catch (IOException e) {
            closeEverything(socket,bufferedReader,bufferedWriter);
        }
    }
    }


    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessageEveryBody("SERVER "+clientUsername+" sohbetten ayrildi.");  
        for (String nameString : names) {
            if(nameString==clientUsername){
                names.remove(clientUsername);
            }
        }

    }
    public void userList(){
        System.out.println("yazdiriliyor.........");
        for (ClientHandler clientHandler : clientHandlers) {
                broadcastMessageEveryBody("kullanicilar"+clientHandler.clientUsername);
        }
    }
    public void closeEverything(Socket socket,BufferedReader bufferedReader,BufferedWriter bufferedWriter){
        removeClientHandler();
        try {
            if(bufferedReader!=null){
                bufferedReader.close();
            }
            if (bufferedWriter!=null) {
                bufferedWriter.close();
            }
            if(socket!=null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Keys generateKeys(String keyAlgorithm, int numBits) {
        Keys keys = null;
        try {
            // Get the public/private key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlgorithm);
            keyGen.initialize(numBits);
            KeyPair keyPair = keyGen.genKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            keys = new Keys(publicKey, privateKey);



        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception");
            System.out.println("No such algorithm: " + keyAlgorithm);
        }
        return keys;
    }
}