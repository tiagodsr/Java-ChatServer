import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Map<String, ClientManager> clientes = new HashMap<>();
    private static Map<String, Set<ClientManager>> salas = new HashMap<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Executar: java ChatServer <porta_pretendida>!");
            return;
        }

        int porta = Integer.parseInt(args[0]);
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("ChatServer iniciado na porta " + porta);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientManager(clientSocket).start();
            }
        } 
        catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    private static class ClientManager extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String salaAtual;
        private String estado = "init"; // estado inicial

        public ClientManager(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    HandleClientMessage(message.trim());
                }
            } 
            catch (IOException exc) {
                System.err.println("Não foi possivel comunicar com o cliente: " + exc.getMessage());
            } 
            finally {
                disconnect_Server();
            }
        }

        private void HandleClientMessage(String message) {
            // message de input partida em partes
            String[] partes = message.split(" ", 2);
            // index 0 corresponde ao comando
            String command = partes[0];
            // se o numero de partes for maior que 1 o argument é partes[1] se nao for nao corresponde a nada ""
            String argument = partes.length > 1 ? partes[1] : "";

            switch (command) {
                case "/nick":
                    if (!argument.isEmpty()) {
                        nick_Command(argument);
                    } else {
                        out.println("ERROR");
                    }
                    break;

                case "/join":
                    if (!argument.isEmpty()) {
                        join_Command(argument);
                    } else {
                        out.println("ERROR");
                    }
                    break;

                case "/leave":
                    leave_Command();
                    break;

                case "/bye":
                    bye_Command();
                    break;

                case "/priv":
                    if (!argument.isEmpty()){
                        priv_Command(argument);
                    } else {
                        out.println("ERROR");
                    }
                default:
                    if (estado.equals("inside")) {
                        message_Room(message);
                    } else {
                        out.println("ERROR");
                    }
                    break;
            }
        }

        // manipula o comando "/priv"
        private void priv_Command(String argumento) {
            // Dividir o argumento em username do destinatário e a mensagem
            String[] partes = argumento.split(" ", 2);
            if (partes.length < 2) {
                out.println("ERROR"); // Falta a mensagem ou destinatário
                return;
            }
            
            String destinatario = partes[0];
            String mensagem = partes[1];
            synchronized (clientes) {
                ClientManager clienteDestino = clientes.get(destinatario);
                if (clienteDestino == null) {
                    out.println("ERROR"); // Username não encontrado
                } else {
                    // Enviar mensagem privada
                    clienteDestino.out.println("PRIVATE " + username + " " + mensagem);
                    out.println("OK"); // Confirmação para o remetente
                }
            }
        }

        // manipula o comando "/nick"
        private void nick_Command(String novoUsername) {
            synchronized (clientes) {
                if (clientes.containsKey(novoUsername) || novoUsername.isEmpty() || novoUsername.contains(" ")){
                    out.println("ERROR");
                } 
                else {
                    String antigoUsername = username;
                    if (antigoUsername != null) {
                        clientes.remove(antigoUsername);
                    }
                    username = novoUsername;
                    clientes.put(username, this);
                    out.println("OK");

                    if (estado.equals("init")) {
                        estado = "outside";
                    } 
                    else if (estado.equals("inside")) {
                        messageForAllinRoom("NEWNICK " + antigoUsername + " " + username, salaAtual);
                    }
                }
            }
        }

        // manipula o comando "/join"
        private void join_Command(String sala) {
            if (estado.equals("init")) {
                out.println("ERROR");
                return;
            }
            if (estado.equals("inside") && sala.equals(salaAtual)) {
                out.println("ERROR");
                return;
            }

            leave_Room();
            synchronized (salas) {
                salaAtual = sala;
                if (!salas.containsKey(sala)) {
                    salas.put(sala, new HashSet<>());
                }
                salas.get(sala).add(this);
                estado = "inside";
                out.println("OK");
                messageForAllinRoom("JOINED " + username, sala);
            }
        }

        // manipula o comando "/leave"
        private void leave_Command() {
            if (estado.equals("inside")) {
                out.println("OK");
                leave_Room();
                estado = "outside";
            } 
            else {
                out.println("ERROR");
            }
        }

        // manipula o comando "/bye"
        private void bye_Command() {
            out.println("BYE");
            disconnect_Server();
        }

        // Envia uma mensagem para todos os clientes conectados a uma sala específica
        private void messageForAllinRoom(String message, String sala) {
            Set<ClientManager> clientesSala = salas.get(sala);
            if (clientesSala != null) {
                for (ClientManager cliente : clientesSala) {
                    try {
                        cliente.out.println(message);
                    } catch (Exception e) {
                        System.err.println("Não foi possível enviar mensagem para o cliente: " + cliente.username);
                    }
                }
            }
        }

        // manipula as Mensagens quando está no estado: inside ou seja numa sala
        private void message_Room(String message) {
            if (message.isEmpty()) {
                out.println("ERROR");
                return;
            }

            String formattedMessage = message.replace("/", "//");
            messageForAllinRoom("MESSAGE " + username + " " + formattedMessage, salaAtual);
        }

        // sair da sala em que se encontra
        private void leave_Room() {
            if (salaAtual != null) {
                Set<ClientManager> clientesSala = salas.get(salaAtual);
                if (clientesSala != null) {
                    clientesSala.remove(this);
                    if (!clientesSala.isEmpty()) {
                        messageForAllinRoom("LEFT " + username, salaAtual);
                    } 
                    else {
                        salas.remove(salaAtual);
                    }
                }
                salaAtual = null;
            }
        }

        // desconecta do Servidor
        private void disconnect_Server() {
            leave_Room();
            synchronized (clientes) {
                if (username != null) clientes.remove(username);
            }
            try {
                socket.close();
            } 
            catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }
}
