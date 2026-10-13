# Task Management - Digital OBD-II

## MVP-08: RPM Gauge Estética Analógica (Formato 2) 🏎️
**Design Requirements (Baseado em Imagem de Referência):**
O usuário solicitou um novo formato de barra de RPM (Formato 2) que simula um conta-giros analógico de alta performance, bem como a liberação do controle de espessura (thickness) da barra.

**Tarefas para o Antigravity (Jurisdição de Código Kotlin):**

- `[x]` **Domain Layer (`VehicleProfile.kt`)**
  - `[x]` Adicionar `rpmBarStyle: Int = 1` (1 = Linha Fina atual, 2 = Analógico).
  - `[x]` Adicionar `rpmBarThickness: Float = 22f` (Para o usuário controlar a grossura).

- `[x]` **Data Layer (`ProfileRepositoryImpl.kt`)**
  - `[x]` Mapear as novas chaves no DataStore e sincronizar o salvamento.

- `[x]` **Presentation / UI Settings (`VisualSettingsScreen.kt` & `VehicleProfileViewModel.kt`)**
  - `[x]` Criar RadioButtons/Segmented Button para "Estilo da Barra de RPM" (Formato 1 vs Formato 2).
  - `[x]` Criar Slider para "Espessura da Barra" (Thickness), variando de ex: `10f` a `80f`.

- `[x]` **Componente Gráfico (`Gauges.kt` -> `ArchedRpmGauge`)**
  - `[x]` Passar a utilizar o novo `rpmBarThickness` no Stroke do Canvas.
  - `[x]` **Lógica do Formato 2:**
    - `[x]` Desenhar as marcações (ticks/tracinhos) radiais ao longo do arco.
    - `[x]` Desenhar numerais grandes (0 a 8) internamente acompanhando o arco. Usar trigonometria (`cos`, `sin`) baseada no ângulo.
    - `[x]` Estilo da fonte dos números: `FontFamily.SansSerif`, `FontWeight.ExtraBold`, `FontStyle.Italic` (para simular a fonte do painel).
    - `[x]` **Redline Estática Fixo:** A trilha de fundo (Background Track) do arco não deve ser cinza/sólida por inteiro. O segmento que vai do `redlineRpm` até o `maxRpm` deve ser pintado de vermelho escuro fixo, imitando painéis reais. O preenchimento dinâmico do RPM corre por cima disso.

## Tarefas Antigas
- `[x]` Identidade Visual Civic G8 (Cores e Motor Pixel-Perfect)
- `[x]` Histórico de Viagens & Persistência 📂
