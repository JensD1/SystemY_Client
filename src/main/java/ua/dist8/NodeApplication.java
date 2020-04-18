package ua.dist8;

public class NodeApplication {
    public static void main(String[] args) {
        UDPListener udpListener = new UDPListener();
        TCPListener tcpListener = new TCPListener();
        udpListener.start();
        tcpListener.start();
    }
}
