import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas com a interface gráfica

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui 
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public String lastMessage; // última mensagem enviada
    public String nomeSala; // nome da sala

    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message + "\n");
    }

    // Construtor
    public ChatClient(String server, int port) throws IOException {
        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        socket = new Socket(server, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void newMessage(String message) throws IOException {
        if (message.startsWith("/")) {
            lastMessage = message; // Armazena a última mensagem antes de enviá-la
            out.println(message);
        } else {
            lastMessage = message.replaceFirst("^/", "//"); // Armazena a mensagem modificada
            out.println(lastMessage);
        }
    }

    private void serverMessageHandler(String message) {
        String[] response = message.split(" ");
        switch(response[0].trim()){ 
            case "ERROR": 
                String last = lastMessage.split(" ")[0];
                // apresentar o tipo de erro consoante o ultimo comando enviado
                switch(last){
                    case "/nick":
                        printMessage("ERROR: Username inválido");
                        break;
                    case "/join":
                        printMessage("ERROR: Precisas de definir um username antes de entrar numa sala");
                        break;
                    case "/leave":
                        printMessage("ERROR: Não estás numa sala");
                        break;
                    case "/priv":
                        printMessage("ERROR: Username não encontrado");
                        break;
                    default:
                        printMessage("ERROR: Comando desconhecido");
                        break;
                }
                break;
            case "OK":
                String last2 = lastMessage.split(" ")[0];
                
                // apresentar o tipo de validação consoante o último comando enviado
                switch(last2){
                    case "/nick":
                        String last3 = lastMessage.split(" ")[1]; //nick novo
                        printMessage("Username alterado para " + last3);
                        break;
                    case "/join":
                        nomeSala = lastMessage.split(" ")[1]; //nome sala
                        printMessage("Entraste na sala " + nomeSala);
                        break;
                    case "/leave":
                        printMessage("Saiste da sala " + nomeSala);
                        break;
                    case "/priv":
                        String last4 = lastMessage.split(" ")[1]; //nick destinatario
                        String last5 = "";
                        for(int i=2; i<(lastMessage.split(" ").length);i++){ //apanhar as palavras todas da mensagem enviada
                            last5 += lastMessage.split(" ")[i] + " "; //msg priv
                        }
                        printMessage("Mensagem privada enviada para " + last4 + ": " + last5);
                        break;
                    default:
                        break;
                }
                break;
            // casos em que o proprio cliente nao mandou comando
            case "MESSAGE":
                printMessage(response[1].replace("\n","") + ": " + message.split(" ",3)[2].replace("\n",""));
                break;
            case "NEWNICK":
                printMessage(response[1].replace("\n","") + " mudou de username para " + response[2].replace("\n",""));
                break;
            case "JOINED":
                printMessage(response[1].replace("\n","") + " entrou na sala");
                break;
            case "LEFT":
                printMessage(response[1].replace("\n","") + " saiu da sala");
                break;
            case "BYE":
                printMessage("A tua conexão ao servidor foi encerrada!");
                break;
            case "PRIVATE":
                printMessage(response[1].replace("\n","") + " enviou-te uma mensagem privada: " + message.split(" ",3)[2].replace("\n",""));
                break;
        }
    }

    // Método principal do objecto
    public void run() {
        new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    serverMessageHandler(response);
                }
            } catch (IOException e) {
                // Termina a conexão com o chatserver
                printMessage("Conexão perdida!");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Executar: java ChatClient <link_servidor> <porta_pretendida> !");
            return;
        }
        // Conexão aceite
        try {
            ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
            client.run();
        } 
        catch (IOException exc) {
            System.out.println("Link de servidor inválido: " + exc.getMessage());
        }
    }
}