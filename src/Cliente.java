import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;

public class Cliente {
   private String usuario;                      // Nombre de usuario
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
      
      System.out.println("Bienvenido " + usuario + "!");
      return 0;
   }

   private void mostrarMenu() throws IOException {
      while (true) {
         System.out.println("\n\u001B[35m--- Menú Principal ---\u001B[0m");
         System.out.println("1. Subir archivo");
         System.out.println("2. Descargar archivo");
         System.out.println("3. Crear carpeta");
         System.out.println("4. Ver archivos y carpetas");
         System.out.println("5. Eliminar archivo o carpeta");
         System.out.println("6. Salir");
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
               System.out.println("crearCarpeta");
               break;
            case "4":
               System.out.println("verArchivosYCarpetas");
               break;
            case "5":
               System.out.println("eliminarArchivoOCarpeta");
               break;
            case "6":
               System.out.println("Saliendo...");
               socketCliente.close();
               inputText.close();
               System.exit(0);
               break;
            default:
               System.out.println("Opción no válida.");
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
      String mensaje = new String(mensajeRecibido.getData(), 0, mensajeRecibido.getLength());
      return mensaje;
   }
   
   private void solicitarCarpetaPersonal(String nombreCarpeta) throws IOException {
      enviarMsjAServidor("0:" + nombreCarpeta);
      String respuesta = recibirMsjDeServidor();
      System.out.println("Respuesta del servidor: " + respuesta);
   }
   
   public static void main(String[] args) {
      new Cliente().iniciarCliente();
   }
}
