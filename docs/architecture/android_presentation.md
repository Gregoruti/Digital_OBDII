# Regras de Negócio e Apresentação (Kotlin)

A arquitetura do Android app segue o padrão **Clean Architecture + MVVM**, utilizando **Coroutines/Flow** para o streaming de dados e **Hilt** para injeção de dependência.

## Domain Layer
A camada `domain` contém modelos de dados puros (sem dependências de Android):
- `VehicleSnapshot`: Representação em tempo real das métricas da ECU.
- `TripSummary`: Dados agregados de tempo, distância e combustível.
- `UseCases`: Cálculos isolados como `CalculateIdealGearUseCase` e `CalculateFuelConsumptionUseCase`.

## Presentation Layer
A camada `presentation` contém as telas em **Jetpack Compose** e seus respectivos `ViewModels`:
- **DashboardScreen**: Renderiza gauges customizados (como o `RpmGauge` desenhado no Canvas). Reativo ao `StateFlow` do `DashboardViewModel`.
- O app inclui seções de personalização profunda (limites de giro, cor do shift light, etc.) injetados no ViewModel a partir dos UseCases.
