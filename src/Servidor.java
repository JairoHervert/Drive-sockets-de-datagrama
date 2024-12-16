import java.net.*;
import java.io.*;

public class Servidor {
   private int puerto = 12345; // Puerto por defecto
   private DatagramSocket socketServidor;
   
   public void iniciarServidor() {
      try {
         socketServidor = new DatagramSocket(puerto);
         socketServidor.setReuseAddress(true);
         
         System.out.println("Servidor iniciado en el puerto " + puerto + " esperando solicitudes...\n");
         
         while (true) {
            manejarSolicitud();
         }
      } catch (Exception e) {
         System.out.println("Error en el servidor: " + e.getMessage());
      }
   }
   
   private void manejarSolicitud() throws IOException {
      DatagramPacket datagramaRecibido = recibirDatagrama();
      
      // obtener dirección IP y puerto del cliente
      InetAddress direccionCliente = datagramaRecibido.getAddress();
      int puertoCliente = datagramaRecibido.getPort();
      
      // obtener mensaje del datagrama
      String mensaje = new String(datagramaRecibido.getData());
      System.out.println("Mensaje recibido: " + mensaje);
      // separar el código de la solicitud y el contenido
      String[] partes = mensaje.split(":");
      String codigo = partes[0];
      String contenido = partes[1];
      System.out.println("codigo = " + codigo);
      System.out.println("contenido = " + contenido);
      
      switch (codigo) {
         case "0":
            System.out.println("crear carpeta personal");
            break;
         case "1":
            System.out.println("guardar archivo");
            break;
         case "2":
            System.out.println("descargar archivo");
            break;
         case "3":
            System.out.println("ver archivos y carpetas");
            break;
         case "4":
            System.out.println("eliminar archivo o carpeta");
            break;
         default:
            System.out.println("Código de solicitud no válido.");
            break;
      }
      
   }
   
   private void enviarMsjACliente(String mensaje, InetAddress direccionCliente, int puertoCliente) throws IOException {
      // Implementar lógica para enviar mensaje al cliente
      byte[] buffer = mensaje.getBytes();
      DatagramPacket mensajeEnviado = new DatagramPacket(buffer, buffer.length, direccionCliente, puertoCliente);
      socketServidor.send(mensajeEnviado);
   }
   
   private DatagramPacket recibirDatagrama() throws IOException {
      byte[] buffer = new byte[1024];
      DatagramPacket datagramaRecibido = new DatagramPacket(buffer, buffer.length);
      socketServidor.receive(datagramaRecibido);
      return datagramaRecibido;
   }
   
   private int crearCarpetaPersonal(String nombreCarpeta) {
      // directorio actual
      String rutaActual = System.getProperty("user.dir");
      File carpeta = new File(rutaActual + "/" + nombreCarpeta);
      
      // si la carpeta ya existe o se crea con éxito, retornar 1. Si hay un error, retornar 0
      if (carpeta.exists()) {
         System.out.println("La carpeta ya existe.");
         return 1;
      } else if (carpeta.mkdir()) {
         System.out.println("Carpeta creada con éxito.");
         return 1;
      } else {
         System.out.println("Error al crear la carpeta.");
         return 0;
      }
   }
   
   public static void main(String[] args) {
      new Servidor().iniciarServidor();
   }
}
