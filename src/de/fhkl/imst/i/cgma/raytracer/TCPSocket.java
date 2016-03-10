package de.fhkl.imst.i.cgma.raytracer;

import java.io.*;
import java.net.*;

public class TCPSocket {
	private Socket socket;
    private BufferedReader istream;
    private BufferedWriter ostream;

    public TCPSocket(String serverAddress, int serverPort)
           throws UnknownHostException, IOException
    {
        socket = new Socket(serverAddress, serverPort);
        initializeStreams();
    }

    public TCPSocket(Socket socket) throws IOException
    {
        this.socket = socket;
        initializeStreams();
    }

    public void sendLine(String s) throws IOException
    {
        ostream.write(s);
        ostream.newLine();
        ostream.flush();
    }

    public String receiveLine() throws IOException
    {
        return istream.readLine();
    }

    public void close() throws IOException
    {
        socket.close();
    }

    private void initializeStreams() throws IOException
    {
        ostream = new BufferedWriter(
                      new OutputStreamWriter(
                          socket.getOutputStream()));
        istream = new BufferedReader(
                      new InputStreamReader(
                          socket.getInputStream()));
    }
}
