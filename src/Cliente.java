import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

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
         //System.out.println("Directorio actual real: " + directorioActualReal);
         System.out.println("Directorio actual UI: " + "\u001B[36mDrive/" + directorioActualUI + "/\u001B[0m");
         System.out.println("\u001B[36m1.\u001B[0m Subir archivo");
         System.out.println("\u001B[36m2.\u001B[0m Descargar archivo");
         System.out.println("\u001B[36m3.\u001B[0m Crear carpeta");
         System.out.println("\u001B[36m4.\u001B[0m Ver archivos y carpetas");
         System.out.println("\u001B[36m5.\u001B[0m Abrir archivo o carpeta");
         System.out.println("\u001B[36m6.\u001B[0m Eliminar archivo o carpeta");
         System.out.println("\u001B[36m7.\u001B[0m Renombrar archivo o carpeta");
         System.out.println("\u001B[36m8.\u001B[0m Mover archivo o carpeta");
         System.out.println("\u001B[36m9.\u001B[0m Retroceder directorio");
         System.out.println("\u001B[36m10.\u001B[0m Salir");
         System.out.print("Selecciona una opción: ");
         
         String opcion = inputText.readLine();
         
         switch (opcion) {
            case "1":
               System.out.println("subirArchivo");
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
               abrirArchivoOCarpeta();
               break;
            case "6":
               System.out.println("eliminarArchivoOCarpeta");
               break;
            case "7":
               System.out.println("renombrarArchivoOCarpeta");
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
   
   private void abrirArchivoOCarpeta() throws IOException {
      String[] listaDelDirectorio = obtenerArchivosYCarpetas(directorioActualUI);

      // verificamos si hay archivos o carpetas para abrir, si no hay, mostramos un mensaje y salimos al menú
      if (listaDelDirectorio[0].isEmpty()) {
         System.out.println("\n\u001B[33mNo hay archivos ni carpetas en el drive de " + directorioActualUI + ".\u001B[0m");
         return;
      }
      
      listarArchivosYCarpetas(directorioActualUI);
      
      System.out.print("\nIngresa el nombre del archivo o carpeta que deseas abrir: ");
      String archivoOCarpeta = inputText.readLine();
      
      if (archivoOCarpeta.isEmpty() || archivoOCarpeta.isBlank()) {
         System.out.println("\u001B[31mEl nombre del archivo o carpeta no puede estar vacío.\u001B[0m");
         return;
      }
      
      boolean existe = false;
      for (String item : listaDelDirectorio) {
         if (item.equals(archivoOCarpeta)) {
            existe = true;
            break;
         }
      }
      
      if (!existe) {
         System.out.println("\u001B[31mEl archivo o carpeta no existe.\u001B[0m");
         return;
      } else {
         // si es una carpeta, actualizar directorio actual. Si es un archivo, abrirlo
         if (archivoOCarpeta.charAt(archivoOCarpeta.length() - 1) == '/') {
            directorioActualUI += "/" + archivoOCarpeta.substring(0, archivoOCarpeta.length() - 1);
         } else {
            System.out.println("\u001B[34mAbriendo " + archivoOCarpeta + "...\u001B[0m");
            // recibirlo como archivo sin guardarlo y abrirlo
         }
      }
   }
   
   private void solicitudCrearCarpeta() throws IOException {
      System.out.println("\nInserta el nombre de la carpeta que deseas crear: ");
      String nombreNuevaCarpeta = inputText.readLine();
      
      if (nombreNuevaCarpeta == null || nombreNuevaCarpeta.isEmpty() || nombreNuevaCarpeta.isBlank() || !nombreNuevaCarpeta.matches("[a-zA-Z0-9_-]+")) {
         System.out.println("\u001B[31mEl nombre de la carpeta no puede estar vacío y solo puede contener letras, números, guiones y guiones bajos.\u001B[0m");
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
