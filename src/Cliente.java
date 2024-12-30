import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Cliente {
   private String usuario;                      // Nombre de usuario
   //private String directorioActualReal = System.getProperty("user.dir");
   private String directorioActualUI = "";
   private int puertoServidor = 12345;          // Puerto por defecto del servidor
   private InetAddress direccionServidor;       // Dirección IP del servidor
   
   private DatagramSocket socketCliente;        // Socket del cliente
   private BufferedReader inputText = new BufferedReader(new InputStreamReader(System.in));
   
   public void iniciarCliente() {
      try {
         socketCliente = new DatagramSocket();
         direccionServidor = InetAddress.getByName("localhost"); // Usamos localhost, lo cual getByName resolverá a 127.0.0.1
         
         while (true) {
            // Ingresar nombre de usuario, si es valido y el servidor lo acepta, mostrar el menú
            if (ingresarUsuario() == 0) {
               mostrarMenu();
            }
         }
      } catch (Exception e) {
         System.out.println("Error en el cliente: " + e.getMessage());
      }
   }
   
   // se ejecuta hasta que el usuario ingrese un nombre de usuario válido
   private int ingresarUsuario() throws IOException {
      while (true) {
         System.out.println("Ingrese su nombre de usuario: ");
         usuario = inputText.readLine();
         
         // Validar que el nombre no esté vacío y que solo contenga letras, números, guiones y guiones bajos
         if (usuario == null || usuario.isEmpty() || usuario.isBlank() || !usuario.matches("[a-zA-Z0-9_-]+")) {
            System.out.println("El nombre de usuario no puede estar vacío y solo puede contener letras, números, guiones y guiones bajos.");
            continue;
         }
         // Si todas las validaciones son correctas, salir del bucle
         break;
      }
      // solicitar al servidor que cree una carpeta con el nombre del usuario
      solicitarCarpetaPersonal(usuario);
      
      //directorioActualReal += "/" + usuario;
      directorioActualUI = usuario;
      System.out.println("Bienvenido " + usuario + "!");
      return 0;
   }

   private void mostrarMenu() throws IOException {
      while (true) {
         System.out.println("\n\u001B[35m--- Menú Principal ---\u001B[0m");
         System.out.println("Directorio actual UI: " + "\u001B[36mDrive/" + directorioActualUI + "/\u001B[0m");
         System.out.println("\u001B[36m1.\u001B[0m Subir archivo");
         System.out.println("\u001B[36m2.\u001B[0m Descargar archivo");
         System.out.println("\u001B[36m3.\u001B[0m Crear carpeta");
         System.out.println("\u001B[36m4.\u001B[0m Ver archivos y carpetas");
         System.out.println("\u001B[36m5.\u001B[0m Abrir carpeta");
         System.out.println("\u001B[36m6.\u001B[0m Eliminar archivo o carpeta");
         System.out.println("\u001B[36m7.\u001B[0m Renombrar archivo o carpeta");
         System.out.println("\u001B[36m8.\u001B[0m Mover archivo o carpeta");
         System.out.println("\u001B[36m9.\u001B[0m Retroceder directorio");
         System.out.println("\u001B[36m10.\u001B[0m Salir");
         System.out.print("Selecciona una opción: ");
         
         String opcion = inputText.readLine();
         
         switch (opcion) {
            case "1":
               solicitarSubirArchivo();
               break;
            case "2":
               System.out.println("descargarArchivo");
               break;
            case "3":
               solicitudCrearCarpeta();
               break;
            case "4":
               listarArchivosYCarpetas(directorioActualUI);
               break;
            case "5":
               solicitudAbrirArchivoOCarpeta();
               break;
            case "6":
               solicitudEliminarArchivoOCarpeta();
               break;
            case "7":
               solicitudRenombrarArchivoOCarpeta();
               break;
            case "8":
               System.out.println("moveArchivoOCarpeta");
               break;
            case "9":
               retrocederDirectorio();
               break;
            case "10":
               salir();
               break;
            default:
               System.out.println("\u001B[31mOpción inválida.\u001B[0m");
               continue;
         }
      }
   }
   
   private void enviarMsjAServidor(String mensaje) throws IOException {
      byte[] buffer = mensaje.getBytes();
      DatagramPacket mensajeEnviado = new DatagramPacket(buffer, buffer.length, direccionServidor, puertoServidor);
      socketCliente.send(mensajeEnviado);
   }
   
   private String recibirMsjDeServidor() throws IOException {
      byte[] buffer = new byte[1024];
      DatagramPacket mensajeRecibido = new DatagramPacket(buffer, buffer.length);
      socketCliente.receive(mensajeRecibido);
      return new String(mensajeRecibido.getData(), 0, mensajeRecibido.getLength());
   }
   
   private void solicitarCarpetaPersonal(String nombreCarpeta) throws IOException {
      enviarMsjAServidor("0:" + nombreCarpeta);
      String respuesta = recibirMsjDeServidor();
      
      switch (respuesta) {
         case "0":
            System.out.println("\n\u001B[33mAccediendo al drive de " + usuario + "...\u001B[0m");
            break;
         case "1":
            System.out.println("\n\u001B[32mDrive de " + usuario + " creado con éxito.\u001B[0m");
            break;
         default:
            System.out.println("\n\u001B[31mError al crear el drive de " + usuario + ".\u001B[0m");
            salir();
            break;
      }
   }
   
   private void solicitarSubirArchivo() throws IOException {
      // solicitar al usuario la ruta del archivo a subir
      System.out.println("\nIngresa la ruta del archivo o carpeta que deseas subir: ");
      String rutaArchivo = inputText.readLine();
      
      // si la ruta está vacía o no es válida, mostrar un mensaje y salir
      if (rutaArchivo == null || rutaArchivo.isBlank() || !rutaArchivo.matches("[a-zA-Z0-9:._\\\\\\- /áéíóúÁÉÍÓÚñÑ]+")) {
         System.out.println("\u001B[31mLa ruta del archivo no puede estar vacía y solo puede contener letras, números, guiones, guiones bajos, diagonal, espacios y puntos.\u001B[0m");
         return;
      }
      
      // rutas para pruebas, una carpeta y un archivo
      // C:\Users\jairo\OneDrive\Escritorio\otros\backgroundDel.py
      // C:\Users\jairo\OneDrive\Escritorio\otros\perro\logo.webp
      // C:\Users\jairo\OneDrive\Imágenes\Wallpapers\1338177.png
      // C:\Users\jairo\Downloads\Redes de Computadoras.pdf
      // C:\Users\jairo\OneDrive\Documentos\ADS\Tareas\Golden Rules y Heuristicas para el  interface design\Golden_Rules_y_Heuristicas_Para_UI_Design_Hervert_Martinez_Jairo_Jesus.tex
      // C:\Users\jairo\Downloads\Proyecto_FEPI.pdf
      // C:\Users\jairo\OneDrive\Escritorio\otros\perro
      // C:\Users\jairo\OneDrive\Escritorio\otros\vacia
      
      // verificar si la ruta es válida y enviar de cierta forma el archivo o carpeta al servidor
      File archivo = new File(rutaArchivo);
      if (archivo.exists()) {
         if (archivo.isDirectory()) {
            subirCarpeta(archivo);
         } else {
            System.out.println("\n\u001B[32mSubiendo archivo...\u001B[0m");
            enviarMsjAServidor("1:" + archivo.getName());
            subirArchivo(archivo);
         }
      } else {
         System.out.println("\u001B[31mEl archivo o carpeta no existe.\u001B[0m");
      }
   }
   
   private void subirCarpeta(File archivo) throws IOException {
      // Crear en el servidor la carpeta con el nombre de la carpeta a subir
      String respaldoDirectorioActualUI = directorioActualUI;
      
      enviarMsjAServidor("3:" + directorioActualUI + "/" + archivo.getName());
      directorioActualUI += "/" + archivo.getName();
      String respuesta = recibirMsjDeServidor();
      
      // si la carpeta ya existe o se creó con éxito, comenzar a subir archivos
      if (respuesta.equals("0") || (respuesta.equals("1"))) {
         // enlista los archivos y carpetas de la carpeta a subir
         File[] archivos = archivo.listFiles();
         if (archivos != null) {
            for (File archivoEnCarpeta : archivos) {
               if (archivoEnCarpeta.isDirectory()) {
                  subirCarpeta(archivoEnCarpeta);
               } else {
                  enviarMsjAServidor("1:" + archivoEnCarpeta.getName());
                  subirArchivo(archivoEnCarpeta);
               }
            }
         }
      } else {
         System.out.println("\u001B[31mError al subir la carpeta.\u001B[0m");
      }
      directorioActualUI = respaldoDirectorioActualUI;
   }
   
   private void subirArchivo(File archivo) throws IOException {
      // enviar la ruta actual del cliente al servidor para que sepa donde guardar el archivo
      enviarMsjAServidor(directorioActualUI);
      
      String nombreArchivo = archivo.getName();
      byte nombreLongitud = (byte) nombreArchivo.length();
      long tamArchivo = archivo.length();
      
      byte tamVentana = 5;
      int tamPaqueteDatos = 1400 - 4 - 1 - nombreLongitud;
      int numPaquetes = (int) Math.ceil((double) tamArchivo / tamPaqueteDatos);
      
      // Enviar el numero de paquetes al servidor
      enviarMsjAServidor(String.valueOf(numPaquetes));
      
      System.out.println("Nombre del archivo: " + "\u001B[33m" + nombreArchivo + "\u001B[0m");
      System.out.println("Tamaño del archivo: " + tamArchivo + " bytes");
      System.out.println("Número de paquetes necesarios: " + "\u001B[36m" + numPaquetes + "\u001B[0m");
      
      FileInputStream fisArchivo = new FileInputStream(archivo);
      
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
            DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionServidor, puertoServidor);
            socketCliente.send(paquete);
            System.out.println("Enviado paquete " + "\u001B[33m" + numSecuencia + "\u001B[0m de \u001B[32m" + numPaquetes + "\u001B[0m");
            
            // Agregar número del paquete enviado al manejador, guardar el paquete en el backup y aumentar el número de secuencia
            manejadorACK.add(numSecuencia);
            backupVentana[i] = baos;
            numSecuencia++;
            i++;
         }
         
         System.out.println("Esperando los ACKs de los paquetes: " + "\u001B[34m" + manejadorACK + "\u001B[0m");
         
         // Esperar ACKs
         socketCliente.setSoTimeout(2000); // Timeout de 2 segundos
         
         try {
            while ((acksRecibidosEnVentana < tamVentana) && (baseDeVentana + acksRecibidosEnVentana < numPaquetes)) {
               byte[] bufferAck = new byte[4];
               DatagramPacket paqueteAck = new DatagramPacket(bufferAck, bufferAck.length);
               socketCliente.receive(paqueteAck);
               
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
            System.out.println("Timeout. Reenviando paquetes...");
            
            // Reenviar paquetes cuyo ACK no se recibió (los que están en el manejador)
            for (int ack : manejadorACK) {
               ByteArrayOutputStream baos = backupVentana[ack - baseDeVentana - 1]; // restamos el 1 porque el índice empieza en 0
               byte[] buffer = baos.toByteArray();
               DatagramPacket paquete = new DatagramPacket(buffer, buffer.length, direccionServidor, puertoServidor);
               socketCliente.send(paquete);
               System.out.println("Reenviado paquete " + "\u001B[33m" + ack + "\u001B[0m de \u001B[32m" + numPaquetes + "\u001B[0m");
            }
         }
         baseDeVentana += acksRecibidosEnVentana;
      }
      fisArchivo.close();
      System.out.println("\u001B[32mArchivo subido con éxito.\u001B[0m");
   }

   
   private String[] obtenerArchivosYCarpetas(String directorio) throws IOException {
      enviarMsjAServidor("4:" + directorio);
      String respuesta = recibirMsjDeServidor();
      return respuesta.split("\n");
   }
   
   private void listarArchivosYCarpetas(String directorio) throws IOException {
      String[] listaDelDirectorio = obtenerArchivosYCarpetas(directorio);
      
      if (listaDelDirectorio[0].isEmpty()) {
         System.out.println("\n\u001B[33mNo hay archivos ni carpetas en el drive de " + directorio + ".\u001B[0m");
      } else {
         System.out.println("\nArchivos y carpetas en el drive de " + directorio + ":");
         for (String archivoOCarpeta : listaDelDirectorio) {
            if (archivoOCarpeta.charAt(archivoOCarpeta.length() - 1) == '/') {
               System.out.println("\u001B[33m" + archivoOCarpeta + "\u001B[0m");
            } else{
               System.out.println("\u001B[32m" + archivoOCarpeta + "\u001B[0m");
            }
         }
      }
   }
   
   private void solicitudAbrirArchivoOCarpeta() throws IOException {
      String[] listaDelDirectorio = obtenerArchivosYCarpetas(directorioActualUI);

      // verificamos si hay archivos o carpetas para abrir, si no hay, mostramos un mensaje y salimos al menú
      if (listaDelDirectorio[0].isEmpty()) {
         System.out.println("\n\u001B[33mNo hay archivos ni carpetas en el drive de " + directorioActualUI + ".\u001B[0m");
         return;
      }
      
      listarArchivosYCarpetas(directorioActualUI);
      
      System.out.print("\nIngresa el nombre del archivo o carpeta que deseas abrir: ");
      String nombreArchivoOCarpeta = inputText.readLine();
      
      if (nombreArchivoOCarpeta == null || nombreArchivoOCarpeta.isBlank() || !nombreArchivoOCarpeta.matches("[a-zA-Z0-9._\\- /áéíóúÁÉÍÓÚñÑ]+")) {
         System.out.println("\u001B[31mEl nombre del archivo o carpeta no puede estar vacío y solo puede contener letras, números, guiones, guiones bajos, diagonal, espacios y puntos.\u001B[0m");
         return;
      } else {
         enviarMsjAServidor("5:" + directorioActualUI + "/" + nombreArchivoOCarpeta);
         String respuesta = recibirMsjDeServidor();
         
         switch (respuesta) {
            case "-1":
               System.out.println("\u001B[31mEl archivo o carpeta " + nombreArchivoOCarpeta + " no existe.\u001B[0m");
               break;
            case "0":
               if (nombreArchivoOCarpeta.charAt(nombreArchivoOCarpeta.length() - 1) == '/') {
                  directorioActualUI += "/" + nombreArchivoOCarpeta.substring(0, nombreArchivoOCarpeta.length() - 1);
               } else {
                  directorioActualUI += "/" + nombreArchivoOCarpeta;
               }
               System.out.println("\u001B[32mAbriendo la carpeta " + nombreArchivoOCarpeta + "...\u001B[0m");
               break;
            case "1":
               System.out.println("\u001B[32mAbriendo " + nombreArchivoOCarpeta + "...\u001B[0m");
               break;
            default:
               System.out.println("\u001B[31mError al abrir el archivo o carpeta " + nombreArchivoOCarpeta + ".\u001B[0m");
               break;
         }
         
      }
   }
   
   private void solicitudCrearCarpeta() throws IOException {
      System.out.println("\nInserta el nombre de la carpeta que deseas crear: ");
      String nombreNuevaCarpeta = inputText.readLine();
      
      if (nombreNuevaCarpeta == null || nombreNuevaCarpeta.isBlank() || !nombreNuevaCarpeta.matches("[a-zA-Z0-9._\\- /áéíóúÁÉÍÓÚñÑ]+")) {
         System.out.println("\u001B[31mEl nombre del archivo o carpeta no puede estar vacío y solo puede contener letras, números, guiones, guiones bajos, diagonal, espacios y puntos.\u001B[0m");
         return;
      } else {
         enviarMsjAServidor("3:" + directorioActualUI + "/" + nombreNuevaCarpeta);
         String respuesta = recibirMsjDeServidor();
         
         switch (respuesta) {
            case "0":
               System.out.println("\u001B[32mLa carpeta " + nombreNuevaCarpeta + " ya existe.\u001B[0m");
               break;
            case "1":
               System.out.println("\u001B[32mCarpeta " + nombreNuevaCarpeta + " creada con éxito.\u001B[0m");
               break;
            default:
               System.out.println("\u001B[31mError al crear la carpeta " + nombreNuevaCarpeta + ".\u001B[0m");
               break;
         }
         
      }
      
   }
   
   private  void solicitudEliminarArchivoOCarpeta() throws IOException {
      String[] listaDelDirectorio = obtenerArchivosYCarpetas(directorioActualUI);
      
      // verificamos si hay archivos o carpetas para abrir, si no hay, mostramos un mensaje y salimos al menú
      if (listaDelDirectorio[0].isEmpty()) {
         System.out.println("\n\u001B[33mNo hay archivos ni carpetas en el directorio actual.\u001B[0m");
         return;
      }
      
      // mostrar archivos y carpetas en el directorio actual
      listarArchivosYCarpetas(directorioActualUI);
      
      System.out.print("\nIngresa el nombre del archivo o carpeta que deseas eliminar: ");
      String archivoOCarpeta = inputText.readLine();
      
      if (archivoOCarpeta == null || archivoOCarpeta.isBlank() || !archivoOCarpeta.matches("[a-zA-Z0-9._\\- /áéíóúÁÉÍÓÚñÑ]+")) {
         System.out.println("\u001B[31mEl nombre del archivo o carpeta no puede estar vacío y solo puede contener letras, números, guiones, guiones bajos, diagonal, espacios y puntos.\u001B[0m");
         return;
      } else {
         enviarMsjAServidor("6:" + directorioActualUI + "/" + archivoOCarpeta);
         String respuesta = recibirMsjDeServidor();
         
         switch (respuesta) {
            case "0":
               System.out.println("\u001B[32mEl archivo o carpeta " + archivoOCarpeta + " eliminado con éxito.\u001B[0m");
               break;
            case "1":
               System.out.println("\u001B[31mEl archivo o carpeta " + archivoOCarpeta + " no existe.\u001B[0m");
               break;
            default:
               System.out.println("\u001B[31mError al eliminar el archivo o carpeta " + archivoOCarpeta + ".\u001B[0m");
               break;
         }
      }
      
   }
   
   private void solicitudRenombrarArchivoOCarpeta() throws IOException {
      String[] listaDelDirectorio = obtenerArchivosYCarpetas(directorioActualUI);
      
      // verificamos si hay archivos o carpetas, si no hay, mostramos un mensaje y salimos al menú
      if (listaDelDirectorio[0].isEmpty()) {
         System.out.println("\n\u001B[33mNo hay archivos ni carpetas en el directorio actual.\u001B[0m");
         return;
      }
      
      // mostrar archivos y carpetas en el directorio actual
      listarArchivosYCarpetas(directorioActualUI);
      
      System.out.print("\nIngresa el nombre del archivo o carpeta que deseas renombrar: ");
      String archivoOCarpeta = inputText.readLine();
      
      if (archivoOCarpeta == null || archivoOCarpeta.isBlank() || !archivoOCarpeta.matches("[a-zA-Z0-9._\\- /áéíóúÁÉÍÓÚñÑ]+")) {
         System.out.println("\u001B[31mEl nombre del archivo o carpeta no puede estar vacío y solo puede contener letras, números, guiones, guiones bajos, diagonal, espacios y puntos.\u001B[0m");
         return;
      } else {
         enviarMsjAServidor("7:" + directorioActualUI + "/" + archivoOCarpeta);
         
         // si la respuesta recibida es -1 significa que el archivo o carpeta no existe, y regresamos al menu
         String respuesta = recibirMsjDeServidor();
         
         if (respuesta.equals("-1")) {
            System.out.println("\u001B[31mEl archivo o carpeta " + archivoOCarpeta + " no existe.\u001B[0m");
            return;
         } else {
            System.out.println("\nIngresa el nuevo nombre, solo nombre, sin la extensión del archivo o la diagonal de la carpeta.");
            System.out.println("Nuevo nombre: ");
            
            String nuevoNombre = inputText.readLine();
            
            if (nuevoNombre == null || nuevoNombre.isBlank() || !nuevoNombre.matches("[a-zA-Z0-9_\\- áéíóúÁÉÍÓÚñÑ]+")) {
               System.out.println("\u001B[31mEl nuevo nombre no puede estar vacío y solo puede contener letras, números, guiones, guiones bajos y espacios.\u001B[0m");
               return;
            }
            enviarMsjAServidor(nuevoNombre);
            
            // si recibimos un 0, significa que el archivo o carpeta se pudo renombrar con éxito
            // si recibimos un -1, significa que hubo un error al renombrar el archivo o carpeta
            String pudoCambiarse = recibirMsjDeServidor();
            if (pudoCambiarse.equals("0")) {
               System.out.println("\u001B[32mEl archivo o carpeta " + archivoOCarpeta + " renombrado con éxito.\u001B[0m");
            } else {
               System.out.println("\u001B[31mError al renombrar el archivo o carpeta " + archivoOCarpeta + ".\u001B[0m");
            }
         }
      }
   }
   
   private void retrocederDirectorio() {
      if (directorioActualUI.equals(usuario)) {
         System.out.println("\u001B[31mNo puedes retroceder más.\u001B[0m");
      } else {
         directorioActualUI = directorioActualUI.substring(0, directorioActualUI.lastIndexOf('/'));
      }
   }
   
   private void salir() throws IOException {
      System.out.println("Saliendo...");
      socketCliente.close();
      inputText.close();
      System.exit(0);
   }
   
   public static void main(String[] args) {
      new Cliente().iniciarCliente();
   }
}
