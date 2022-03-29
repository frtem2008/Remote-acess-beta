//сервер для удалённого выполнения команд админами на компах клиентов
//просто демонстрационная версия
//клиенты уже переписываются на питон


//формат данных которые получает сервер от админа и клиента
//data = A$ip на кого$id на кого$command$args - админ
//data = C$id команды$id свой$результат - клиент

//формат данных которые отправляет сервер админу и клиенту
//data = A$id клиента$command$args$success - админ
//data = C$id команды$command$args - клиент

import java.io.*;
import java.net.ServerSocket;
import java.util.*;

public class Server {
    public static int PORT = 8080;//порт сервера
    public static Scanner s = new Scanner(System.in);

    public static ArrayList<Phone> phones = new ArrayList<>();//все сокеты
    public static ArrayList<Phone> phonesA = new ArrayList<>();//админские сокеты
    public static ArrayList<Phone> phonesC = new ArrayList<>();//клиентские сокеты

    //список подключений
    public static ArrayList<String> connections = new ArrayList<>();

    public static ArrayList<Integer> idAll = new ArrayList<>();//все уникальные id

    //TODO убрать ненужные списки либо сделать их нужными
    public static ArrayList<Integer> idA = new ArrayList<>();//уникальные id админов
    public static ArrayList<Integer> idC = new ArrayList<>();//уникальные id клиентов

    //TODO оптимизировать списки (особо ничего не делают)
    public static ArrayList<String> ips = new ArrayList<>();//все ip адреса
    public static ArrayList<String> ipsA = new ArrayList<>();//ip адреса админов
    public static ArrayList<String> ipsC = new ArrayList<>();//ip адреса клиентов

    //промежуточный список запросов, которые ещё выполняются
    public static ArrayList<Request> tempRequests = new ArrayList<>();

    //файл для хранения логов
    public static File logs;

    //файл для определения пути к проекту
    public static File test = new File("test");

    //файлы для хранения запросов
    public static File tempRequestFile = new File("logs/temp req.txt");
    public static File mainRequestFile = new File("logs/fin req.txt");

    //файлы для хранения информации о пользователях
    public static File connectionsFile = new File("logs/connectionsFile.txt");
    public static File idFile = new File("logs/id.txt");

    //создание всех файлов сервера
    public static void createFiles() {
        try {
            //получение пути к файлам
            System.out.println("Attempting to create files");
            System.out.println("Creating test: " + test.createNewFile());
            String testPath = test.getAbsolutePath().replaceAll("test", "");

            //создание логового файла
            logs = new File(testPath + "/logs/");
            System.out.println("Creating logs dir: " + logs.mkdir());
            System.out.println(logs.getAbsolutePath());

            //переменные для отладки

            boolean a, b, c, d;

            if (!tempRequestFile.exists()) {
                a = tempRequestFile.createNewFile();
                if (a) {
                    System.out.println("Successfully created file tempReq");
                } else {
                    System.out.println("!Failed to create file tempReq");
                }
            }
            if (!mainRequestFile.exists()) {
                b = mainRequestFile.createNewFile();
                if (b) {
                    System.out.println("Successfully created file mainReq");
                } else {
                    System.out.println("!Failed to create file mainReq");
                }
            }
            if (!connectionsFile.exists()) {
                c = connectionsFile.createNewFile();
                if (c) {
                    System.out.println("Successfully created file mainReq");
                } else {
                    System.out.println("!Failed to create file connectionsFile");
                }
            }
            if (!idFile.exists()) {
                d = idFile.createNewFile();
                if (d) {
                    System.out.println("Successfully created file id");
                } else {
                    System.out.println("!Failed to create file id");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //чтение всех зарегистрированных до этого id из файла
    public static void fillLists() {
        String ids = readFile(idFile);
        System.out.println("Ids read from file: \n" + ids);
        String[] idSplit = ids.split("\n");
        if (idSplit[0].equals("")) {
            System.out.println("No id's to parse");
            return;
        }
        for (int i = 0; i < idSplit.length; i++) {
            addReplace(idAll, Integer.parseInt(idSplit[i].trim()));
            //idAll.add(Integer.parseInt(idSplit[i].trim()));
        }
    }

    public static void main(String[] args) {
        createFiles(); //создание необходимых файлов
        fillLists();//заполнение списков информацией из файлов

        new Thread(() -> {
            server();
        }).start();//запуск сервера

        new Thread(() -> {
            serverConsole();
        }).start();
    }

    //TODO Обработка некорректного Id

    //добавление с перезаписью для типа String
    public static void serverConsole() {
        String action;
        while (true) {
            action = s.nextLine();

            switch (action) {
                case "/shutdown":
                    System.out.println("Shutting down...");
                    //TODO shutdown actions
                    System.exit(0);
                    break;
                case "/connections":
                    refreshConnections();
                    System.out.println("All active connections: ");
                    for (int i = 0; i < connections.size(); i++) {
                        System.out.println(connections.get(i));
                    }
                    break;
                case "/idlist":
                    System.out.println("All registrated IDs: ");
                    for (int i = 0; i < idAll.size(); i++) {
                        System.out.println(idAll.get(i));
                    }
                    break;
                case "/iplist":
                    refreshConnections();
                    System.out.println("All active IPs: ");
                    for (int i = 0; i < ips.size(); i++) {
                        System.out.println(ips.get(i));
                    }
                    break;
                case "/help":
                    System.out.println("Help: ");
                    System.out.println("""
                            /help to show this
                            /shutdown to shut the server down
                            /disconnect <int id> to disconnect a client from server
                            /connections to show all active connections
                            /iplist to show all connected ips
                            /idlist to show all registrated ids
                            /msg <int id> <String message> to send a message to the client\040
                            """);
                    break;
                default:
                    if (action.matches("/disconnect[ ]*\\d*[ ]*")) {
                        if (action.split("/disconnect").length > 0) {
                            int idToDisconnect = Integer.parseInt(action.split("/disconnect ")[1]);
                            Objects.requireNonNull(getPhoneById(phones, idToDisconnect)).writeLine("DISCONNECT");
                            phones.remove(getPhoneById(phones, idToDisconnect));
                            System.out.println("Disconnected client with id " + idToDisconnect);
                        } else {
                            for (int i = 0; i < phones.size(); i++) {
                                phones.get(i).writeLine("DISCONNECT");
                            }
                            //phones.clear();
                            System.out.println("Disconnected all clients");
                        }
                    }
                    //TODO /msg
            }
        }
    }

    public static void addReplace(ArrayList<String> where, String what) {
        if (where.contains(what)) {
            where.add(where.indexOf(what), what);
        } else {
            where.add(what);
        }
    }
    //добавление с перезаписью для типа int

    public static void addReplace(ArrayList<Integer> where, int what) {
        if (where.contains(what)) {
            where.add(where.indexOf(what), what);
        } else {
            where.add(what);
        }
    }

    //добавление с перезаписью для типа Phone
    //TODO equals и hashCode для phone
    public static void addReplace(ArrayList<Phone> where, Phone what) {
        if (where.contains(what)) {
            where.add(where.indexOf(what), what);
        } else {
            where.add(what);
        }
    }


    public static void server() {
        try (ServerSocket server = new ServerSocket(PORT)) { //запуск сервера
            System.out.println("Server started");
            while (true) {
                Phone phone = new Phone(server);//создание сокета сервера и ожидание присоединения клиентов
                new Thread(() -> {//каждый клиент в отдельном потоке
                    //переменные для вывода и отправки информации

                    String data, root, ip, command, args, success, dataSent;
                    long messageId; //итендификатор сообщения (отслеживание выполнения)
                    //uniId для регистрации и авторизации
                    //aUniId для отправки данных на id администратора
                    //cUniId для отправки данных на id клиента
                    int uniId, aUniId, cUniId;

                    addReplace(phones, phone);//запись о сокетах
                    addReplace(ips, phone.getIp());//запись об ip адресах
                    //отправка инфы о подключении клиенту

                    boolean flag = false;
                    do {//пока клиент не зарегается или не войдёт
                        dataSent = phone.readLine();
                        root = dataSent.split("\\$")[0]; //Admin or Client + id
                        uniId = Integer.parseInt(dataSent.split("\\$")[1]);
                        //если id отрицательный, то регистрируем пользователя

                        if (uniId <= 0) {
                            if (idAll.contains(-uniId)) {
                                System.out.println("The user with id " + (-uniId) + " already exists");
                                phone.writeLine("Invalid id");
                                flag = true;
                            } else {
                                String register = "Successfully registrated new user with root " + root + " and id: " + (-uniId);
                                appendStrToFile(idFile, String.valueOf(-uniId));
                                System.out.println(register);
                                phone.writeLine("Registration success");
                                phone.id = -uniId;
                                addReplace(idAll, uniId);//добавление нашего id в список
                                break;
                            }
                        } else {
                            //TODO проверка id
                            if (idAll.contains(uniId)) {
                                flag = false;
                                System.out.println("User with id " + uniId + " has logged in");
                                phone.writeLine("Login success");
                                phone.id = uniId;
                            } else {
                                System.out.println("Failed to login a user with id " + uniId + ": this id is free");
                                phone.writeLine("Login failed");
                                flag = true;
                            }
                        }
                    } while (flag);


                    connections.add("Ip: " + phone.getIp() + " id: " + uniId + " root: " + root);
                    System.out.println("Client connected: ip address is " + phone.getIp() + " root is " + root + " unique id is " + Math.abs(uniId));
                    phone.writeLine("Connected to server as " + root + " with unique id " + Math.abs(uniId));
                    writeConnection(phone.getIp(), dataSent);

                    while (true) {
                        //формат данных которые получает сервер от админа и клиента
                        //data = A$ip на кого$id на кого$command$args - админ
                        //data = C$id команды$id свой$результат - клиент

                        //формат данных которые отправляет сервер админу и клиенту
                        //data = A$id клиента$command$args$success - админ
                        //data = C$id команды$command$args - клиент

                        data = phone.readLine();//считывание данных
                        System.out.println("Data read: " + data);

                        root = data.split("\\$")[0]; //информация об отправителе(админ/клиент)
                        String[] split = data.split("\\$");//чтобы каждый раз не сплитить строку

                        if (root.trim().equals("A")) {

                            //добавление информации об админе

                            aUniId = uniId;
                            String aip = phone.getIp();
                            addReplace(phonesA, phone);
                            addReplace(ipsA, aip);
                            addReplace(idA, uniId);

                            if (split[0].equals("info")) { //получение информации
                                //TODO getInfo();
                            } else {
                                System.out.println("___________________________________");

                                //получение информации о клиенте
                                //ip клиента, исполняющего команду без пробелов
                                //(надо для кривого кода на питоне, который их вставляет))))

                                ip = split[1].replaceAll(" ", "");//ip клиента
                                System.out.println("Ip to send: " + ip);

                                cUniId = Integer.parseInt(split[2]);//уникальный id клиента
                                System.out.println("Id to send: " + cUniId);
                                System.out.println("Id who sent: " + uniId);

                                command = split[3];//сама команда
                                System.out.println("Command to send: " + command);

                                args = split[4];//аргументы команды
                                System.out.println("Args to send: " + args);

                                //id запроса выставляется автоматически прямо в конструкторе
                                Request thisReq = new Request(aip, ip, aUniId, cUniId, command, args);

                                //добавление запроса в список запросов и в файл
                                tempRequests.add(thisReq);
                                writeRequest(thisReq);

                                //отправка информации нужному клиенту
                                for (int i = 0; i < idAll.size(); i++) {
                                    System.out.println("ID: " + idAll.get(i));
                                }
                                int index = idAll.indexOf(cUniId);
                                System.out.println("Index of id: " + index);
                                getPhoneById(phones, cUniId).writeLine(thisReq.id + "$" + command + "$" + args);
                                //phones.get(index).writeLine(thisReq.id + "$" + command + "$" + args);
                            }
                        } else if (root.trim().equals("C")) {
                            //добавление информации о клиенте
                            System.out.println("___________________________________");
                            int commandId = Integer.parseInt(split[1]);
                            System.out.println("Command id: " + commandId);

                            //получение команды, которую выполнял клиент, по её id
                            Request clientReq = getReqById(tempRequests, commandId);

                            ip = clientReq.ipC;//его ip адрес
                            addReplace(ipsC, ip);//запись об ip адресе клиента
                            addReplace(phonesC, phone);//запись о сокете клиента
                            System.out.println("Client ip : " + ip);

                            //получение уникального итендификатора клиента
                            cUniId = Integer.parseInt(split[2]);
                            addReplace(idC, uniId);
                            System.out.println("Client id to send: " + uniId);

                            //получение id админа, отправившего команду
                            aUniId = clientReq.idA;
                            addReplace(idA, aUniId);
                            System.out.println("Admin id to send: " + aUniId);

                            command = clientReq.cmd;//команда, которая была выполнена
                            System.out.println("Command to send: " + command);

                            args = clientReq.args;//аргументы команды
                            System.out.println("Args to send: " + args);

                            success = split[3];//успех выполнения (success/no success)
                            System.out.println("Success to send: " + success);

                            //формирование ответа
                            String response = cUniId + "$" + command + "$" + args + "$" + success;
                            //обработка запроса
                            Request done = getReqById(tempRequests, commandId);
                            if (done.equals(Request.ZEROREQUEST)) {
                                System.out.println("Client " + cUniId + " wanted to write a zeroRequest");
                            }

                            //запись запроса в файл
                            Request mainReq = new Request(done, success);
                            writeRequest(mainReq);
                            //удаление запроса из промежуточного списка
                            tempRequests.remove(done);

                            //отправка данных о клиенте админу с id aUniId

                            //int index = idAll.indexOf(aUniId);
                            //phones.get(index).writeLine(response);

                            getPhoneById(phones, aUniId).writeLine(response);
                        }

                        try {
                            Thread.sleep(10);//пауза в запросах
                        } catch (InterruptedException e) {
                            System.out.println("Client disconnected");
                        }
                    }
                }).start();
            }
        } catch (NullPointerException | IOException e) {
            System.out.println("Client disconnected");
            refreshConnections();

        }
    }

    //TODO СДЕЛАТЬ ОБНОВЛЕНИЕ БАЗЫ КЛИЕНТОВ НОРМАЛЬНО
    public static void refreshConnections() {
        System.out.println("Refreshing connections...");
        idAll.clear();
        ips.clear();
        for (int i = 0; i < phones.size(); i++) {
            phones.get(i).writeLine("msg$IDREFRESH");
            int finalI = i;
            new Thread(() -> {
                Timer t = new Timer();
                final String[] idGot = new String[1];
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        idGot[0] = phones.get(finalI).readLine();
                        if (idGot[0].matches("IDREFRESH\\$\\d*\\$\\d*.\\d*.\\d*.\\d*")) { //ЧТОООО - РЕГУЛЯРНОЕ ВЫРАЖЕНИЕ???
                            idAll.add(Integer.parseInt(idGot[0].split("\\$")[1]));
                            ips.add((idGot[0].split("\\$")[2]));
                        }
                    }
                }, 10000);


            }).start();
        }
        System.out.println("Refreshing completed!");
    }

    //получение информации админом

    /**
     * режимы:
     * 1 - получение информации о всех пользователях за всё время
     * args пустой
     * 2 - получение информации о всех пользователях за дату
     * args[0] - дата
     * 3 - получение информации о всех пользователях за период
     * args[0] - дата1; args[1] - дата2
     * 4 - получение информации о конкретном пользователе по ip
     * args[0] - ip пользователя
     * 5 - получение информации о конкретном пользователе по ip за дату (кто, команды)
     * args[0] - ip пользователя; args[1] - дата
     * 6 - получение информации о конкретном пользователе по ip за период (кто, команды)
     * args[0] - ip пользователя; args[1] - дата1; args[2] - дата2
     * 7 - получение информации о группе пользователей (все ip адреса и запросы по каждому)
     * args[0] - пользователи (all/admins/clients)
     * 8 -
     **/
    //TODO GET INFO
    public static void getInfo(int mode, String[] args) {

    }

    //получение Request из списка по уникальному итендификатору
    public static Request getReqById(ArrayList<Request> reqList, long id) {
        for (int i = 0; i < reqList.size(); i++) {
            if (reqList.get(i).id == id) {
                return reqList.get(i);
            }
        }

        return Request.ZEROREQUEST;
    }

    //получение Phone из списка по уникальному итендификатору
    public static Phone getPhoneById(ArrayList<Phone> phoneList, long id) {
        for (int i = 0; i < phoneList.size(); i++) {
            if (phoneList.get(i).id == id) {
                return phoneList.get(i);
            }
        }

        return null;
    }

    //TODO НОРМАЛЬНЫЕ ЛОГИ
    //запись запросов в файл в зависимости от содержания
    public static void writeRequest(Request req) {
        String writeReq;
        Date reqDate = new Date();
        if (req.equals(Request.ZEROREQUEST)) {
            System.out.println("A try to write a zero request into file");
        } else if (req.success.equals("NaN")) { //запись в файл со временными запросами
            writeReq = reqDate + "$" + req.id + "$" + req.ipA + "$" + req.ipC + "$" + req.cmd + "$" + req.args;
            appendStrToFile(tempRequestFile, writeReq + "\n");
        } else { //запись в файл с завершёнными запросами
            writeReq = reqDate + "$" + req.id + "$" + req.ipA + "$" + req.ipC + "$" + req.cmd + "$" + req.args + "$" + req.success;
            appendStrToFile(mainRequestFile, writeReq + "\n");
        }
    }

    //TODO НОРМАЛЬНЫЕ ЛОГИ 2
    //запись в файл о подключении пользователя
    public static void writeConnection(String ip, String root) {
        Date now = new Date();
        appendStrToFile(connectionsFile, ip + "$" + root + "$" + now);
    }

    //функции для работы с файлами
    public static void appendStrToFile(File file, String str) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
            out.write(str + "\n");
            out.close();
        } catch (IOException e) {
            System.out.println("exception occurred" + e);
        }
    }

    public static void clearFile(String fileName) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, false));
            out.write("");
            out.close();
        } catch (IOException e) {
            System.out.println("exception occurred" + e);
        }
    }

    public static String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.getPath()))) {
            String line = reader.readLine();
            ;
            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

//класс для хранения запросов
//нужен для хранения всех команд на сервере и обращения к ним по id

class Request {
    //специальный "нулевой" запрос
    public static final Request ZEROREQUEST = new Request("0", "0", 0, 0, "0", "0");

    static long count = 1;//количество запросов

    public String ipA, ipC, cmd, args, success;
    public int idA, idC;
    long id;//уникальный итендификатор

    //конструктор для временного хранения
    public Request(String ipA, String ipC, int idA, int idC, String cmd, String args) {
        this.ipA = ipA;
        this.ipC = ipC;
        this.cmd = cmd;
        this.args = args;
        this.success = "NaN";
        this.id = count;
        this.idC = idC;
        this.idA = idA;
        count++;
    }

    //конструктор для постоянного хранения
    public Request(Request what, String success) {
        this.ipA = what.ipA;
        this.ipC = what.ipC;
        this.idA = what.idA;
        this.idC = what.idC;
        this.cmd = what.cmd;
        this.args = what.args;
        this.id = what.id;

        this.success = success;
    }
}