# Indexador de Arquitetura para Agentes de IA

Este diretório contém a documentação modular da arquitetura do aplicativo Digital OBD-II. 
Para otimizar o consumo de janela de contexto (Context Window) de agentes de IA (Android Studio Gemini e Antigravity), **leia apenas o arquivo relevante para a tarefa atual**.

## Módulos de Arquitetura

1. **[Regras de Negócio e Apresentação (Kotlin)](android_presentation.md)**
   - Clean Architecture, Hilt, MVVM.
   - Jetpack Compose e UI (Gauges, Dashboards).
   - UseCases (Cálculo de Consumo, Marcha Ideal, RPM Preditivo).

2. **[Camada de Dados, OBD e Bluetooth (Kotlin)](data_obd_bluetooth.md)**
   - Sockets Bluetooth Clássico e SPP.
   - Comunicação com ELM327 (AT Commands, Boot, PIDs).
   - ObdPollingEngine e Priority Interleaving.

3. **[Motor de Áudio e Processamento Digital de Sinais (C++)](engine_audio_cpp.md)**
   - Sintetizador de Áudio (V6 Twin-Turbo, Granular).
   - Google Oboe / AAudio.
   - Integração JNI e Regras Restritas da Thread de Áudio (Sem Alocações, 16KB Page Size).

4. **[Banco de Dados e Persistência Local (Room/DataStore)](room_database.md)**
   - Entities, DAOs e Repositórios.
   - Persistência de preferências de UI via DataStore.
