# Benchmark BCrypt de I1-H01

ADR-007 fija inicialmente BCrypt coste 12. El benchmark es deliberadamente
manual para no convertir variaciones de runners compartidos en fallos de CI:

```bash
cd server
./mvnw -Dbcrypt.benchmark=true -Dtest=BCryptCostBenchmarkTest test
```

Debe registrarse antes del cierre de H01:

- fecha, CPU/arquitectura, memoria y JVM;
- diez muestras sintéticas;
- media y máximo observados;
- decisión explícita de conservar o ajustar el coste configurado.

No usa contraseñas, correos ni otros datos reales. El benchmark local documentado
forma parte de la aceptación de H01. Repetirlo en la arquitectura de staging es
evidencia operativa posterior de CL0 y no bloquea el cierre local de H01.

## Referencia local de 2026-09-13

- Entorno: x86_64, AMD Ryzen 5 7430U, 14,5 GiB de RAM.
- JVM: OpenJDK 21.0.12.
- Muestras sintéticas: 10 operaciones de codificación y verificación con coste 12.
- Resultado: media de 248 ms y máximo de 307 ms.

El resultado confirma que el test manual es ejecutable, pero no aprueba todavía
el presupuesto de staging. Esa decisión se toma al preparar el entorno cloud sin
cambiar el coste BCrypt 12 aprobado para H01.

## Revalidación local de 2026-09-19

- Entorno: x86_64, AMD Ryzen 5 7430U, 14,5 GiB de RAM.
- JVM: OpenJDK 21.0.12.
- Configuración: BCrypt coste 12, 10 entradas sintéticas y verificación de cada
  hash.
- Resultado: media de 243 ms y máximo de 336 ms.

No se modifican los parámetros de seguridad a partir de esta medición.
