# Gap Buffer y POO en Java — Preguntas de estudio

> Preguntas surgidas mientras armaba un editor de texto con gap buffer
> y sistema de undo/redo. Algoritmos y Estructura de Datos III — 2026.

---

## Parte I — La estructura: Gap Buffer

### Concepto

1. **¿Qué es un gap buffer?**

2. **¿Por qué se lo considera una estructura de datos?** ¿Qué campos la componen realmente?

3. **¿Qué problema resuelve?** ¿Qué ventaja concreta ofrece frente a un array plano o una lista enlazada?

### El gap en sí

4. **¿Puede haber más de un gap simultáneamente?**

5. **¿El gap es un carácter?** ¿Representa un espacio en blanco, un `\0`, un backspace?

6. **¿Dónde queda el cursor respecto del gap?** ¿Es una variable aparte o se deduce de los índices?

7. **¿Cómo se "coloca" la basura entre `gapStart` y `gapEnd`?**

8. **¿Cómo termina apareciendo un hueco en el medio de un texto como `HOLA····MUNDO`?**

### Operaciones

9. **¿Por qué mover el cursor implica copiar caracteres?** ¿No alcanza con mover los índices?

10. **¿Cada pulsación de flecha equivale a una copia?** ¿Cuál es el costo real?

11. **Si el cursor está al inicio del texto, ¿el contenido queda pegado al final del array?**

### Ciclo de vida

12. **¿Cuál es el ciclo de vida completo, paso a paso?**
    Creación → insertar 1 char → insertar 2 → insertar 3 → borrar → copiar → pegar → realocar → guardar.

13. **¿El texto que ingresa el usuario arranca siempre al final del array?**

14. **¿Tiene sentido reservar 1000 caracteres para escribir 10?** ¿Cómo se elige el tamaño del gap?

15. **¿Qué pasa cuando el gap se agota?**

---

## Parte II — Diseño: patrón Command

16. **¿`BufferGap` implementa la interface `Comando`?** ¿Cuál es la relación entre ambos?

17. **¿Qué información necesita guardar cada comando para poder deshacerse?**

---

## Parte III — Java: interfaces y clases abstractas

### Diferencias de fondo

18. **¿Qué es una interface y qué es una clase abstracta?** ¿Cuándo conviene cada una?

19. **Ejemplo con animales** que muestre por qué hacen falta las dos.

### Reglas de herencia

20. **¿Una clase abstracta puede implementar interfaces?**

21. Verificar el mapa completo:
    - Una clase concreta extiende **una** clase e implementa **varias** interfaces.
    - Una clase abstracta extiende **una** clase e implementa **varias** interfaces.
    - Una interface no extiende clases, pero **sí** extiende varias interfaces.

### Instanciación y obligaciones

22. **¿Se puede hacer `new ClaseAbstracta()`?** Si tiene constructor definido, ¿para qué sirve?

23. **¿Se puede instanciar una interface?**

24. **¿Una clase concreta está obligada a definir todos los métodos de la interface?** ¿Hay excepciones?

25. **¿Cuál es la ventaja de que una clase abstracta implemente una interface?**

### Visibilidad

26. **¿Una interface puede tener métodos y atributos?**

27. **¿Va `public interface` o `interface { public ... }`?** ¿Cuál de los dos `public` importa?

28. **Ese `public` obligatorio en la implementación, ¿viene de la visibilidad de la interface o del método?**

29. **¿Puede una interface ser package-private y tener métodos public a la vez?** ¿No es contradictorio?

---

## Parte IV — Documentación y formato de entrega

> A partir de acá, las consultas ya no son sobre la estructura sino sobre
> cerrar el trabajo: documentarlo, verificarlo contra el enunciado y entregarlo.

30. **¿Cómo se ejecuta este proyecto desde la terminal?** Es un proyecto Java sin Maven ni Gradle, armado en IntelliJ. Quiero los comandos `javac`/`java` explícitos, no depender del IDE. **Propone primero el plan y lo discutimos.**

31. **¿Qué formato debe tener cada archivo fuente según el enunciado?** ¿Alcanza con nombre y apellido de los integrantes, o pide algo más?

32. **¿Qué debe contener el Readme según la rubrica?** 

33. **En la cabecera que generaste dice que el archivo incluye `PosicionInvalidaException`, pero no existe.** 

34. **¿Dónde pide el enunciado la tabla de desplazamientos?** ¿En el código, en el README, o en los dos lugares?

---

## Parte V — Verificación contra el enunciado

35. **Evaluá el estado del trabajo contra la rúbrica y decime qué queda pendiente.** No quiero una lista de mejoras posibles: quiero lo que falta para cumplir lo que el enunciado pide.

36. **¿Qué es exactamente `testTablas()` y con qué parte del enunciado se corresponde?**

37. **El enunciado pide `Nodo` como *inner class* privada de `PilaES`.** ¿Cómo queda el archivo con ese cambio? **Mostrame el código antes de aplicarlo.**

38. **¿Qué debería devolver `descripcion()`?** ¿El PDF especifica algo, o queda a criterio nuestro?

39. **Cuando decís que `testTablas()` está incompleto, ¿te referís a las operaciones o solo a lo que imprime?**

40. **Verificá de nuevo el estado del repo** — hubo cambios desde la última revisión, incluidos commits de mi compañero.

41. **Evaluá desde cero, como si no hubieras visto el proyecto antes.** Incluí los textos del README, no solo el código: quiero saber si alguna afirmación que escribimos ahí no se corresponde con lo que hace el programa.

---