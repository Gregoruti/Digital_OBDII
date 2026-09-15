# Project Guidelines
GUIDELINES.md, pronto para ser salvo na raiz da pasta docs/ (ou raiz do repositório) do seu novo projeto Android/Kotlin.

markdown
# GUIDELINES — [NOME DO PROJETO] (Android/Kotlin)

> Diretrizes de Desenvolvimento do Projeto
> Última atualização: [DATA] | Mantenedor: [NOME/EQUIPE]
> Stack: Kotlin + Jetpack Compose + Clean Architecture + Hilt + Room + Coroutines/Flow

---

## 0. Governança de IA (Pair Programming com Agentes)

### 0.1 Modelo(s) Padrão
- **Assistente(s) oficial(is):** [ex.: GitHub Copilot Agent, Gemini Agent (Android Studio), Claude Code, Cursor]
- **Modelo(s) utilizado(s):** [ex.: Gemini 2.x Pro/Flash, GPT-5, Claude Sonnet/Opus]
- **Regra de uso:** manter rastreabilidade do modelo utilizado no header dos arquivos alterados e em `docs/CHANGELOG.md`.

### 0.2 Operação com Agentes de IA no Android Studio
- Utilizar `implementation_plan.artifact.md` para planejar mudanças complexas (novas telas, migrations, refatorações) antes de codificar.
- Manter `task.artifact.md` sincronizado durante a execução.
- Validar alterações via terminal integrado do Android Studio ou scripts `.bat` do projeto.
- Preferir sessões curtas: **uma tela/use case/feature por vez**, com critério de aceite claro (compila + testa + roda no emulador/dispositivo).
- Sempre revisar o diff gerado pela IA antes de aplicar — atenção especial a `build.gradle.kts`, `AndroidManifest.xml` e migrations do Room.
- Nunca permitir que o agente rode `git push --force` ou apague migrations existentes sem confirmação explícita.

### 0.3 Regras de Registro (Obrigatórias)
- Atualizar, nas **primeiras ~50 linhas** dos arquivos críticos (`ViewModel`, `Repository`, `UseCase`, `Database`): objetivo, correlações, histórico e status.
- Registrar no `CHANGELOG.md`: o que mudou, impacto, `versionCode`/`versionName`, e qual modelo/agente de IA foi usado.
- Atualizar o documento de status vigente quando a mudança afetar navegação, banco de dados ou fluxos principais.

### 0.4 Arquivos Críticos para Leitura Inicial (ordem obrigatória)
1. `docs/GUIDELINES.md` (este arquivo)
2. `docs/CHANGELOG.md`
3. `docs/SPECIFICATION_FOR_APP.md`
4. Documento de status vigente (ex.: `docs/REVISAO_STATUS_ATUAL_[DATA].md`)
5. `app/build.gradle.kts` (versões e dependências atuais)
6. Arquivos de código diretamente relacionados à tarefa (Entity, DAO, Repository, UseCase, ViewModel, Screen)

### 0.5 Boas Práticas de Prompt Engineering para o Projeto
- Fornecer ao agente: **objetivo**, **camada afetada** (presentation/domain/data), **critérios de aceite** e **arquivos relevantes**.
- Para mudanças no Room: sempre informar a versão atual do banco e pedir explicitamente a criação da `Migration`.
- Para Compose: pedir sempre `@Preview` junto com o componente novo.
- Fragmentar tarefas grandes em subtarefas por camada (ex.: "1. Entity + Migration", "2. DAO + Repository", "3. UseCase", "4. ViewModel", "5. Screen").
- Validar toda resposta contra este documento antes de aceitar (arquitetura, nomenclatura, testes, PowerShell, segurança).

### 0.6 Gestão de Contexto em Sessões Longas
- Resumir o progresso em `task.artifact.md` para retomar sem reprocessar tudo.
- Se o agente repetir erros já corrigidos (ex.: usar `&&` no PowerShell, esquecer campo em migration), encerrar a sessão e reabrir citando este documento.
- Persistir decisões arquiteturais importantes em `docs/`, nunca depender só da memória do chat.

---

## 1. Filosofia do Projeto

### 1.1 Abordagem MVP (Minimum Viable Product)
- Desenvolvimento incremental por MVPs numerados (MVP-01, MVP-02, ...).
- Cada MVP é completo e funcional (compila, roda, tem valor de ponta a ponta).
- Validação rigorosa (build + testes + anti-regressão) antes de avançar.
- Documentação sempre sincronizada com o código.

### 1.2 Princípios Fundamentais
- **Clean Architecture:** separação `presentation` / `domain` / `data`.
- **SOLID**, **DRY**, **KISS**, **YAGNI**.
- **TDD:** testes guiam o desenvolvimento, especialmente em `UseCase` e `Repository`.
- **Security by Design:** nunca expor secrets, validar entradas, seguir LGPD/GDPR quando houver dados pessoais.
- **Compose-first:** UI declarativa como padrão; evitar Views/XML exceto em casos legados justificados.

---

## 2. Estrutura Arquitetural

presentation/          # UI e ViewModels
├── screens/           # Telas Compose
├── components/        # Componentes reutilizáveis
├── navigation/        # NavHost, rotas, args
└── theme/             # Design System (Color, Type, Shape, Theme)

domain/                # Regras de negócio (Kotlin puro, sem dependência de Android)
├── model/             # Entidades de domínio
├── repository/        # Interfaces de repositório
└── usecase/           # Casos de uso (1 responsabilidade cada)

data/                  # Fontes de dados
├── database/          # Room Database
│   ├── dao/           # Data Access Objects
│   ├── entities/      # Entidades Room (@Entity)
│   └── Converters.kt  # TypeConverters (único arquivo!)
├── remote/            # Retrofit/Ktor, DTOs, mappers
└── repository/        # Implementações de repositório

di/                    # Módulos Hilt (AppModule, DatabaseModule, NetworkModule...)

ruby

> 💡 **Dica de modularização (projetos grandes):** considerar Gradle multi-módulo (`:app`, `:core:designsystem`, `:core:database`, `:feature:x`) para builds mais rápidos e melhor separação de responsabilidades. Avaliar a partir de ~3-4 features complexas.

### 2.2 Fluxo de Dados

UI (Compose) → ViewModel → UseCase → Repository → (Room | Retrofit/API)

markdown

- Dados reativos com `Flow` na camada `domain`/`data`.
- Estado de UI exposto via `StateFlow<UiState>` no `ViewModel`.
- Eventos únicos (navegação, snackbar) via `Channel`/`SharedFlow`, nunca via `LiveData` para eventos.
- Injeção de dependências via **Hilt**.

---

## 3. Padrões de Código

### 3.1 Nomenclatura
- **Classes:** PascalCase (ex.: `ChildProfile`, `TaskRepository`)
- **Funções:** camelCase (ex.: `saveTask`, `getChildProfile`)
- **Constantes:** UPPER_SNAKE_CASE (ex.: `MAX_ITEMS`, `DEFAULT_TIMEOUT_MS`)
- **Composables:** PascalCase, começando com o nome do componente (ex.: `TaskCard`, `HomeScreen`)
- **Arquivos:** nome da classe/composable principal (ex.: `HomeScreen.kt`)
- **State classes:** sufixo `UiState` (ex.: `HomeUiState`)
- **Sealed classes de evento/efeito:** sufixo `UiEvent` / `UiEffect`

### 3.2 Organização de Arquivos
- Um arquivo por classe principal (exceto states/sealed classes pequenas relacionadas).
- Agrupar por feature dentro de `presentation/screens/<feature>/`.
- Testes no mesmo package lógico, em `src/test` (JVM) ou `src/androidTest` (instrumentado).

### 3.3 Comentários e Documentação (KDoc)

```kotlin
/**
 * Descrição da classe/função.
 *
 * @param parametro Descrição do parâmetro
 * @return Descrição do retorno
 * @since MVP-XX
 */
3.4 Segurança e Dados Sensíveis ⭐
Nunca commitar google-services.json com chaves reais, keystores, tokens ou senhas.
Usar local.properties (já no .gitignore) para chaves de API locais; em CI, usar Secrets do GitHub Actions.
Dados sensíveis em disco: usar EncryptedSharedPreferences ou Jetpack Security Crypto (nunca SharedPreferences puro para tokens/senhas).
Certificado de assinatura de release: armazenar .jks fora do repositório; usar variáveis de ambiente/Secrets no CI.
Validar/sanitizar toda entrada de usuário antes de persistir ou enviar à API.
Habilitar minifyEnabled/R8 em builds de release (ofusca código e remove código morto).
Revisar dependências periodicamente (./gradlew dependencyCheckAnalyze ou Dependabot no GitHub).
4. Estratégia de Testes
4.1 Pirâmide de Testes
70% Unitários (JUnit + MockK + Truth): UseCase, Repository, ViewModel, Mappers
20% Integração: Room DAOs (androidTest com banco em memória), Retrofit com MockWebServer
10% E2E/UI: Compose UI Tests (createComposeRule) para fluxos críticos
4.2 Cobertura Mínima
Camada	Cobertura mínima
Use Cases	100%
Repositories	90%
ViewModels	80%
Composables	Testes críticos (fluxos principais)
4.3 Padrão de Testes
kotlin
@Test
fun `should return success when valid data`() = runTest {
    // Given (Arrange)
    val input = TaskFixtures.valid()

    // When (Act)
    val result = useCase(input)

    // Then (Assert)
    assertThat(result).isEqualTo(Result.success(expected))
}
4.4 Ferramentas Recomendadas
JUnit4/5 — base de testes
MockK — mocks em Kotlin (preferir sobre Mockito)
Truth (Google) — assertions fluentes
Turbine — testar Flow/StateFlow
kotlinx-coroutines-test (runTest, TestDispatcher) — testar coroutines
Compose UI Testing (createComposeRule, createAndroidComposeRule)
Room em memória (Room.inMemoryDatabaseBuilder) para testes de DAO
MockWebServer (OkHttp) — testar chamadas de rede sem depender de backend real
4.5 Boas Práticas
Testes independentes e determinísticos (usar TestDispatcher, nunca Thread.sleep).
Fixtures/builders para objetos de teste (evitar duplicação de setup).
Nomear testes descrevendo comportamento esperado.
Testar estados de erro/loading nos ViewModels, não só o "caminho feliz".
5. Controle de Versão
5.1 Versionamento Semântico + Versionamento Android
Semântico do app: MAJOR.MINOR.PATCH (ex.: 1.7.0) → mapeado para versionName no build.gradle.kts.
versionCode: incrementar sempre em toda release publicável (Play Store exige incremento monotônico).
kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = 17
        versionName = "1.7.0"
    }
}
5.2 Commits Semânticos
scss
feat(mvp-XX): descrição curta
fix(component): descrição do bug corrigido
docs(file): atualização de documentação
test(usecase): adição de testes
refactor(repository): melhoria de código
chore(deps): atualização de dependências
perf(compose): redução de recomposições
5.3 Estratégia de Commits com PowerShell ⚠️ CRÍTICO
5.3.1 Problemas Conhecidos do PowerShell
❌ Problema 1 — Operador &:

powershell
PS> git add . & git commit -m "mensagem"
# ERRO: O caráter de E comercial (&) não é permitido.
❌ Problema 2 — Operador &&:

powershell
PS> git add . && git commit -m "mensagem" && git push
# ERRO: O token '&&' não é um operador de instrução válido
❌ Problema 3 — Executar .bat sem .\:

powershell
PS> compilar_e_testar.bat
# ERRO: não é reconhecido como nome de cmdlet
✅ Correção: PS> .\compilar_e_testar.bat

❌ Problema 4 — Executar gradlew sem .\:

powershell
PS> gradlew clean
# ERRO: não é reconhecido como nome de cmdlet
✅ Correção: PS> .\gradlew clean

5.3.2 Soluções Recomendadas
✅ Solução 1 — Scripts .bat (preferencial):

bash
📁 raiz do projeto
├── compilar_e_testar.bat      # Limpa, compila e testa
├── compilar_e_instalar.bat    # Compila e instala no dispositivo
├── executar_testes.bat        # Executa apenas testes
└── fazer_commit.bat           # Template genérico de commit
✅ Solução 2 — Ponto e vírgula (;):

powershell
PS> git add .; git commit -m "mensagem"; git push
PS> .\gradlew clean; .\gradlew assembleDebug; .\gradlew installDebug
✅ Solução 3 — Comandos em linhas separadas:

powershell
git add .
git commit -m "mensagem"
git push
5.3.3 Template de Script de Commit (fazer_commit.bat)
bat
@echo off
echo ============================================
echo  COMMIT LOCAL E REMOTO - v1.X.X
echo ============================================
echo.

echo [1/5] Verificando status do Git...
git status
echo.

echo [2/5] Adicionando arquivos modificados...
git add .
echo.

echo [3/5] Criando commit local...
git commit -m "tipo(escopo): descricao curta

Status: MVP-XX - X%% completo"
echo.

echo [4/5] Verificando commit criado...
git log -1 --oneline
echo.

echo [5/5] Enviando para repositorio remoto...
git push origin main
echo.

echo ============================================
echo  COMMIT CONCLUIDO COM SUCESSO!
echo ============================================
pause
⚠️ Salvar sempre como ANSI ou UTF-8 sem BOM (ver item 10.4.3 sobre erro de encoding em .bat gerados por agentes de IA).

5.3.4 Checklist de Commit
 Código compilando sem erros (.\gradlew build)
 Testes passando (.\gradlew test)
 CHANGELOG.md atualizado
 versionCode/versionName incrementados (se release)
 Mensagem de commit descritiva e completa
5.3.5 ⚠️ Instrução Padrão para Agentes de IA sobre Terminal
markdown
Ao gerar comandos para Windows PowerShell:
NUNCA usar: &&, &
SEMPRE usar:
  1. Scripts .bat (preferencial)
  2. Ponto-e-vírgula (;) entre comandos
  3. Comandos em linhas separadas
  4. .\ antes de .bat e gradlew
Se o terminal integrado do agente ficar "silencioso" (executa mas não retorna output): não insistir em novas tentativas; criar o .bat e instruir execução manual; seguir com tarefas que não dependem de terminal.

5.4 Estratégia de Branches
5.4.1 Estrutura de Branches
main: código estável, sempre funcional, MVPs validados, protegida contra push direto.
feature/mvp-XX-nome-descritivo: desenvolvimento de MVP completo.
feature/mvp-XX-faseY-nome: fase específica de MVP complexo.
hotfix/descricao: correções urgentes em produção.
bugfix/descricao: correções de bugs não urgentes.
5.4.2 Quando Criar Branch
✅ Novo MVP; fase complexa que afeta múltiplos arquivos; feature >1 dia; refatoração significativa; risco de breaking change (ex.: migration de Room).
❌ Typos em docs; ajustes triviais de formatação.

5.4.3 Nomenclatura
bash
tipo/mvp-numero-fase-descricao-curta
bash
feature/mvp-07-implementacao-completa
feature/mvp-07-fase1-entidades-database
feature/mvp-07-fase2-componentes-ui
hotfix/crash-ao-salvar-tarefa
bugfix/timer-nao-para-corretamente
kebab-case, máx. 50 caracteres.

5.4.4 Fluxo de Trabalho
powershell
# 1. Criar branch
git checkout main
git pull origin main
git checkout -b feature/mvp-07-fase1-entidades-database

# 2. Desenvolver
git add .
git commit -m "feat(mvp-07): adicionar campos imageUrl e category em Task"
git push origin feature/mvp-07-fase1-entidades-database

# 3. Atualizar com main
git checkout main
git pull origin main
git checkout feature/mvp-07-fase1-entidades-database
git merge main
git push origin feature/mvp-07-fase1-entidades-database

# 4. Finalizar
git checkout main
git pull origin main
git merge feature/mvp-07-fase1-entidades-database
git push origin main

# 5. Limpar (opcional)
git branch -d feature/mvp-07-fase1-entidades-database
git push origin --delete feature/mvp-07-fase1-entidades-database
5.4.5 Estratégia para MVPs Complexos (dividir em fases)
scss
main (MVP-06 estável)
  │
  ├─→ feature/mvp-07-fase1-entidades-database (1-2 dias)
  │   ├─ Entidades, migrations, DAOs, repositórios, testes
  │   └─ MERGE → main (validado)
  ├─→ feature/mvp-07-fase2-componentes (2-3 dias)
  │   ├─ Composables reutilizáveis + testes/preview
  │   └─ MERGE → main (validado)
  ├─→ feature/mvp-07-fase3-telas (3-4 dias)
  │   ├─ Screens, ViewModels, navegação, testes de UI
  │   └─ MERGE → main (validado)
  └─→ feature/mvp-07-fase4-integracao (2-3 dias)
      ├─ Integração completa, E2E, anti-regressão, polimento
      └─ MERGE → main (MVP-07 completo!)
5.4.6 Boas Práticas de Branch
✅ Branch descritiva antes de trabalho significativo; commits pequenos e frequentes; atualizar com main antes do merge final; executar testes/build antes do merge; atualizar CHANGELOG antes do merge; deletar branch após merge.

❌ Trabalhar direto na main; nomes genéricos ("teste", "temp", "wip"); branches vivas >2 semanas; merge sem testes; branches abandonadas.

5.4.7 Resolução de Conflitos
powershell
git status   # arquivos "both modified"
# resolver manualmente, remover marcadores <<<<<<<, =======, >>>>>>>
git add arquivo-resolvido.kt
git commit -m "merge: resolver conflitos entre main e feature/mvp-07-fase1"
5.4.8 Comandos Úteis
powershell
git branch                 # locais
git branch -a               # todas
git switch nome-da-branch
git branch -d nome-da-branch
git branch -D nome-da-branch
git push origin --delete nome-da-branch
git diff main..feature/mvp-07-fase1
5.4.9 Checklist de Merge para main
 Testes unitários passando
 Testes de integração passando
 Build: SUCCESS
 Anti-regressão: MVPs anteriores funcionando
 CHANGELOG.md e docs atualizados
 Code review realizado (humano e/ou IA)
 Branch atualizada com main
 Sem conflitos pendentes
6. Processo de Desenvolvimento por MVP
Planejamento — escopo, funcionalidades, draft de arquitetura (entidades, telas, navegação).
Implementação — seguir Clean Architecture, camada por camada (data → domain → presentation).
Testes — unitários, integração (Room/API), anti-regressão.
Documentação — CHANGELOG.md, MVPXX_VALIDATION_SUMMARY.md.
Validação — build completo (.\gradlew clean build), todos os testes passando, revisão de código.
Avançar — incrementar versionCode/versionName, planejar próximo MVP.
7. Checklist de Validação de MVP
 Código implementado e funcionando
 Testes unitários escritos e passando (cobertura adequada)
 Testes de integração (DAOs/API) se aplicável
 Build gradle: SUCCESS
 Anti-regressão: MVPs anteriores funcionando
 CHANGELOG.md atualizado
 MVPXX_VALIDATION_SUMMARY.md criado
 versionCode/versionName incrementados
 Commit com mensagem semântica
 Push para repositório remoto
8. Boas Práticas Específicas
8.1 Jetpack Compose
Componentes pequenos, reutilizáveis e stateless sempre que possível.
State hoisting: estado sobe para o chamador; Composable recebe value + onValueChange.
@Preview para cada componente (incluir @Preview(showBackground = true) com dados fake).
Acessibilidade: contentDescription, semantics {}, área mínima de toque 48dp.
Evitar lambdas não estáveis em parâmetros de Composables críticos (afeta recomposição) — usar remember, key, derivedStateOf quando necessário.
Usar LazyColumn/LazyRow com key = { item.id } para evitar recomposições e perda de estado ao reordenar listas.
Nomear parâmetros modifier: Modifier = Modifier sempre como último parâmetro opcional.
Evitar lógica de negócio dentro do Composable — delegar ao ViewModel/UseCase.
8.2 Hilt/Dagger
Um módulo por camada/feature (DatabaseModule, NetworkModule, RepositoryModule).
Providers claros e específicos (@Provides/@Binds).
Evitar dependências circulares.
Usar @ViewModelScoped/@ActivityRetainedScoped conforme o ciclo de vida real da dependência (evitar @Singleton por padrão sem necessidade).
8.3 Room Database
Migrations versionadas e testadas (MigrationTestHelper).
Testes de integração para DAOs (banco em memória).
Índices em campos de busca/filtro frequente (@Index).
Converters para tipos complexos — ver 8.3.1.
8.3.1 TypeConverters — Boas Práticas ⭐ CRÍTICO
Regra 1 — Um único arquivo de Converters:

✅ Manter todos os TypeConverters em Converters.kt.
❌ Não criar múltiplos arquivos (causa erro de compilação por duplicidade).
kotlin
// ❌ ERRADO - arquivo separado causa conflito
// DateTimeConverters.kt
@TypeConverter
fun toLocalDateTime(timestamp: Long?): LocalDateTime? { ... }

// Converters.kt (já existe!)
@TypeConverter
fun toLocalDateTime(timestamp: Long?): LocalDateTime? { ... }
// ERRO: Multiple functions define the same conversion
Regra 2 — Verificar converters existentes antes de adicionar novos:

kotlin
class Converters {
    // Existentes (MVP-08)
    @TypeConverter fun fromLocalDateTime(dateTime: LocalDateTime?): Long?
    @TypeConverter fun toLocalDateTime(timestamp: Long?): LocalDateTime?

    // Novos (MVP-09) - apenas adicionar ao mesmo arquivo
    @TypeConverter fun fromLocalDate(date: LocalDate?): Long?
    @TypeConverter fun toLocalDate(epochDay: Long?): LocalDate?
}
Regra 3 — Desugaring para java.time em API < 26:

kotlin
// app/build.gradle.kts
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true  // OBRIGATÓRIO
    }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
Regra 4 — Estratégias de conversão por tipo:

Tipo Kotlin	Tipo SQL	Estratégia	Exemplo
LocalDate	LONG	toEpochDay()	dias desde 1970-01-01
LocalDateTime	LONG	toEpochSecond()/toEpochMilli()	segundos/millis desde epoch
Enum	TEXT	name/valueOf()	"ACTIVE" ↔ TaskStatus.ACTIVE
List<String>	TEXT	joinToString()/split()	"a,b,c" ↔ listOf("a","b","c")
UUID	TEXT	toString()/fromString()	String ↔ UUID
Regra 5 — Documentar Converters claramente:

kotlin
/**
 * Converte LocalDate para Long (epochDay) (armazenamento)
 * MVP09 - Sistema de Controle Diário
 * Armazena número de dias desde 1970-01-01
 */
@TypeConverter
fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()
Checklist TypeConverters:

 Verificar se converter já existe em Converters.kt
 Adicionar ao arquivo existente (não criar novo)
 Habilitar desugaring se usar java.time e minSdk < 26
 Registrar apenas uma vez em @TypeConverters(Converters::class)
 Documentar estratégia de conversão
 Testar compilação: .\gradlew clean build
8.3.2 Migrations — Checklist de Alteração de Schema
 Migration criada e registrada em ordem em AppDatabase
 Campo novo adicionado na entidade correspondente (@Entity)
 Valor padrão compatível com a migration
 Testes de build e execução realizados
 Documentação atualizada (CHANGELOG.md, GUIDELINES.md)
⚠️ Erro comum: adicionar campo via migration/DAO mas esquecer de refletir na data class da entidade → erro de compilação e falha Room/SQLite em runtime. Sempre sincronizar migration ↔ entidade.

8.4 Coroutines e Flow
Dispatchers.IO para operações de I/O (Room, rede, arquivos).
Flow para streams de dados reativos (ex.: dao.observeAll(): Flow<List<Entity>>).
StateFlow para estado de UI no ViewModel.
Tratamento adequado de exceções: try/catch em UseCase/Repository, nunca deixar coroutine "engolir" erro silenciosamente; usar CoroutineExceptionHandler quando apropriado.
Usar viewModelScope no ViewModel (nunca GlobalScope).
Cancelar/reiniciar operações de busca com debounce() + distinctUntilChanged() em campos de busca.
8.5 Navegação (Navigation Compose)
Rotas tipadas (sealed class ou @Serializable com Navigation 2.8+) em vez de Strings soltas.
Argumentos de navegação sempre validados/nulos tratados.
Um NavHost central em presentation/navigation/.
Deep links documentados quando existirem.
9. Design System
9.1 Cores
Seguir Material Design 3 (Material You / dynamic color quando aplicável).
Paleta definida em Theme.kt (ColorScheme light/dark).
Cores semânticas (success, error, warning, info) definidas como tokens próprios.
9.2 Tipografia
Famílias de fontes consistentes (Typography.kt).
Hierarquia clara (Display, Headline, Title, Body, Label — conforme M3).
Tamanhos e pesos definidos centralmente.
9.3 Espaçamentos
Sistema padronizado (4dp, 8dp, 16dp, 24dp, 32dp) via objeto Spacing ou Dimens.
Margens e paddings consistentes entre telas.
9.4 Acessibilidade ⭐
Contraste mínimo WCAG AA entre texto e fundo.
Áreas de toque mínimas de 48x48dp.
contentDescription em todo ícone/imagem interativa; null explícito quando decorativo.
Suporte a TalkBack testado nos fluxos críticos.
Respeitar fontScale do sistema (usar sp, nunca dp para texto).
10. Ferramentas e Ambiente
10.1 Requisitos
Android Studio (última versão estável)
JDK 17 (recomendado para AGP 8.x+; usar 11+ apenas se módulo legado exigir)
Gradle 8.0+ / Android Gradle Plugin compatível
Git
10.2 Dependências Principais
Kotlin (última stable, com K2 compiler habilitado quando estável para o projeto)
Jetpack Compose (BOM mais recente)
Hilt/Dagger
Room
Coroutines/Flow
Retrofit/OkHttp ou Ktor Client (se houver rede)
JUnit, MockK, Truth, Turbine (testes)
LeakCanary (debug builds) — detecção de memory leaks
Coil (carregamento de imagens em Compose)
Timber (logging estruturado)
10.3 Comandos Gradle no PowerShell ⚠️ IMPORTANTE
O PowerShell NÃO suporta o operador && para encadear comandos.

❌ ERRADO (não funciona no PowerShell):

powershell
.\gradlew clean && .\gradlew assembleDebug && .\gradlew installDebug
git add . && git commit -m "mensagem" && git push
✅ CORRETO:

Opção 1 — comandos em sequência na mesma linha (Gradle aceita múltiplas tasks):

powershell
.\gradlew clean assembleDebug installDebug
Opção 2 — ponto-e-vírgula:

powershell
.\gradlew clean; .\gradlew assembleDebug; .\gradlew installDebug
Opção 3 — scripts .bat prontos:

powershell
.\compilar_e_testar.bat
.\compilar_e_instalar.bat
.\executar_testes.bat
Comandos Gradle comuns:

powershell
.\gradlew clean assembleDebug              # Limpar e compilar
.\gradlew assembleDebug installDebug       # Compilar e instalar
.\gradlew test                             # Testes unitários (JVM)
.\gradlew connectedAndroidTest             # Testes instrumentados
.\gradlew clean test assembleDebug installDebug  # Sequência completa
.\gradlew lint                             # Lint estático
.\gradlew ktlintCheck                      # Lint de formatação Kotlin (se configurado)
.\gradlew bundleRelease                    # Gerar AAB para Play Store
Comandos Git:

powershell
# CORRETO:
git add .
git commit -m "mensagem"
git push
# Ou com ponto-e-vírgula:
git add .; git commit -m "mensagem"; git push
Scripts Batch Disponíveis:

Script	Descrição	Comando
compilar_e_testar.bat	Limpa, compila e executa testes	.\compilar_e_testar.bat
compilar_e_instalar.bat	Compila e instala no dispositivo	.\compilar_e_instalar.bat
executar_testes.bat	Executa apenas os testes	.\executar_testes.bat
10.4 Erros Conhecidos de Compilação ⚠️
10.4.1 Cache Corrompido do Gradle/Kotlin Daemon
Erro: Daemon compilation failed: null + Could not close incremental caches + file-to-id.tab

Causa: cache incremental do Kotlin Daemon corrompido (arquivo bloqueado por outro processo, interrupção de compilação anterior).

Solução:

powershell
.\gradlew --stop        # Parar todos os Gradle Daemons
.\gradlew clean         # Limpar cache de build
.\gradlew assembleDebug # Recompilar do zero
Prevenção: não interromper compilações em andamento; fechar o Gradle Sync do Android Studio antes de compilar via terminal.

10.4.2 Room/Kapt (ou KSP) — "Failed to create MD5 hash"
Erro: Failed to create MD5 hash for file '...RewardDao.kapt_metadata' as it does not exist.

Causa: mesma do item 10.4.1 — cache corrompido pós-interrupção.

Solução: mesma do 10.4.1 (--stop → clean → assembleDebug).

💡 Recomendação atual: migrar de kapt para KSP (Kotlin Symbol Processing) no Room/Hilt sempre que possível — builds significativamente mais rápidos e menos propensos a esse tipo de corrupção de cache.

10.4.3 Encoding Inválido de Arquivos .bat Gerados por Agentes de IA
Erro:

javascript
.\script.bat : Falha na execução do programa: O executável especificado
não é um aplicativo válido para esta plataforma de SO.
Causa: arquivo .bat criado com encoding UTF-8 BOM ou UTF-16, que o Windows não executa como batch.

Solução: recriar o arquivo manualmente com encoding ANSI/UTF-8 sem BOM, ou executar os comandos diretamente no terminal:

powershell
git add .
git commit -m "mensagem"
git push
10.5 Boas Práticas ao Trabalhar com Agentes de IA e Terminal
10.5.1 Terminal Silencioso do Agente
Sintoma: terminal integrado executa comandos mas não retorna output (comum em sessões longas de chat).

Workarounds:

Usar scripts .bat executados manualmente pelo usuário.
Criar o .bat via ferramenta de criação de arquivo do agente (funciona normalmente).
Instruir o usuário a executar .\nome_do_script.bat no PowerShell.
Focar em edições de arquivo/documentação enquanto o terminal não responde.
Não insistir em múltiplas tentativas — desperdiça tempo/tokens.
10.5.2 Gradle Sync "travado" no Android Studio
Causa: conflito de locks entre o Android Studio e o terminal (ambos usando o Gradle Daemon simultaneamente).

Solução:

powershell
# 1. No Android Studio: File → Invalidate Caches → Invalidate and Restart
# 2. OU via terminal:
.\gradlew --stop
# Fechar Android Studio
.\gradlew assembleDebug
# Reabrir Android Studio
Prevenção: não compilar via terminal com Gradle Sync ativo; usar File → Sync Project with Gradle Files antes de compilar via terminal; fechar o Android Studio ao compilar APK/AAB para distribuição.

10.5.3 Checklist para Sessões Produtivas com o Agente
 Android Studio fechado (se for compilar via terminal)
 Terminal PowerShell aberto na raiz do projeto
 Última versão do código: git pull
 Projeto compilando: .\gradlew assembleDebug
 Dispositivo conectado (se for instalar): adb devices
 Arquivos de docs abertos para contexto: GUIDELINES.md, CHANGELOG.md
 Problema a resolver documentado claramente antes de pedir ao agente
Dicas para evitar travamentos:

Sessões curtas e focadas — uma tarefa por vez.
Confirmar o output do terminal antes de pedir nova ação.
Se o agente travar, fechar e abrir nova sessão com contexto resumido.
Manter os cabeçalhos de 50 linhas atualizados — o agente depende deles.
Compilação manual — executar .\gradlew assembleDebug após edições grandes.
11. Documentação Obrigatória
Para cada MVP, criar/atualizar:

CHANGELOG.md — histórico de mudanças (o quê, impacto, versionCode, modelo de IA usado)
MVPXX_VALIDATION_SUMMARY.md — resumo de validação
PATHS.md — fluxo de navegação (se aplicável)
Comentários/KDoc no código
12. Anti-Regressão
Sempre que um novo MVP é desenvolvido:

Executar todos os testes existentes.
Verificar se MVPs anteriores continuam funcionando.
Validar navegação completa (todas as rotas do NavHost).
Testar build em dispositivo real quando possível (não só emulador).
Rodar .\gradlew lint para detectar regressões estáticas.
13. Estratégia de Navegação
13.1 Estrutura Atual (v[X.Y.Z])
css
SplashScreen
    │
    ├─→ [Fluxo principal 1]
    └─→ [Fluxo principal 2]
Documentar aqui o grafo de navegação real do app, atualizando a cada MVP.

13.2 Expansão Planejada (MVP-XX)
Descrever novas rotas/telas previstas para o próximo MVP.

14. Roadmap de MVPs
Lista viva dos MVPs concluídos, em andamento e planejados.

MVP	Descrição	Status	Versão
MVP-01	[ex.: Onboarding + estrutura base]	✅ Concluído	1.0.0
MVP-02	[ex.: CRUD principal]	🔄 Em andamento	1.1.0
MVP-03	[descrição]	📋 Planejado	—
15. CI/CD com GitHub Actions ⭐
Exemplo de pipeline básico (.github/workflows/android-ci.yml):

yaml
name: Android CI

on:
  pull_request:
    branches: [ main ]
  push:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      - name: Lint
        run: ./gradlew lint
      - name: Unit tests
        run: ./gradlew test
      - name: Build debug APK
        run: ./gradlew assembleDebug
Bloquear merge para main se o pipeline falhar (branch protection rules).
Adicionar job de testes instrumentados com emulador (reactivecircus/android-emulator-runner) quando o projeto justificar.
Automatizar ktlint/detekt como gate de PR.
Versionar versionCode automaticamente a partir do número de commits/tags (opcional).
16. Observabilidade, Performance e Qualidade
16.1 Logging
Usar Timber em vez de Log.* diretamente.
Nunca logar dados sensíveis (senhas, tokens, PII), nem em builds debug.
16.2 Crash Reporting
Integrar Firebase Crashlytics (ou similar) em builds de release.
Adicionar breadcrumbs em fluxos críticos (compra, cadastro, etc.).
16.3 Performance
LeakCanary em builds debug para detectar memory leaks.
Baseline Profiles para reduzir tempo de startup e jank em telas críticas.
Evitar recomposições desnecessárias em Compose (usar Layout Inspector → Recomposition Counts).
Macrobenchmark/Microbenchmark para medir performance antes de otimizar (profiling > suposição).
Paginação (Paging 3) para listas grandes vindas de Room/API.
16.4 R8/ProGuard
Habilitar isMinifyEnabled = true e isShrinkResources = true em builds de release.
Manter proguard-rules.pro documentado, com comentários explicando cada -keep necessário.
Testar o APK/AAB de release (minificado) antes de publicar — funcionalidades podem quebrar silenciosamente por ofuscação de reflection/serialização.
17. Publicação (Play Console) ⭐
Usar Android App Bundle (.aab), não APK, para publicação (.\gradlew bundleRelease).
Seguir política de dados do Google Play (Data Safety form atualizado a cada alteração de permissões/coleta de dados).
Testar em internal testing track antes de produção.
Monitorar Android Vitals (crash rate, ANR rate) pós-lançamento.
Manter versionCode sempre crescente entre uploads.
18. Revisão de Código (Humana e com IA)
Checklist mínimo de revisão de PR:

 Segue a arquitetura definida (seção 2)
 Nomenclatura e padrões de código (seção 3)
 Testes adequados (seção 4)
 Sem segredos/dados sensíveis expostos (seção 3.4)
 Migrations do Room corretas e testadas (seção 8.3)
 Sem regressões conhecidas (seção 12)
 Documentação atualizada (seção 11)
 Lint (.\gradlew lint) sem novos warnings críticos
Ao usar IA como revisora, fornecer o diff completo + objetivo da mudança, e pedir explicitamente riscos de segurança, performance (recomposição/memória) e regressão de banco de dados.

19. Histórico de Erros e Soluções
Registrar cronologicamente problemas relevantes e soluções, para evitar repetição em sessões futuras com agentes de IA.

Template de entrada:

markdown
### [Data] — [Título do problema]
**Sintoma:** ...
**Causa raiz:** ...
**Solução aplicada:** ...
**Prevenção futura:** ...
Exemplo real (categoria recorrente — migration/entidade dessincronizada):

markdown
### [Data] — Campo ausente na entidade após migration
**Sintoma:** erro de compilação e falha Room/SQLite ao rodar o app.
**Causa raiz:** campo `xyz` adicionado via Migration + DAO, mas não
adicionado à data class da entidade correspondente.
**Solução aplicada:** adicionar o campo na entidade com valor padrão
compatível com a migration; rebuild.
**Prevenção futura:** sempre usar o checklist da seção 8.3.2 ao alterar schema.
Apêndice A — Placeholders a Substituir ao Iniciar um Novo Projeto Android
[NOME DO PROJETO]
[DATA], [NOME/EQUIPE]
[X.Y.Z] (versionName), versionCode inicial
applicationId no app/build.gradle.kts
Roadmap de MVPs (seção 14) e histórico de erros (seção 19)