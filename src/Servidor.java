import java.net.*;
import java.io.*;

public class Servidor {
   private int puerto = 12345; // Puerto por defecto
   private DatagramSocket socketServidor;
   
   String directorioActual = System.getProperty("user.dir");
   
   public void iniciarServidor() {
      try {
         socketServidor = new DatagramSocket(puerto);
         socketServidor.setReuseAddress(true);
         
         System.out.println("Servidor iniciado en el puerto " + puerto + " esperando solicitudes...");
         
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
      String mensaje = new String(datagramaRecibido.getData(), 0, datagramaRecibido.getLength());
      System.out.println("\nMensaje recibido: \u001B[33m" + mensaje + "\u001B[0m");
      // separar el código de la solicitud y el contenido
      String[] partes = mensaje.split(":");
      String codigo = partes[0];
      String contenido = partes[1];
      System.out.println("codigo = \u001B[32m" + codigo + "\u001B[0m");
      System.out.println("contenido = \u001B[34m" + contenido + "\u001B[0m");
      
      String respuesta = "";
      
      switch (codigo) {
         case "0":
            // Crear carpeta personal con el nombre del usuario (contenido)
            respuesta = crearCarpeta(codigo, contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "1":
            System.out.println("guardar archivo");
            break;
         case "2":
            System.out.println("descargar archivo");
            break;
         case "3":
            respuesta = crearCarpeta(codigo, contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "4":
            respuesta = obtenerArchivosYCarpetas(contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "5":
            System.out.println("abrir archivo o carpeta");
            break;
         case "6":
            respuesta = eliminarArchivoOCarpeta(contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "7":
            System.out.println("renombrar archivo o carpeta");
            break;
         case "8":
            System.out.println("mover archivo o carpeta");
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
   
   // Crear carpeta personal o carpetas del usuario
   // Retorna 0 si la carpeta ya existe o se crea con éxito. Retorna -1 si hay un error.
   private String crearCarpeta(String codigo, String nombreCarpeta) {
      // directorio actual
      File carpeta = new File(directorioActual + "/" + nombreCarpeta);
      
      // si la carpeta ya existe o se crea con éxito, retornar 1. Si hay un error, retornar 0
      if (codigo.equals("0")) {
         if (carpeta.exists()) {
            System.out.println("\u001B[35mAccediendo al drive de " + nombreCarpeta + "...\u001B[0m");
            return "0";
         } else if (carpeta.mkdir()) {
            System.out.println("\u001B[35mDrive de " + nombreCarpeta + " creado con éxito.\u001B[0m");
            return "1";
         } else {
            System.out.println("\u001B[35mError al crear el drive de " + nombreCarpeta + ".\u001B[0m");
            return "-1";
         }
      } else {
         if (carpeta.exists()) {
            System.out.println("\u001B[35mLa carpeta " + nombreCarpeta + " ya existe.\u001B[0m");
            return "0";
         } else if (carpeta.mkdir()) {
            System.out.println("\u001B[35mCarpeta " + nombreCarpeta + " creada con éxito.\u001B[0m");
            return "1";
         } else {
            System.out.println("\u001B[35mError al crear la carpeta " + nombreCarpeta + ".\u001B[0m");
            return "-1";
         }
      }
   }
   
   
   private String obtenerArchivosYCarpetas(String nombreCarpeta) {
      
      File directorio = new File(directorioActual + "/" + nombreCarpeta);
      
      // aunque la ruta siempre será un directorio, se verifica si es un directorio por si acaso
      if(directorio.isDirectory()) {
         File[] archivos = directorio.listFiles();
         String lista = "";
         for (File archivo : archivos) {
            if (archivo.isFile()) {
               //lista += "\u001B[32m" + archivo.getName() + "\n\u001B[0m";
               lista += archivo.getName() + "\n";
            } else if (archivo.isDirectory()) {
               //lista += "\u001B[33m" + archivo.getName() + "/\n\u001B[0m";
               lista += archivo.getName() + "/\n";
            }
         }
         System.out.println("\u001B[35mLista de archivos y carpetas en " + nombreCarpeta + " enviada con éxito.\u001B[0m");
         return lista;
      } else {
         return "\u001B[31mLa ruta especificada no es un directorio.\u001B[0m";
      }
   }
   
   private String eliminarArchivoOCarpeta(String nombreCarpeta) {
      File archivoOCarpeta = new File(directorioActual + "/" + nombreCarpeta);
      
      if (archivoOCarpeta.exists()) {
         if (eliminarRecursivamente(archivoOCarpeta)) {
            System.out.println("\u001B[35mArchivo o carpeta " + nombreCarpeta + " eliminado con éxito.\u001B[0m");
            return "0";
         } else {
            System.out.println("\u001B[35mError al eliminar el archivo o carpeta " + nombreCarpeta + ".\u001B[0m");
            return "-1";
         }
      } else {
         System.out.println("\u001B[35mEl archivo o carpeta " + nombreCarpeta + " no existe.\u001B[0m");
         return "1";
      }
   }
   
   private boolean eliminarRecursivamente(File archivoOCarpeta) {
      if (archivoOCarpeta.isDirectory()) {
         // Obtener todos los archivos y subcarpetas dentro de esta carpeta
         File[] contenido = archivoOCarpeta.listFiles();
         if (contenido != null) {
            for (File archivo : contenido) {
               // Llamar recursivamente para cada archivo o subcarpeta
               if (!eliminarRecursivamente(archivo)) {
                  return false;
               }
            }
         }
      }
      // Eliminar archivo o carpeta vacía
      return archivoOCarpeta.delete();
   }

   
   public static void main(String[] args) {
      new Servidor().iniciarServidor();
   }
}
