# Documentación del Proyecto: appTest

## 1. Descripción General

`appTest` es una aplicación cliente para una plataforma de e-commerce. Permite a los usuarios registrarse, iniciar sesión, ver un catálogo de productos y gestionar su perfil. La aplicación está diseñada para ser robusta y escalable, con una clara separación entre la interfaz de usuario, la lógica de negocio y la comunicación con el servidor.

---

## 2. Tecnologías y Librerías Principales

- **Lenguaje:** 100% **Kotlin**.
- **Arquitectura:** Patrones MVVM (Model-View-ViewModel) implícitos, con una clara separación de capas (UI, datos, red).
- **Asincronía:** **Corrutinas de Kotlin** para todas las operaciones en segundo plano, como las llamadas a la red.
- **Networking:**
    - **Retrofit:** Para definir de forma declarativa la API REST y gestionar las peticiones HTTP.
    - **OkHttp:** Utilizado por Retrofit para realizar las llamadas. Se personaliza con un `AuthInterceptor` para inyectar tokens de autorización.
    - **Gson:** Para la serialización y deserialización automática de objetos JSON a clases de datos de Kotlin.
- **Componentes de Jetpack:**
    - **ViewBinding:** Para acceder a las vistas de forma segura y sin `findViewById`.
    - **Navigation Component:** Para gestionar toda la navegación entre fragments dentro de la `MainActivity`.
    - **Lifecycle (`lifecycleScope`):** Para lanzar corrutinas que están atadas al ciclo de vida de las Activities y Fragments.

---

## 3. Estructura del Proyecto

La organización del código está dividida en paquetes según su funcionalidad, lo que facilita su mantenimiento:

- **`com.example.apptest`**
    - **`api`**: Contiene todo lo relacionado con la comunicación de red.
        - `ApiClient.kt`: Objeto singleton que configura y provee la instancia de Retrofit para toda la app.
        - `AuthInterceptor.kt`: Intercepta cada llamada de red para añadir el token de autorización si el usuario ha iniciado sesión.
        - **`services`**: Interfaces de Retrofit que definen los endpoints de la API (ej. `AuthService.kt`, `ProductService.kt`).
    - **`data.model`**: Clases de datos (`data class`) que representan los objetos que se envían y reciben de la API (ej. `User.kt`, `LoginRequest.kt`, `LoginResponse.kt`, `Product.kt`).
    - **`storage`**: Se encarga del almacenamiento local.
        - `SessionManager.kt`: Un singleton crucial que gestiona la sesión del usuario (token y datos del perfil), tanto en memoria como de forma persistente.
    - **`ui`**: Contiene las Activities y Fragments, es decir, la capa de presentación.
        - `LoginActivity.kt`: Pantalla de inicio de sesión.
        - `RegisterActivity.kt`: Pantalla de registro de nuevos usuarios.
        - `MainActivity.kt`: La actividad principal que alberga la navegación por fragments.
        - `HomeFragment.kt`: Muestra la lista de productos.
        - `ProfileFragment.kt`: Muestra los datos del usuario y permite cerrar sesión.
- **`res` (Recursos)**
    - **`layout`**: Archivos XML que definen la interfaz de cada pantalla.
    - **`drawable`**: Contiene los iconos vectoriales (`ic_home.xml`, `ic_profile.xml`).
    - **`menu`**: `bottom_nav_menu.xml`, que define los botones de la barra de navegación.
    - **`navigation`**: `nav_graph.xml`, que define el mapa de navegación entre los fragments.

---

## 4. Lógica y Flujos de Funcionamiento

### 4.1. Flujo de Autenticación

Es el corazón de la lógica de negocio y el flujo más complejo.

**A. Inicio de Sesión (`LoginActivity.kt`)**
1.  **Arranque:** `LoginActivity` es la actividad de lanzamiento (`LAUNCHER`).
2.  **Verificación de Sesión:** Al iniciar, consulta `SessionManager.getToken()`.
    -   **Si existe un token:** Significa que el usuario ya tiene una sesión activa. La app salta directamente a `MainActivity` y `LoginActivity` se finaliza.
    -   **Si no existe un token:** Se muestra la pantalla de login.
3.  **Proceso de Login:**
    - El usuario introduce sus credenciales y pulsa "Login".
    - Se lanza una corrutina. Se llama a `authService.login()`.
    - Si la llamada es exitosa, se recibe un `LoginResponse`.
    - Se guarda el token y los datos del usuario usando `sessionManager.saveToken()` o `setSessionOnlyToken()` dependiendo de si el checkbox "Recordar sesión" está marcado.
    - Finalmente, se navega a `MainActivity` y se limpia el historial para que el usuario no pueda volver a la pantalla de login con el botón "atrás".

**B. Registro de Usuario (`RegisterActivity.kt`)**
1.  Desde `LoginActivity`, el usuario pulsa en "Registrarse" y se abre `RegisterActivity`.
2.  El usuario rellena el formulario.
3.  Al pulsar "Registrarse", se inicia el **flujo de registro en dos pasos**, que fue clave en nuestra depuración:
    - **Paso 1: Crear el Usuario.** Se llama a `authService.register()`. Según la API, esta llamada crea el usuario en la base de datos y devuelve el objeto `User` recién creado (sin token).
    - **Paso 2: Obtener el Token.** Inmediatamente después, se llama a `authService.login()` usando el email y contraseña que el usuario acaba de introducir. Esta segunda llamada sí devuelve un `LoginResponse` con el token de sesión.
4.  Con la respuesta del `login`, se guardan el token y los datos del usuario en el `SessionManager`.
5.  Se navega a `MainActivity`, limpiando el historial para que el usuario no pueda volver a la pantalla de registro.

### 4.2. Gestión de Sesión (`SessionManager.kt`)

- **Patrón Singleton:** `SessionManager` está implementado como un singleton para garantizar que solo exista **una única instancia** en toda la aplicación. Esto asegura que tanto las Activities como el `AuthInterceptor` accedan siempre al mismo estado de sesión.
- **Doble Almacenamiento:**
    - **En Memoria (`inMemoryToken`):** Guarda el token para la sesión actual. Es rápido y se limpia cuando la app se cierra. Permite el login "solo por esta sesión".
    - **Persistente (`SharedPreferences`):** Guarda el token de forma permanente en el dispositivo. Permite el login "recordado".
- **`getToken()`:** Este método es inteligente. Primero intenta devolver el token de la memoria. Si no lo encuentra, busca en `SharedPreferences`. Esto permite que ambos tipos de sesión funcionen.

### 4.3. Flujo Principal y Navegación (`MainActivity.kt`)

1.  **Contenedor Principal:** `MainActivity` actúa como el anfitrión. Su layout (`activity_main.xml`) tiene dos componentes clave:
    - `FragmentContainerView`: El área donde se mostrarán los diferentes fragments.
    - `BottomNavigationView`: La barra de navegación inferior.
2.  **Conexión Mágica:** En el `onCreate` de `MainActivity`, la línea `NavigationUI.setupWithNavController(...)` conecta la barra de navegación con el `NavController` del `FragmentContainerView`.
3.  **Funcionamiento:** Gracias a esta conexión, cuando el usuario pulsa un ítem en la barra (ej. "Perfil"), el `NavController` busca en el `nav_graph.xml` un fragment con el mismo ID (`@+id/profileFragment`) y automáticamente realiza la transacción para mostrarlo en el contenedor. Los iconos para estos botones se definen en `bottom_nav_menu.xml` y se cargan desde la carpeta `drawable`.

### 4.4. Perfil y Cierre de Sesión (`ProfileFragment.kt`)

- **Mostrar Datos:** Al crearse, el fragment obtiene el nombre del usuario desde `sessionManager.getUserName()` y lo muestra en la interfaz.
- **Cerrar Sesión:**
    - El botón "Logout" tiene un listener.
    - Llama a `sessionManager.clear()`, que borra tanto el token en memoria como el persistente.
    - Redirige al usuario a `LoginActivity` y usa `flags` para limpiar completamente el historial de navegación, asegurando que no pueda volver a la pantalla de perfil con el botón "atrás".

---

## 5. Instrucciones de Ejecución Local

Esta sección explica cómo ejecutar localmente tanto el backend como la aplicación Android.

### 5.1. Requisitos Previos

- JDK 21 (la build usa Java 21; Gradle puede auto-descargar el toolchain si no lo tienes instalado).
- Maven para el backend (se incluye `mvnw`).
- Android SDK y Gradle para la app Android (se incluye `gradlew`).
- Emulador de Android o un dispositivo físico.

> Nota: Si ves el error "SDK location not found", configura `sdk.dir` en `local.properties` (por ejemplo: `sdk.dir=C:\\Android\\Sdk`) o define la variable de entorno `ANDROID_HOME`.

### 5.1.1. Compatibilidad con Java 21

El proyecto está configurado para compilar con Java 21:

- `compileOptions` usa `JavaVersion.VERSION_21`.
- `kotlinOptions.jvmTarget` está en `21` y `kotlin { jvmToolchain(21) }` permite que Gradle use/descargue el JDK 21 automáticamente.

### Backend

- Este proyecto usa exclusivamente Xano para autenticación y registro de clientes.
- La base se define en `BuildConfig.XANO_BASE_URL`.
- El backend antiguo fue removido de esta app (conservado aparte según indicación).

### 5.2. Ejecución del Backend

1.  Abre una terminal en la carpeta raíz del backend (`Fullstackll`).
2.  Construye y ejecuta el proyecto con Maven:
    ```shell
    .\mvnw.cmd -DskipTests package
    java -jar target/*.jar
    ```
3.  Verifica que el servidor esté funcionando en `http://localhost:8080`.

### 5.3. Ejecución de la App Android

1.  Abre el proyecto `appTest` en Android Studio.
2.  **Configuración de la URL del Backend:** Es crucial que la app pueda comunicarse con el backend. La URL base se gestiona en el archivo `app/build.gradle.kts` a través de `BuildConfig.BASE_URL`.
    -   **Para el emulador de Android:** Usa `http://10.0.2.2:8080/`. Esta IP es un alias especial que el emulador usa para referirse al `localhost` de la máquina anfitriona.
    -   **Para un dispositivo físico:** Usa la IP de tu máquina en la red local (ej. `http://192.168.1.100:8080/`).
3.  Ejecuta la aplicación desde Android Studio seleccionando un emulador o dispositivo conectado.
