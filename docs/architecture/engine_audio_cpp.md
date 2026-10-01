# Motor de Áudio (Engine Sound) e C++

O módulo `:engine-audio` utiliza **C++ moderno (C++17)** acoplado à biblioteca de baixa latência **Google Oboe / AAudio** para a síntese procedural em tempo real.

## DSP e Síntese de Áudio
- **Sintetizador**: Usa osciladores aditivos, algoritmos granulares e envelopes dinâmicos controlados pela carga do motor (Throttle) e RPM lidos do adaptador Bluetooth.
- **Efeitos Inclusos**: `RevLimiter` (corte de giro de ignição com estouros de escapamento), e `TurboEffect` (spool da turbina e válvula blow-off no lift-off).

## Regras Críticas do Callback de Áudio
- **SEM I/O, SEM ALOCAÇÃO:** A thread de callback do Oboe possui deadline estrito (poucos milissegundos). É expressamente proibido alocar memória (usar `new`, `std::vector::push_back`, etc.) ou fazer I/O (disco/log extenso) dentro do callback para evitar "clicks/pops" (underrun) de áudio.
- **Sincronização Atômica:** A passagem de telemetria do Kotlin para o C++ (RPM, Throttle, Gear) é trafegada no `JniBridge.cpp` gravando os valores em `std::atomic<float>`. Nenhum Lock/Mutex deve ser usado no fluxo de leitura no callback de áudio.

## NDK e Compilação
- Alinhamento OBRIGATÓRIO de 16KB para binários `.so` (Android 15+ Google Play Rule). NUNCA usar macros `PAGE_SIZE` ou hardcode `4096`.
- Compilação do NDK (Release) via `CMakeLists.txt` usa flags extremas: `-O3`, `-ffast-math`, `-funroll-loops`. 
