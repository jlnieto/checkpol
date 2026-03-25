# Guia de desarrollo

## Principios generales

- Cambios pequenos.
- Implementar solo lo pedido.
- No inventar requisitos.
- Priorizar claridad y mantenibilidad.
- Codigo sobrio y profesional.
- Sin sobreingenieria.

## Estilo de implementacion

- Preferir soluciones directas y explicitas.
- Evitar capas o interfaces sin necesidad real.
- Usar validaciones cercanas al caso de uso o al dominio.
- Mantener nombres consistentes con el lenguaje del producto.
- No meter patrones por costumbre.

## UX

La UX debe priorizar a una persona no tecnica.

Criterios:

- formularios claros,
- pocos pasos,
- textos comprensibles,
- validaciones utiles,
- vistas limpias,
- sin ruido visual.

## Vistas

La interfaz inicial debe apoyarse en:

- Thymeleaf,
- HTML simple,
- CSS limpio,
- sin JavaScript complejo salvo necesidad puntual.

## Base de datos

- PostgreSQL como base de datos objetivo.
- Flyway para todas las migraciones.
- No modificar esquemas manualmente fuera de migraciones.

## Testing

Cada fase debe dejar una verificacion proporcionada a su alcance.

Preferencias:

- tests de contexto y de arranque para la base,
- tests de servicio para reglas de negocio,
- tests MVC para formularios y flujos simples cuando aporten valor.

Documentar tambien el estado real cuando una fase quede implementada.
No dejar en `README`, `AGENTS.md` o `docs/` una funcionalidad como futura si ya existe en el codigo.

## XML

Si una fase toca generacion XML:

- aislar la generacion en un componente claro,
- definir entradas bien nombradas,
- no fijar como oficial una estructura no verificada,
- implementar solo la modalidad de `parte de viajeros` salvo cambio explicito de alcance.

## Lo que debe evitarse

- React o SPA al inicio,
- microservicios,
- colas,
- automatizaciones prematuras,
- autenticacion avanzada sin necesidad,
- diseno de un PMS generalista.

## Cuando actualizar la documentacion

Actualizar `docs/` cuando cambie:

- el alcance funcional,
- la arquitectura,
- el modelo de dominio,
- las reglas de implementacion,
- una decision importante de producto.

Revisar tambien `README.md` y `AGENTS.md` cuando el cambio afecte:

- al alcance real del MVP,
- a flujos disponibles para el usuario,
- a restricciones activas del proyecto.
