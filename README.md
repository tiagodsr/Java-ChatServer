# Redes de Comunicação 2024/2025

Tiago da Silva Rodrigues, DCC/FCUP

Este projeto foi desenvolvido no âmbito da Unidade Curricular Redes de Comunicação, durante o ano letivo 2024/2025, na Faculdade de Ciências da Universidade do Porto

ChatServer e um cliente simples para comunicar com este desenvolvidos em Java

# Execução

Começa por compilar os ficheiros executando `javac ChatServer.java ChatClient.java`

Iniciar o servidor num terminal executando `java ChatServer <porta>`

Abrir outro terminal e iniciar o cliente que irá abrir um terminal executando `java ChatClient <endereço_servidor> <porta>`

Para poder interagir com outro cliente, este terá que se conectar noutra máquina num terminal, ou podes utilizar dois terminais para o efeito e simular a interação entre dois clientes.

Após se encontrar dentro servidor pode começar utilizar os comandos disponiveis, sendo que antes de poder entrar numa sala terá que definir um username que não esteja a ser utilizado no servidor

# Comandos disponiveis:

```
/nick <username>   // define o username (caso já tenha sido definido altera para o novo username)
/join <sala>   // entra na sala definida após o comando

Após entrar na sala poderás interagir com outros clientes que estejam na mesma sala podendo enviar e receber mensagens de outros clientes

/leave   // sai da sala em que se encontra
/bye   // desconect a ligação ao servidor independetemente de te encontrares numa sala ou não

/priv <username_destino> <mensagem>   // envia uma mensagem privada ao username_destino independetemente de te encontrares numa sala ou não e só este é que a irá receber

NOTA: caso tente executar algum comando indisponível irá receber feedback:  "Comando desconhecido"
```
