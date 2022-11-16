package br.ufsm.csi.redes.service;


import br.ufsm.csi.redes.model.Sonda;
import br.ufsm.csi.redes.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SondaService {

    private final String nomeUsuario;
    private final Set<Usuario> listaUsuarios = new HashSet<>(); //Falta sincronizar os objetos dessa lista ou usar ao inves dela, usar a lista de dentro do objeto do chat

    public SondaService(String nomeUsuario) {
        init();
        this.nomeUsuario = nomeUsuario;
    }

    public static void main(String[] args){
        new SondaService("Marina");
    }

    private void init() {
        new Thread(new RecebeSonda()).start();
        new Thread(new EnviaSonda()).start();
        new Thread(new AtualizaLista()).start();
    }

    private class AtualizaLista implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(8000);
                List<Usuario> listRemover = new ArrayList<>();
                System.out.println("[THREAD LISTA]");
                synchronized (listaUsuarios) {
                    //varre a lista para retirar da lista os usuarios inativos
                    for (Usuario usuario : listaUsuarios) {
                        System.out.println("-->" +usuario);
                        //calcula a diferença de tempo
                        long milis = System.currentTimeMillis() - usuario.getTimeStampSonda();

                        if (milis > 30000) {

                            listRemover.add(usuario);
                        }
                    }
                    for (Usuario usuario : listRemover) {
                        listaUsuarios.remove(usuario);
                    }
                }
            }
        }
    }

    private class RecebeSonda implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket(8080);
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String pacoteStr = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);
                Sonda sonda = new ObjectMapper().readValue(pacoteStr, Sonda.class);
                System.out.println("[THREAD RECEBE] Sonda recebida: "+sonda);
                //transformar a sonda em usuario
                Usuario usuario = Usuario.builder().timeStampSonda(System.currentTimeMillis()).nome(sonda.getUsuario())
                                        .status(sonda.getStatus()).endereco(packet.getAddress()).build();

                System.out.println(usuario);
                synchronized (listaUsuarios) {
                    //Se ele já existir, remove antes de add
                    listaUsuarios.remove(usuario);
                    listaUsuarios.add(usuario);
                }
            }
        }
    }

    private class EnviaSonda implements Runnable {
        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket();
            while (true) {
                Sonda sonda = Sonda.builder().status("online").tipoMensagem("sonda").usuario(nomeUsuario).build();
                byte[] pacote = new ObjectMapper().writeValueAsString(sonda).getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(pacote, 0, pacote.length,
                                                            InetAddress.getByName("255.255.255"), 8080);
                datagramSocket.setBroadcast(true);
                System.out.println("[THREAD ENVIA] Enviou sonda");
                datagramSocket.send(packet);
                Thread.sleep(5000);
            }
        }
    }
}
