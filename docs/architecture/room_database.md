# Banco de Dados e Persistência Local

A persistência do aplicativo Digital OBD-II divide-se em: Histórico Complexo de Viagens e Preferências Rápidas do Usuário.

## Histórico de Viagens (Room)
O `AppDatabase` é gerido via biblioteca Room, e expõe DAOs reativos com `Flow`.
- **TripEntity**: Armazena carimbos de data/hora, distâncias acumuladas e estimativas de uso de combustível.
- Todas as migrations devem ser explicitamente registradas. Conversões complexas (ex. `LocalDateTime` para `Long`) devem ser agrupadas no arquivo `Converters.kt`.

## Perfis de Usuário / Layouts (DataStore)
O `ProfileRepositoryImpl` usa Jetpack DataStore Preferences para persistir configurações leves:
- Geometria customizada da barra de RPM (ângulo, arco, raio).
- Valores de calibração de MAF (offset de consumo).
- MAC Adress salvo do último dispositivo conectado para tentativa de auto-conexão pelo Watchdog de Background.
