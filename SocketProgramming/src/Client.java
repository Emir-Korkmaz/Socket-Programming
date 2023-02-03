import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Cipher;

public class Client {

    public static ArrayList<Client> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String userName;
    private static PublicKey publicKey;
    private PrivateKey privateKey;

    public Client(Socket socket, String userName) {
        try {

            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.userName = userName;

            long startTime = System.currentTimeMillis();
            generateKeys();
            long endTime = System.currentTimeMillis();
            long estimatedTime = endTime - startTime;
            double seconds = (double) estimatedTime / 1000;
            System.out.println("Anahtarin olusmasi icin gecen saniye:" + seconds);

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendUserName() {
        try {
            bufferedWriter.write(userName);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            System.out.println("Username hatasi");
        }
    }

    public void publicKey() {
        try {
            byte[] byte_pubkey = publicKey.getEncoded();
            String str_key = Base64.getEncoder().encodeToString(byte_pubkey);

            bufferedWriter.write(str_key);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            System.out.println("public key hatasi");
        }
    }

    private String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public void sendMessage() throws Exception {
        try {
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                if (messageToSend.trim().equals("@cikis")) {
                    System.out.print("Mesajlamadan cikildi.");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
                if (messageToSend.trim().charAt(0) == '@') {
                    String newUserName = "";
                    newUserName = messageToSend.replace("@", "");
                    newUserName += " ";
                    String fullMessage = newUserName.substring(newUserName.indexOf(" "));
                    if (fullMessage.replaceAll("\\s", "").isEmpty()) {
                        System.out.println("Lutfen mesaj yaziniz");
                        continue;
                    }
                    String[] receiverName = newUserName.split(" ");
                    String path = "Uygulamanin src'sinin copyPath i yazilacak" + receiverName[0];
                    Path newPath = Paths.get(path);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    byte[] data = Files.readAllBytes(newPath);
                    PublicKey newPublicKey = (PublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(data));
                    bufferedWriter.write("| " + receiverName[0] + " " + userName + ":" + " "
                            + encrypt(newPublicKey, fullMessage.trim()));
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } else {
                    bufferedWriter.write(messageToSend);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public static PublicKey get(String filename)
            throws Exception {

        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);

    }

    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;
                while (socket.isConnected()) {
                    try {
                        msgFromGroupChat = bufferedReader.readLine();
                        String[] fullMessage = msgFromGroupChat.split(" ");
                        if (msgFromGroupChat.charAt(0) == '|') {
                            String newMessage = msgFromGroupChat.replace("|", "");
                            System.out.println("Ilk once sifreli mesaj:" + fullMessage[2]);
                            System.out.println(fullMessage[1] + ":" + decrypt(privateKey, fullMessage[2]));
                        }
                        else {
                            System.out.println(msgFromGroupChat);
                        }
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Isim giriniz: ");
        String userName = scanner.nextLine();
        System.out.println("\n");
        Socket socket = new Socket("localhost", 5000);
        Client client = new Client(socket, userName);

        client.sendUserName();

        displayMessage();

        client.publicKey();

        client.listenForMessage();

        try {
            client.sendMessage();

        } catch (Exception e) {
            System.out.println("Hata var");
        }

    }

    private void generateKeys() {
        GeneratePublicPrivateKeys generatePublicPrivateKeys = new GeneratePublicPrivateKeys();
        Keys keys = generatePublicPrivateKeys.generateKeys("RSA", 512);
        publicKey = keys.getPublicKey();
        System.out.println("public key:" + getHexString(publicKey.getEncoded())+"\n");
        privateKey = keys.getPrivateKey();
        System.out.println(userName+" icin public ve private key olusturuldu" + "\n");
    }

    private static String encrypt(PublicKey publicKey, String message) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(message.getBytes("UTF8"));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    private static String decrypt(PrivateKey privateKey, String encrypted) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encrypted);

        Cipher decriptCipher = Cipher.getInstance("RSA");
        decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decriptCipher.doFinal(bytes), "UTF8");
    }

    public static void displayMessage() {
        System.out.println("\nMerhaba.! Hos geldin mesajlasaya.\n");
        System.out.println("Komutlar:");
        System.out.println("1. Direkt mesaj gonderirseniz tum cevrim ici kullanicilara sifresiz olarak gidecektir.  ");
        System.out.println(
                "2. Tip '@kullanici<bosluk>mesaj' seklinde gonderirseniz yazdiginiz kullaniciya sifreli bir sekilde gidecektir\n");
                System.out.println("3. Tip '@exit' seklinde gonderirseniz cikis yapacaksiniz");
    }
}