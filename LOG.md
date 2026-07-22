# DevFlow Hub - Registo de Desenvolvimento

## Nota sobre a reorganização

Este LOG foi reconstruído de forma organizada com base no código-fonte existente, nos documentos das sprints, nos registos de desenvolvimento anteriores e nas validações realizadas.

O repositório anterior permanece como histórico do trabalho original. Este novo repositório organiza os principais marcos técnicos através de branches e commits coerentes.

As datas dos novos commits representam a data da reorganização no Git e não substituem as datas reais registadas nas sessões originais.

---

## Marco 0 - 17/07/2026 - Criação do novo repositório organizado

### Objetivo da sessão

Criar uma fundação limpa para o projeto DevFlow Hub, removendo dependências geradas, configurações pessoais e ficheiros desnecessários do novo histórico Git.

### Funcionalidades implementadas

- Criação de um novo repositório Git local.
- Definição da branch inicial `main`.
- Criação de um `.gitignore` adequado para Java, Maven, Node.js, Vite e IDEs.
- Criação de um `.gitattributes` para normalizar os finais de linha.
- Criação de um `.editorconfig` para uniformizar a formatação.
- Criação do README inicial.
- Criação do novo LOG de desenvolvimento.
- Definição da estratégia de branches.
- Preservação do código validado numa pasta-fonte separada.
- Exclusão da configuração local que contém a password do PostgreSQL.

### Problemas encontrados

- O repositório anterior continha ficheiros gerados dentro do histórico Git.
- A pasta `node_modules` tinha sido registada no controlo de versões.
- O projeto estava localizado dentro do OneDrive, causando bloqueios no npm e no Maven.
- O histórico anterior acumulava várias funcionalidades no mesmo branch.
- O README e o LOG já não estavam totalmente sincronizados com o código atual.

### Como resolvi

- Mantive o repositório anterior sem o eliminar.
- Copiei o código para uma pasta-fonte fora do OneDrive.
- Excluí `.git`, `node_modules`, `target`, `dist`, `.idea` e ficheiros locais.
- Criei o novo repositório em `C:\Dev\DevFlow_Hub`.
- Defini uma estratégia baseada em `main`, `develop` e branches de trabalho.
- Decidi adicionar o código progressivamente através de commits coerentes.
- Adicionei uma nota transparente sobre a reconstrução do histórico.

### Ficheiros atualizados

- `.gitignore`
- `.gitattributes`
- `.editorconfig`
- `README.md`
- `LOG.md`

### Estado atual

- Novo repositório inicializado.
- Branch `main` criada.
- Configuração base do Git preparada.
- Código-fonte original preservado em `C:\Dev\DevFlow_Hub_Source`.
- Nenhuma password local adicionada ao novo repositório.
- Nenhum ficheiro gerado adicionado ao Git.

### Validação ainda pendente

- Criar a branch `develop`.
- Adicionar o esquema PostgreSQL.
- Adicionar o backend progressivamente.
- Adicionar a API REST.
- Adicionar o frontend React.
- Adicionar a interface Thymeleaf.
- Adicionar os testes.
- Validar o JAR final.
- Criar o novo repositório remoto no GitHub.

### Próxima etapa

Criar a branch `develop` e iniciar a branch `feature/database-schema` para adicionar o esquema PostgreSQL e os dados de demonstração.

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**259 linhas**.

A contagem inclui os ficheiros de configuração e documentação utilizados para
criar a fundação do novo repositório.

---

## Marco 1 - 17/07/2026 - Estrutura PostgreSQL e migração

### Objetivo da sessão

Adicionar ao novo repositório a estrutura persistente do DevFlow Hub e
documentar a criação e atualização da base de dados PostgreSQL.

### Funcionalidades implementadas

- Adição do script principal `devflow_hub.sql`.
- Criação das tabelas `collaborators`, `projects`, `tasks` e
  `internal_programs`.
- Inclusão das relações entre colaboradores, projetos, tarefas e programas.
- Utilização de identificadores `BIGINT`, compatíveis com `Long` no backend.
- Inclusão de campos de auditoria.
- Inclusão dos campos utilizados pelo temporizador das tarefas.
- Inclusão de constraints, índices e dados de demonstração.
- Adição do script `migrate_existing_database.sql`.
- Criação de um guia específico para preparar e atualizar o PostgreSQL.

### Problemas encontrados

- A base de dados de trabalho já continha tabelas criadas por versões
  anteriores.
- `CREATE TABLE IF NOT EXISTS` não recria tabelas existentes nem adiciona
  constraints que estejam em falta.
- A primeira consulta às foreign keys não apresentou resultados na base
  antiga.

### Como resolvi

- Mantive a base de trabalho sem eliminar os dados existentes.
- Criei uma base vazia chamada `devflow_hub_validation`.
- Executei o script principal numa instalação limpa.
- Validei separadamente as tabelas, os dados de demonstração e as foreign
  keys.
- Mantive um script específico para a migração de bases anteriores.

### Ficheiros atualizados

- `database/devflow_hub.sql`
- `database/migrate_existing_database.sql`
- `database/README.md`
- `README.md`
- `LOG.md`

### Estado atual

- As quatro tabelas principais foram criadas corretamente.
- Foram carregados 6 colaboradores, 4 projetos, 5 tarefas e 4 programas
  internos.
- Foram validadas quatro foreign keys:
  - `internal_programs.manager_id` referencia `collaborators.id`;
  - `projects.manager_id` referencia `collaborators.id`;
  - `tasks.assignee_id` referencia `collaborators.id`;
  - `tasks.project_id` referencia `projects.id`.
- O script principal está preparado para instalações novas.
- O script de migração está disponível para bases antigas.
- Nenhuma password administrativa real foi adicionada ao repositório.

### Validação ainda pendente

- Executar `migrate_existing_database.sql` numa cópia de uma base antiga.
- Confirmar o esquema através do backend com `ddl-auto=validate`.

### Próxima etapa

Adicionar a fundação do backend Spring Boot, incluindo Maven, configuração
segura, entidades e repositories.

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**465 linhas**.

A contagem inclui os scripts SQL, a documentação da base de dados e as
atualizações associadas no README e no LOG.

---

## Atualização - Guia de instalação local da base de dados

### Objetivo da sessão

Criar um documento simples com as instruções necessárias para preparar a base
de dados PostgreSQL numa máquina local.

### Funcionalidades implementadas

- Criação de um guia TXT para instalação local do PostgreSQL.
- Documentação da criação da base `devflow_hub`.
- Instruções para executar o script `devflow_hub.sql`.
- Instruções para configurar `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- Inclusão de consultas para validar tabelas, dados e chaves estrangeiras.
- Documentação de erros frequentes e respetivas soluções.

### Problemas encontrados

- Era necessário fornecer instruções independentes do README principal.
- As credenciais do PostgreSQL variam entre computadores.

### Como resolvi

- Criei um ficheiro TXT dedicado à instalação local.
- Mantive as passwords reais fora do repositório.
- Adicionei uma referência ao guia no README principal.

### Ficheiros atualizados

- `database/INSTALACAO_BASE_DADOS_LOCAL.txt`
- `README.md`
- `LOG.md`

### Estado atual

- O projeto possui instruções para criar a base de dados localmente.
- O utilizador pode configurar as suas próprias credenciais.
- O guia inclui validação das quatro tabelas e das foreign keys.

### Validação ainda pendente

- Testar o guia completo numa segunda máquina ou instalação PostgreSQL limpa.

### Próxima etapa

Continuar a implementação da branch `feature/backend-core`.

### Estatísticas do Git

Segundo as estatísticas do Git, esta atualização alterou **3 ficheiros**, com
**371 linhas adicionadas** e **1 linha removida**.

A contagem inclui o guia de instalação local e as atualizações correspondentes
no README e no LOG.

---

## Marco 2 - 20/07/2026 - Fundação e serviços do backend

### Objetivo da sessão

Adicionar ao novo repositório a fundação Spring Boot e organizar as principais
regras de negócio do DevFlow Hub através de entidades, repositories e services.

### Funcionalidades implementadas

- Configuração do backend com Java 21 e Spring Boot.
- Adição do Maven Wrapper.
- Configuração segura da ligação PostgreSQL através de variáveis de ambiente.
- Criação das entidades `Collaborator`, `Project`, `Task` e
  `InternalProgram`.
- Criação dos repositories Spring Data JPA.
- Centralização dos estados e prioridades permitidos.
- Criação de uma função comum para normalização de texto.
- Adição de exceções reutilizáveis.
- Implementação das regras de negócio dos colaboradores.
- Implementação da lógica de autenticação simples de colaboradores.
- Implementação das regras de negócio dos projetos.
- Implementação das regras de negócio das tarefas.
- Implementação das ações de iniciar, pausar, retomar e concluir o timer.
- Implementação das regras de negócio dos programas internos.
- Injeção de `Clock` para tornar a lógica temporal testável.

### Problemas encontrados

- O Java configurado anteriormente dependia de uma pasta interna de uma
  extensão do VS Code.
- A pasta utilizada pela extensão deixou de existir.
- O Maven Wrapper não conseguia encontrar uma instalação válida através de
  `JAVA_HOME`.
- Os valores de estados e prioridades estavam distribuídos por diferentes
  partes da aplicação.
- A lógica temporal precisava de ser testável sem depender diretamente da hora
  real do computador.

### Como resolvi

- Instalei o Eclipse Temurin JDK 21 através do WinGet.
- Configurei permanentemente `JAVA_HOME`.
- Atualizei o `PATH` e as configurações Java do VS Code.
- Removi dependências de caminhos temporários da extensão Pleiades.
- Centralizei estados e prioridades na camada de domínio.
- Mantive os repositories responsáveis apenas pelo acesso aos dados.
- Mantive as regras da aplicação dentro dos services.
- Injetei um `Clock` configurável no service de tarefas.
- Validei a compilação através do Maven Wrapper.

### Ficheiros atualizados

- `backend/.mvn/`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/pom.xml`
- `backend/src/main/java/com/devflowhub/backend/BackendApplication.java`
- `backend/src/main/java/com/devflowhub/backend/config/`
- `backend/src/main/java/com/devflowhub/backend/domain/`
- `backend/src/main/java/com/devflowhub/backend/entity/`
- `backend/src/main/java/com/devflowhub/backend/exception/`
- `backend/src/main/java/com/devflowhub/backend/repository/`
- `backend/src/main/java/com/devflowhub/backend/service/`
- `backend/src/main/java/com/devflowhub/backend/util/`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.example.properties`
- `README.md`
- `LOG.md`

### Estado atual

- A fundação Spring Boot está presente.
- Java 21 está instalado e configurado.
- As entidades estão alinhadas com o esquema PostgreSQL.
- Os repositories estão disponíveis.
- As principais regras de negócio estão implementadas.
- O temporizador das tarefas está implementado.
- O backend compila com sucesso através do Maven Wrapper.
- Nenhuma password administrativa foi adicionada ao repositório.

### Validação ainda pendente

- Adicionar testes unitários dos services.
- Executar o backend ligado à base PostgreSQL.
- Validar o esquema com `ddl-auto=validate`.
- Adicionar os controllers REST.
- Adicionar o tratamento global dos erros da API.
- Adicionar os controllers MVC e a interface Thymeleaf.

### Próxima etapa

Criar a branch `feature/rest-api` e implementar os endpoints REST da aplicação.

### Estatísticas do Git

Segundo as estatísticas do Git, a pasta `backend` adicionou **24 ficheiros** e
**1 855 linhas**.

A contagem inclui código Java, Maven Wrapper, configuração Spring Boot e
ficheiros de propriedades.

---

## Marco 3 - 20/07/2026 - Implementação da API REST

### Objetivo da sessão

Adicionar uma API REST ao DevFlow Hub para expor as funcionalidades já
implementadas na camada de serviços.

### Funcionalidades implementadas

- Criação dos DTOs utilizados pelo dashboard.
- Criação do serviço de consulta e agregação do dashboard.
- Criação do controller REST dos colaboradores.
- Criação do controller REST dos projetos.
- Criação do controller REST das tarefas.
- Criação do controller REST dos programas internos.
- Criação do controller REST do dashboard.
- Implementação de endpoints CRUD.
- Implementação dos endpoints do temporizador das tarefas.
- Implementação do endpoint para concluir uma tarefa.
- Criação de uma estrutura comum para erros da API.
- Implementação do tratamento global das exceções.

### Endpoints adicionados

- `/api/collaborators`
- `/api/projects`
- `/api/tasks`
- `/api/internal-programs`
- `/api/dashboard`
- `/api/tasks/{id}/start-timer`
- `/api/tasks/{id}/pause-timer`
- `/api/tasks/{id}/resume-timer`
- `/api/tasks/{id}/total-time`
- `/api/tasks/{id}/timer`
- `/api/tasks/{id}/complete`

### Problemas encontrados

- O `DashboardController` dependia de um `DashboardService` que ainda não
  estava presente no novo repositório.
- A primeira compilação dos controllers terminou com erro porque essa classe
  não podia ser encontrada.
- Era necessário manter os controllers REST separados dos controllers MVC da
  interface Thymeleaf.

### Como resolvi

- Localizei o `DashboardService` no código-fonte original.
- Adicionei o serviço num commit separado.
- Validei que o serviço utiliza os repositories e DTOs já presentes.
- Mantive os controllers REST na pasta `controller`.
- Não adicionei ainda os controllers MVC existentes em `web/controller`.
- Compilei novamente o backend depois de adicionar a dependência em falta.
- Mantive o tratamento global de erros num commit independente.

### Ficheiros atualizados

- `backend/src/main/java/com/devflowhub/backend/controller/CollaboratorController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/DashboardController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/InternalProgramController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/ProjectController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/TaskController.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardProjectItem.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardSummary.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardTaskItem.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiError.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/main/java/com/devflowhub/backend/service/DashboardService.java`
- `README.md`
- `LOG.md`

### Estado atual

- Os principais recursos possuem controllers REST.
- As operações CRUD estão expostas através da API.
- As operações do temporizador estão disponíveis através de endpoints
  específicos.
- O dashboard possui DTOs e serviço próprios.
- As exceções da aplicação são tratadas globalmente.
- O backend compila e pode ser empacotado através do Maven Wrapper.
- O ficheiro `backend/target/devflow-hub.jar` foi gerado.
- Na validação realizada em 20/07/2026, o JAR gerado possuía
  `57 700 076` bytes.
- A pasta `target` permaneceu fora do controlo de versões.

### Validação ainda pendente

- Executar a aplicação ligada ao PostgreSQL.
- Testar os endpoints com Postman ou ferramenta equivalente.
- Adicionar testes unitários dos services.
- Adicionar testes dos controllers com MockMvc.
- Validar os pedidos com Jakarta Validation.
- Adicionar os controllers MVC e a interface Thymeleaf.
- Validar o JAR numa máquina diferente.

### Próxima etapa

Criar a branch `test/backend-api` para adicionar testes unitários dos services
e testes dos controllers REST com MockMvc. Depois, executar a aplicação ligada
ao PostgreSQL e validar os endpoints através do Postman.

### Estatísticas do Git

Segundo as estatísticas do Git, a implementação da API REST adicionou
**11 ficheiros** e **528 linhas**.

A contagem inclui controllers REST, DTOs, tratamento de exceções e o serviço do
dashboard. As alterações no README e no LOG são contabilizadas separadamente
no commit documental.

---

## Marco 4 - 20/07/2026 - Testes automatizados do backend

### Objetivo da sessão

Adicionar testes automatizados para validar as principais regras de negócio,
o controller REST de tarefas e o arranque do contexto Spring Boot.

### Testes implementados

- Testes dos valores permitidos da camada de domínio.
- Teste da normalização comum de texto.
- Testes do service de colaboradores.
- Testes do service de projetos.
- Testes do service de programas internos.
- Teste do service do dashboard.
- Testes do service de tarefas.
- Testes das operações temporais das tarefas.
- Testes MockMvc do controller de tarefas.
- Teste de arranque do contexto Spring Boot.
- Configuração de uma base H2 em memória para o perfil `test`.

### Componentes validados

- `DomainValues`
- `TextNormalizer`
- `CollaboratorService`
- `ProjectService`
- `InternalProgramService`
- `DashboardService`
- `TaskService`
- `TaskController`
- configuração Spring Boot;
- repositories Spring Data JPA;
- ligação JPA à base H2 em memória.

### Problemas encontrados

- Inicialmente, a branch não continha ficheiros de teste.
- O Maven indicava que não existiam testes para executar.
- Os testes do projeto original estavam misturados entre API REST, services,
  interface MVC e segurança.
- Os testes MVC e de segurança dependiam de componentes que ainda não tinham
  sido adicionados ao novo repositório.
- O contexto Spring apresentou um aviso porque a pasta de templates Thymeleaf
  ainda não existe.
- O Mockito apresentou avisos sobre o carregamento dinâmico do Java Agent em
  versões futuras do Java.

### Como resolvi

- Adicionei os testes progressivamente e em commits separados.
- Mantive fora desta branch os testes da interface MVC e do interceptor de
  autenticação.
- Executei cada grupo de testes antes do respetivo commit.
- Configurei o perfil `test` com uma base H2 em memória.
- Adicionei um teste de contexto com `@SpringBootTest`.
- Executei a suite completa através do Maven Wrapper.
- Confirmei que os avisos do Thymeleaf e do Mockito não representam falhas
  nesta etapa.

### Ficheiros adicionados e atualizados

- `backend/src/test/java/com/devflowhub/backend/BackendApplicationTests.java`
- `backend/src/test/java/com/devflowhub/backend/controller/TaskControllerTest.java`
- `backend/src/test/java/com/devflowhub/backend/domain/DomainValuesTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/CollaboratorServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/DashboardServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/InternalProgramServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/ProjectServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/TaskServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/util/TextNormalizerTest.java`
- `backend/src/test/resources/application-test.properties`
- `README.md`
- `LOG.md`

### Resultado da validação

A suite completa foi executada com:

```powershell
.\mvnw.cmd clean test
```

Resultado:

```text
Tests run: 18
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O teste de contexto confirmou:

- perfil `test` ativo;
- ligação à base `jdbc:h2:mem:devflow_hub_test`;
- quatro repositories JPA encontrados;
- inicialização do `EntityManagerFactory`;
- arranque completo do contexto Spring Boot.

### Estado atual

- As principais regras de negócio possuem testes unitários.
- A lógica temporal das tarefas possui testes automatizados.
- O controller de tarefas possui testes MockMvc.
- O contexto Spring Boot inicia corretamente com H2.
- A suite completa possui 18 testes.
- Todos os testes passaram sem falhas ou erros.

### Validação ainda pendente

- Adicionar testes dos restantes controllers REST.
- Adicionar testes de persistência dos repositories.
- Executar testes de integração com PostgreSQL.
- Validar os endpoints através do Postman.
- Adicionar os testes MVC quando a interface Thymeleaf for integrada.
- Resolver preventivamente o aviso futuro do Java Agent do Mockito.

### Próxima etapa

Executar o backend ligado ao PostgreSQL, validar o esquema com
`ddl-auto=validate` e testar os endpoints REST através do Postman.

### Estatísticas do Git

Segundo as estatísticas do Git, a implementação dos testes adicionou
**10 ficheiros** e **579 linhas**.

A contagem inclui testes unitários, testes MockMvc, o teste de contexto Spring
e a configuração H2. As alterações no README e no LOG são contabilizadas
separadamente no commit documental.

---

## Marco 5 - 20/07/2026 - Validação da API com PostgreSQL

### Objetivo da sessão

Validar o backend DevFlow Hub ligado a uma base de dados PostgreSQL local,
confirmar a compatibilidade entre as entidades JPA e o esquema existente e
criar um smoke test reproduzível para os principais endpoints REST.

### Validações realizadas

- Ligação ao PostgreSQL através do JDBC.
- Inicialização do pool de ligações HikariCP.
- Deteção do PostgreSQL 18.3 e do dialecto `PostgreSQLDialect`.
- Validação do esquema através de
  `spring.jpa.hibernate.ddl-auto=validate`.
- Inicialização do `EntityManagerFactory`.
- Arranque do Tomcat na porta `8080`.
- Validação dos endpoints:
  - `GET /api/collaborators`;
  - `GET /api/projects`;
  - `GET /api/tasks`;
  - `GET /api/internal-programs`;
  - `GET /api/dashboard`.
- Validação da presença dos principais campos do dashboard.
- Validação de um recurso inexistente com resposta HTTP `404`.
- Validação de um pedido inválido com resposta HTTP `400`.
- Validação do CRUD de colaboradores:
  - criação;
  - consulta por ID;
  - atualização;
  - eliminação.
- Confirmação de que a password é aceite nos pedidos de criação, mas não é
  devolvida nas respostas da API.
- Confirmação de que uma atualização sem password preserva a password
  existente.
- Confirmação da remoção do colaborador temporário após o teste.
- Criação de um script PowerShell para repetir automaticamente as validações.

### Problemas encontrados

- As variáveis de ambiente da base de dados tinham de ser definidas na mesma
  sessão PowerShell utilizada para iniciar o backend.
- Um pedido de criação de colaborador sem password devolveu corretamente
  HTTP `400`.
- O `Invoke-WebRequest` apresentou um aviso de segurança ao processar a
  resposta do pedido `DELETE`.
- Alguns fragmentos de comandos colados no terminal criaram ficheiros vazios
  e não rastreados na raiz do repositório.
- A primeira versão do script de smoke tests continha erros de sintaxe
  causados pela formatação do conteúdo copiado.
- Foram identificados registos antigos com caracteres portugueses corrompidos,
  como `MÃ³dulo`, `GestÃ£o` e `FormaÃ§Ã£o`.

### Como resolvi

- Defini `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` antes de executar o Maven
  Wrapper na mesma sessão PowerShell.
- Consultei a resposta JSON do erro para confirmar a regra obrigatória da
  password.
- Confirmei a eliminação do colaborador através de uma consulta posterior que
  devolveu HTTP `404`.
- Removi apenas os ficheiros vazios criados acidentalmente e confirmei que o
  Maven Wrapper verdadeiro permaneceu em `backend/mvnw.cmd`.
- Corrigi e validei a sintaxe do script PowerShell.
- Adicionei limpeza automática do colaborador temporário através de um bloco
  `finally`.
- Mantive a correção dos dados antigos com problemas de codificação como uma
  tarefa separada, para não misturar alterações de dados com a validação da
  API.

### Ficheiros adicionados e atualizados

- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Resultado da validação

O backend foi executado com sucesso utilizando PostgreSQL real.

```text
Database: PostgreSQL 18.3
API port: 8080
Schema validation: successful
Main GET endpoints: successful
Dashboard validation: successful
Collaborator CRUD: successful
Error handling 400/404: successful
Temporary data cleanup: successful
```

O smoke test foi executado com:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1"
```

Resultado final:

```text
All API smoke tests passed.
```

### Estado atual

- O backend inicia corretamente ligado ao PostgreSQL.
- As entidades JPA são compatíveis com o esquema existente.
- Os cinco endpoints principais respondem corretamente.
- O dashboard devolve os indicadores esperados.
- O tratamento global de erros devolve respostas estruturadas para HTTP
  `400` e `404`.
- O CRUD de colaboradores foi validado com persistência real.
- A password não é exposta nas respostas JSON.
- O script de smoke tests remove os dados temporários criados durante a
  execução.
- Nenhuma credencial real foi adicionada ao repositório.

### Validação ainda pendente

- Expandir os smoke tests para projetos, tarefas e programas internos.
- Validar os endpoints do temporizador com PostgreSQL.
- Adicionar testes automatizados de integração com persistência PostgreSQL.
- Expandir os testes dos restantes controllers REST.
- Corrigir os registos antigos com caracteres corrompidos.
- Avaliar o armazenamento seguro das passwords antes de implementar
  autenticação para utilização real.
- Validar o JAR final numa instalação independente.

### Próxima etapa

Expandir a validação da API para o CRUD de projetos, tarefas e programas
internos, incluindo as operações do temporizador. Depois, tratar separadamente
os dados antigos com problemas de codificação.

### Estatísticas do Git

As estatísticas deste marco devem ser registadas depois da criação do commit,
para refletirem os valores reais apresentados pelo Git.

---

## Marco 6 - 21/07/2026 - Validação CRUD completa com PostgreSQL

### Objetivo

Expandir o smoke test da API para validar os principais recursos com
persistência PostgreSQL real, incluindo relações, regras de negócio e o ciclo
completo do temporizador das tarefas.

### Validação realizada

Foram validados:

- CRUD de colaboradores, projetos, tarefas e programas internos;
- relações entre colaboradores, gestores, projetos e responsáveis;
- estados e prioridades permitidos;
- validação das datas iniciais e finais;
- campos obrigatórios e referências inexistentes;
- proteção dos campos internos do temporizador durante a criação;
- início, pausa, retoma e conclusão do temporizador;
- acumulação do tempo de várias sessões;
- proteção do estado durante um temporizador ativo;
- rejeição da pausa de um temporizador inativo;
- rejeição do reinício de uma tarefa concluída;
- respostas HTTP `400` e `404`;
- eliminação e limpeza dos recursos temporários.

### Smoke test automatizado

O ficheiro `scripts/smoke-test-api.ps1` foi expandido para criar e validar
temporariamente:

- um colaborador;
- um projeto;
- um programa interno;
- uma tarefa.

Os recursos são eliminados por ordem de dependência:

1. tarefa;
2. programa interno;
3. projeto;
4. colaborador.

O bloco `finally` também remove os recursos temporários caso ocorra uma falha
durante a execução.

### Correção no PowerShell

A conversão dos payloads JSON para UTF-8 devolvia o array de bytes elemento por
elemento através do pipeline.

A função foi corrigida com `Write-Output -NoEnumerate`, garantindo que o
`Invoke-RestMethod` recebe o array completo.

### Resultado

- todos os testes PostgreSQL da API passaram;
- código de saída: `0`;
- as contagens regressaram aos valores iniciais;
- nenhum dado temporário permaneceu na base de dados;
- nenhuma credencial real foi adicionada ao repositório.

Mensagem final:

```text
All PostgreSQL API smoke tests passed.
```

### Ficheiros atualizados

- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Trabalho pendente

- tratar `HttpMessageNotReadableException` para JSON malformado;
- corrigir os registos antigos com caracteres corrompidos;
- implementar armazenamento seguro das passwords;
- criar testes Maven com uma instância PostgreSQL dedicada;
- validar o JAR final numa instalação independente.

### Próxima etapa

Rever o diff final, criar o commit da branch
`test/postgresql-resource-crud` e abrir um pull request para `develop`.

## Marco 7 - 22/07/2026 - Tratamento seguro de JSON malformado

### Problema identificado

Os pedidos com JSON malformado não eram tratados pelo `ApiExceptionHandler`.

O Spring devolvia HTTP `400`, mas a resposta padrão podia expor:

- stack trace;
- nomes de classes internas;
- detalhes do parser JSON;
- campos como `trace`, `error`, `exception` e `path`.

### Implementação

Foi adicionado tratamento específico para
`HttpMessageNotReadableException`.

A API passa a devolver uma resposta baseada em `ApiError`:

```json
{
  "timestamp": "...",
  "status": 400,
  "message": "The request body contains invalid JSON.",
  "validationErrors": {}
}
```

Também foram adicionados:

- um teste MockMvc para JSON malformado;
- validação da mensagem e do estado HTTP;
- verificação da ausência de detalhes internos;
- um cenário equivalente no smoke test PostgreSQL;
- limpeza automática dos ficheiros temporários utilizados pelo teste.

### Validação realizada

Foram confirmados:

- `TaskControllerTest`: 3 testes sem falhas;
- suite Maven completa: 19 testes sem falhas ou erros;
- resposta real HTTP `400` através da porta `8080`;
- ausência de `trace`, `error`, `exception` e `path`;
- smoke test completo com PostgreSQL;
- código de saída do smoke test: `0`;
- reposição das contagens iniciais da base de dados.

Mensagem final:

```text
All PostgreSQL API smoke tests passed.
```

### Ficheiros atualizados

- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/test/java/com/devflowhub/backend/controller/TaskControllerTest.java`
- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Trabalho pendente

- corrigir os registos antigos com caracteres corrompidos;
- implementar armazenamento seguro das passwords;
- criar testes Maven com uma instância PostgreSQL dedicada;
- concluir a interface Thymeleaf e o frontend React;
- validar o JAR final numa instalação independente.

### Próxima etapa

Rever o diff final, criar o commit da branch
`fix/api-malformed-json-errors` e abrir um pull request para `develop`.

## Marco 8 — Diagnóstico de encoding no PostgreSQL e Windows PowerShell

- A base de dados devflow_hub foi confirmada com server_encoding, client_encoding e encoding da base em UTF8.
- A inspeção hexadecimal confirmou que valores como Gestão, página e Práticas estão armazenados com bytes UTF-8 corretos.
- A resposta JSON bruta da API também apresentou corretamente Módulo, Gestão e outros caracteres portugueses.
- As sequências visíveis como GestÃ£o no psql e no Invoke-RestMethod do Windows PowerShell 5.1 resultam da interpretação incorreta da saída UTF-8 pelo terminal, e não de corrupção dos dados.
- Nenhum UPDATE, alteração de schema ou recriação da base de dados foi necessária.
- Para validação fiável no Windows PowerShell 5.1, a resposta pode ser gravada com curl.exe e lida explicitamente através de ReadAllText(..., UTF8); PowerShell 7 também evita este problema de apresentação.
