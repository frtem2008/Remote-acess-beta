//модуль для облегчения работы с сокетами
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Phone implements Closeable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    public int id;
    //получение ip адреса в виде строки через сокет
    public String getIp() {
        return socket.getInetAddress().getHostAddress();
    }

    //конструктор для клиента
    public Phone(String ip, int port) {
        try {
            this.socket = new Socket(ip, port);
            this.reader = createReader();
            this.writer = createWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //конструктор для сервера
    public Phone(ServerSocket server) {
        try {
            this.socket = server.accept();//ожидание клиентов
            this.reader = createReader();
            this.writer = createWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //отправка сообщения
    public void writeLine(String msg) {
        try {
            writer.write(msg);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //считывание сообщения
    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //создание потока ввода
    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    //создание потока вывода
    private BufferedWriter createWriter() throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    //чтобы можно было использовать try-catch with resources
    @Override
    public void close() throws IOException {
        writer.close();
        reader.close();
        socket.close();
    }
}
