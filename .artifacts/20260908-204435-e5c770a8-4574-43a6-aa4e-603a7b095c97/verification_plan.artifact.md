# Plano de Verificação e Testes - MVP-01

Este documento estabelece os critérios de aceite e os testes necessários para aprovar a fundação do app (Conectividade e Parsing).

## 1. Testes Unitários (Automatizados)

Para aprovar a lógica de negócio e parsing sem depender de hardware real.

### [A] Parsing de Respostas OBD-II (`ObdResponseParserTest`)
- [ ] **Sucesso**: Simular resposta `41 0C 1A F8` para RPM e validar o valor calculado.
- [ ] **Resiliência**: Simular resposta com espaços extras ou sem espaços (`410C1AF8`).
- [ ] **Erro de Protocolo**: Simular respostas `NO DATA`, `ERROR`, `CAN ERROR` e garantir que retorne `null` (não crash).
- [ ] **Tensão do Adaptador**: Validar o parsing específico do comando `AT RV`.

### [B] Casos de Uso (`CalculateIdealGearUseCaseTest`)
- [ ] **Cálculo de Marcha**: Validar se, dadas relações de marcha X, o UseCase retorna a marcha correta para um set de RPM/Velocidade.
- [ ] **Casos Limite**: Velocidade zero ou RPM zero deve retornar marcha 0 (Neutro).

---

## 2. Testes Manuais (Smartphone + ELM327)

Para validar a integração com o hardware real.

### [C] Fluxo de Permissões e Descoberta
- [ ] **Permissões**: Abrir o app e negar permissões -> Verificar se exibe aviso.
- [ ] **Permissões**: Abrir o app e conceder permissões -> Verificar se lista dispositivos pareados.
- [ ] **Lista**: Verificar se o nome e o endereço MAC dos dispositivos aparecem corretamente.

### [D] Ciclo de Conexão (Logcat)
- [ ] **Conexão**: Selecionar um ELM327 -> Verificar no Logcat a tentativa de conexão SPP.
- [ ] **Inicialização**: Verificar se os comandos `ATZ`, `ATE0`, etc., são enviados após o socket abrir.
- [ ] **Navegação**: Verificar se, ao conectar com sucesso, o app navega para a tela de Dashboard (placeholder).

---

## 3. Critérios de Aprovação do MVP-01
1. **Build**: `.\gradlew assembleDebug` termina com sucesso.
2. **Testes**: Todos os testes unitários da seção 1 passam.
3. **Funcionalidade**: É possível selecionar um dispositivo Bluetooth e ver o log de inicialização AT bem-sucedido.
