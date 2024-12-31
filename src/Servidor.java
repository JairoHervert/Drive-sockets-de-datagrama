import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
            guardarArchivo(contenido, direccionCliente, puertoCliente);
            break;
         case "2":
            verificarEnvio(contenido, direccionCliente, puertoCliente);
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
            respuesta = abrirArchivoOCarpeta(contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "6":
            respuesta = eliminarArchivoOCarpeta(contenido);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
            break;
         case "7":
            respuesta = renombrarArchivoOCarpeta(contenido, direccionCliente, puertoCliente);
            enviarMsjACliente(respuesta, direccionCliente, puertoCliente);
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
      byte[] buffer = new byte[1400];
      DatagramPacket datagramaRecibido = new DatagramPacket(buffer, buffer.length);
      socketServidor.receive(datagramaRecibido);
      return datagramaRecibido;
   }
   
   private void guardarArchivo(String nombreArchivo, InetAddress direccionCliente, int puertoCliente) throws IOException {
      // recibimos la ruta donde se guardará el archivo
      DatagramPacket datagramaRecibido = recibirDatagrama();
      String ruta = new String(datagramaRecibido.getData(), 0, datagramaRecibido.getLength());
      String rutaCompleta = directorioActual + "/" + ruta + "/";
      
      // Recibimos el tamaño del archivo
      datagramaRecibido = recibirDatagrama();
      int numPaquetes = Integer.parseInt(new String(datagramaRecibido.getData(), 0, datagramaRecibido.getLength()));
   
      System.out.println("\n\u001B[35mGuardando archivo \"" + nombreArchivo + "\"...\u001B[0m");
      
      FileOutputStream archivoGuardado = new FileOutputStream(rutaCompleta + nombreArchivo);
      TreeMap<Integer, byte[]> fragmentosDelArchivo = new TreeMap<>();
      
      while (true) {
         datagramaRecibido = recibirDatagrama();
         byte[] datos = datagramaRecibido.getData();
         int longitudDatos = datagramaRecibido.getLength();
         
         ByteArrayInputStream bais = new ByteArrayInputStream(datos, 0, longitudDatos);
         DataInputStream dis = new DataInputStream(bais);
         
         int numSecuencia = dis.readInt();
         byte longitudNombre = dis.readByte();
         byte[] nombreArchivoBytes = new byte[longitudNombre];
         dis.readFully(nombreArchivoBytes);
         
         String nombreArchivoRecibido = new String(nombreArchivoBytes);
         byte[] datosArchivo = new byte[longitudDatos - 4 - 1 - longitudNombre];
         dis.readFully(datosArchivo);
         
         System.out.println("Fragmento recibido: " + "\u001B[33m" + numSecuencia + "\u001B[0m de \u001B[32m" + numPaquetes + "\u001B[0m");
         
         // Agregamos los fragmentos del archivo al TreeMap para ordenarlos (al final se unirán)
         fragmentosDelArchivo.put(numSecuencia, datosArchivo);
         
         // Enviar ACK
         byte[] ackData = ByteBuffer.allocate(4).putInt(numSecuencia).array();
         DatagramPacket paqueteAck = new DatagramPacket(ackData, ackData.length, direccionCliente, puertoCliente);
         socketServidor.send(paqueteAck);
         
         // Verificar si es el último paquete (se recibieron todos los fragmentos)
         if (fragmentosDelArchivo.size() == numPaquetes) {
            System.out.println("\u001B[32mArchivo guardado con éxito.\u001B[0m");
            break;
         }
      }
      
      // Unir los fragmentos del archivo
      for (Map.Entry<Integer, byte[]> fragmento : fragmentosDelArchivo.entrySet()) {
         archivoGuardado.write(fragmento.getValue());
         archivoGuardado.flush();
      }
      
      archivoGuardado.close();
   }

   
   private void verificarEnvio(String nombreArchivo, InetAddress direccionCliente, int puertoCliente) throws IOException {
      // verificar si el archivo existe
      File archivo = new File(directorioActual + "/" + nombreArchivo);
      
      if (archivo.exists()) {
         if (archivo.isDirectory()) {
            enviarMsjACliente("0", direccionCliente, puertoCliente);
            System.out.println("\u001B[32mEnviando carpeta...\u001B[0m");
            enviarCarpeta(archivo, direccionCliente, puertoCliente);
         } else {
            enviarMsjACliente("1", direccionCliente, puertoCliente);
            System.out.println("\n\u001B[32mEnviando archivo...\u001B[0m");
            enviarArchivo(archivo, direccionCliente, puertoCliente);
         }
         
      } else {
         System.out.println("\n\u001B[35mEl archivo " + nombreArchivo + " no existe.\u001B[0m");
         enviarMsjACliente("-1", direccionCliente, puertoCliente);
      }
   }
   
   private void enviarArchivo(File archivo, InetAddress direccionCliente, int puertoCliente) throws IOException {
      
      String nombreArchivo = archivo.getName();
      byte nombreLongitud = (byte) nombreArchivo.length();
      long tamArchivo = archivo.length();
      
      byte tamVentana = 5;
      int tamPaqueteDatos = 1400 - 4 - 1 - nombreLongitud;
      int numPaquetes = (int) Math.ceil((double) tamArchivo / tamPaqueteDatos);
      
      // Enviar al cliente el número de paquetes para que sepa cuantos paquetes debe recibir
      enviarMsjACliente(String.valueOf(numPaquetes), direccionCliente, puertoCliente);
      
      System.out.println("Nombre del archivo: " + "\u001B[33m" + nombreArchivo + "\u001B[0m");
      System.out.println("Tamaño del archivo: " + tamArchivo + " bytes");
      System.out.println("Número de paquetes necesarios: " + "\u001B[36m" + numPaquetes + "\u001B[0m");
      
      // Flujo de entrada para leer el archivo e ir enviando los fragmentos
      FileInputStream fisArchivo = new FileInputStream(archivo);
      
      // Variables para controlar la ventana deslizante
      int numSecuencia = 1;
      int baseDeVentana = 0;
      int acksRecibidos = 0;
      Set<Integer> manejadorACK = new HashSet<>();
      byte[] bufferPaquete = new byte[tamPaqueteDatos];
      
      while (acksRecibidos < numPaquetes) {
         int acksRecibidosEnVentana = 0;
         // BackUp de los paquetes enviados
         ByteArrayOutputStream[] backupVentana = new ByteArrayOutputStream[tamVentana];
         
         // Enviar paquetes dentro de la ventana
         int i = 0;
         while ((i < tamVentana) && (numSecuencia < numPaquetes + 1)) {
            int bytesLeidos = fisArchivo.read(bufferPaquete);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(ByteBuffer.allocate(4).putInt(numSecuencia).array());
            baos.write(nombreLongitud);
            baos.write(nombreArchivo.getBytes());
            baos.write(bufferPaquete, 0, bytesLeidos);
            
            byte[] buffer = baos.toByteArray();
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionCliente, puertoCliente);
            socketServidor.send(paquete);
            
            System.out.println("Enviado paquete " + "\u001B[33m" + numSecuencia + "\u001B[0m de \u001B[32m" + numPaquetes + "\u001B[0m");
            
            // Agregar número del paquete enviado al manejador, guardar el paquete en el backup y aumentar el número de secuencia
            manejadorACK.add(numSecuencia);
            backupVentana[i] = baos;
            numSecuencia++;
            i++;
         }
         
         System.out.println("Esperando los ACKs de los paquetes: " + "\u001B[34m" + manejadorACK + "\u001B[0m");
         
         // Esperar ACKs
         socketServidor.setSoTimeout(2000);
         
         try {
            while ((acksRecibidosEnVentana < tamVentana) && (baseDeVentana + acksRecibidosEnVentana < numPaquetes)) {
               byte[] bufferAck = new byte[4];
               DatagramPacket paqueteAck = new DatagramPacket(bufferAck, bufferAck.length);
               socketServidor.receive(paqueteAck);
               
               int ack = ByteBuffer.wrap(paqueteAck.getData()).getInt();
               System.out.println("ACK recibido: " + "\u001B[35m" + ack + "\u001B[0m");
               
               // Verificar si el ACK recibido está en el manejador y removerlo
               if (manejadorACK.contains(ack)) {
                  manejadorACK.remove(ack);
                  acksRecibidos++;
                  acksRecibidosEnVentana++;
               }
            }
            
         } catch (SocketTimeoutException e) {
            System.out.println("Tiempo de espera agotado. Reenviando paquetes...");
            
            // Reenviar paquetes cuyo ACK no se recibió (los que están en el manejador)
            for (int ack : manejadorACK) {
               ByteArrayOutputStream baos = backupVentana[ack - baseDeVentana - 1]; // restamos el 1 porque el índice empieza en 0
               byte[] buffer = baos.toByteArray();
               DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionCliente, puertoCliente);
               socketServidor.send(paquete);
               System.out.println("Reenviado paquete " + "\u001B[33m" + ack + "\u001B[0m de \u001B[32m" + numPaquetes + "\u001B[0m");
            }
         }
         baseDeVentana += acksRecibidosEnVentana;
      }
      fisArchivo.close();
      socketServidor.setSoTimeout(0);  // Desactivar el tiempo de espera
      System.out.println("\u001B[32mArchivo enviado con éxito.\u001B[0m");
   }

   private void enviarCarpeta(File carpeta, InetAddress direccionCliente, int puertoCliente) throws IOException {
      // enviar la lista de archivos y carpetas en la carpeta (se envia como String)
      File[] archivos = carpeta.listFiles();
      String lista = "";
      for (File archivo : archivos) {
         if (archivo.isDirectory()) {
            lista += archivo.getName() + "/\n";
         } else {
            lista += archivo.getName() + "\n";
         }
         System.out.println("archivo = " + archivo);
      }
      
      enviarMsjACliente(lista, direccionCliente, puertoCliente);
      
      for (File archivo : archivos) {
         if (archivo.isDirectory()) {
            enviarCarpeta(archivo, direccionCliente, puertoCliente);
         } else {
            enviarArchivo(archivo, direccionCliente, puertoCliente);
         }
      }
      
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
   
   // retorna 0 si es una carpeta, 1 si el archivo se abre con éxito (mandarlo), -1 si no existe
   private String abrirArchivoOCarpeta(String nombreCarpeta) {
      File archivoOCarpeta = new File(directorioActual + "/" + nombreCarpeta);
      
      if (archivoOCarpeta.exists()) {
         if (archivoOCarpeta.isDirectory()) {
            System.out.println("\u001B[35mAccediendo a la carpeta " + nombreCarpeta + "...\u001B[0m");
            return "0";
         } else {
            System.out.println("\u001B[35mAbriendo el archivo " + nombreCarpeta + "...\u001B[0m");
            return "1";
         }
      } else {
         System.out.println("\u001B[35mEl archivo o carpeta " + nombreCarpeta + " no existe.\u001B[0m");
         return "-1";
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

   private String renombrarArchivoOCarpeta (String nombreArchivoOCarpeta, InetAddress direccionCliente, int puertoCliente) throws IOException {
      File directorioArchivoOCarpeta = new File(directorioActual + "/" + nombreArchivoOCarpeta);
      if (directorioArchivoOCarpeta.exists()) {
         // avisar al cliente que el archivo o carpeta existe y puede ser renombrado
         enviarMsjACliente("0", direccionCliente, puertoCliente);
         System.out.println("\u001B[35mArchivo o carpeta " + nombreArchivoOCarpeta + " existe y puede ser renombrado.\u001B[0m");
         System.out.println("\u001B[35mEsperando nuevo nombre...\u001B[0m");
         
         // Recibimos el nuevo nombre del archivo o carpeta
         DatagramPacket datagramaRecibido = recibirDatagrama();
         String nuevoNombre = new String(datagramaRecibido.getData(), 0, datagramaRecibido.getLength());
         
         // ajustamos la ruta para posicionarnos en el directorio actual y solo agregar el nuevo nombre ya sea de archivo o carpeta
         String ruta = formatearRuta(directorioActual, nombreArchivoOCarpeta);
         
         File nuevoArchivoOCarpeta = null;
         if (directorioArchivoOCarpeta.isDirectory()) {
            nuevoArchivoOCarpeta = new File(ruta + nuevoNombre);
         } else {
            // si es un archivo separamos el nombre del drive y la extensión
            String[] partes = nombreArchivoOCarpeta.split("\\.");
            
            // si tiene extensión se la agregamos al nuevo nombre y si no, solo el nuevo nombre
            if (partes.length == 2) {
               // formateamos como: drive/nuevoNombre.extensión
               nuevoNombre = ruta + nuevoNombre + "." + partes[1];
            } else {
               // formateamos como: drive/nuevoNombre
               nuevoNombre = ruta + nuevoNombre;
            }
            nuevoArchivoOCarpeta = new File(directorioActual + "/" + nuevoNombre);
         }
         
         if (directorioArchivoOCarpeta.renameTo(nuevoArchivoOCarpeta)) {
            System.out.println("\u001B[35mArchivo o carpeta " + nombreArchivoOCarpeta + " renombrado a " + nuevoNombre + " con éxito.\u001B[0m");
            return "0";
         } else {
            System.out.println("\u001B[35mError al renombrar el archivo o carpeta " + nombreArchivoOCarpeta + ".\u001B[0m");
            return "-1";
         }
      } else {
         System.out.println("\u001B[35mEl archivo o carpeta " + nombreArchivoOCarpeta + " no existe.\u001B[0m");
         return "-1";
      }
   }
   
   private String formatearRuta(String directorioActual, String nombreArchivoOCarpeta) {
      // obtenemos el nombre del drive excluyendo la carpeta o archivo
      int ultimaBarra = nombreArchivoOCarpeta.lastIndexOf("/");

      if (nombreArchivoOCarpeta.length() >= ultimaBarra) {
         //System.out.println("entré al if y hare el while");
         while (nombreArchivoOCarpeta.charAt(nombreArchivoOCarpeta.length()-1) == '/') {
            nombreArchivoOCarpeta = nombreArchivoOCarpeta.substring(0, nombreArchivoOCarpeta.length()-1);
         }
      }
      
      ultimaBarra = nombreArchivoOCarpeta.lastIndexOf("/");
      nombreArchivoOCarpeta = nombreArchivoOCarpeta.substring(0, ultimaBarra);
      nombreArchivoOCarpeta = nombreArchivoOCarpeta + "/";
      return nombreArchivoOCarpeta;
   }
   
   public static void main(String[] args) {
      new Servidor().iniciarServidor();
   }
}
